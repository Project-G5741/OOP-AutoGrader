package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.SolutionImportChallengeResult;
import com.eiu.capstone.backend.DTO.rubric.SolutionImportResponse;
import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.ParsedField;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;
import com.eiu.capstone.backend.grading.ParsedMmdRelation;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.service.compile.CompileErrorMessage;
import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.NormalizationResult;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

@Service
public class SolutionImportService {

    private static final Pattern CHALLENGE_PATTERN =
            Pattern.compile("challenge[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);

    private final LabRepository labRepository;
    private final MasterDataRepository masterDataRepository;
    private final JavaCompilerService javaCompilerService;
    private final ReflectionClassParser reflectionClassParser;
    private final MmdParser mmdParser;

    public SolutionImportService(LabRepository labRepository,
                                 MasterDataRepository masterDataRepository,
                                 JavaCompilerService javaCompilerService,
                                 ReflectionClassParser reflectionClassParser,
                                 MmdParser mmdParser) {
        this.labRepository = labRepository;
        this.masterDataRepository = masterDataRepository;
        this.javaCompilerService = javaCompilerService;
        this.reflectionClassParser = reflectionClassParser;
        this.mmdParser = mmdParser;
    }

    public SolutionImportResponse importSolution(UUID labId, List<MultipartFile> files) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files uploaded");
        }

        Map<Integer, ChallengeSources> byNumber = groupByChallenge(files);
        if (byNumber.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No challenge_n folders with .java or .mmd files found");
        }

        MasterDataIndex masterData = MasterDataIndex.load(masterDataRepository);
        List<SolutionImportChallengeResult> results = new ArrayList<>();
        List<Integer> numbers = byNumber.keySet().stream().sorted().toList();
        for (Integer number : numbers) {
            results.add(processChallenge(number, byNumber.get(number), masterData));
        }
        return new SolutionImportResponse(lab.getId(), results);
    }

    private SolutionImportChallengeResult processChallenge(int number,
                                                           ChallengeSources sources,
                                                           MasterDataIndex masterData) {
        if (sources.javaFiles.isEmpty()) {
            return SolutionImportChallengeResult.skipped(number, "No .java files in challenge folder");
        }

        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("solution-import-");
            Path classesDir = tempRoot.resolve("classes");
            Files.createDirectories(classesDir);

            List<SourceEntry> raw = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : sources.javaFiles.entrySet()) {
                String fileName = Path.of(entry.getKey()).getFileName().toString();
                raw.add(new SourceEntry(fileName, new String(entry.getValue(), StandardCharsets.UTF_8)));
            }
            NormalizationResult normalization = StudentSourceNormalizer.normalizeChallengeSources(raw);
            List<JavaFileObject> javaSources = new ArrayList<>();
            for (SourceEntry entry : normalization.sources()) {
                javaSources.add(new MemorySourceJavaFileObject(
                        entry.logicalPath(),
                        entry.source().getBytes(StandardCharsets.UTF_8)));
            }

            CompileOutcome outcome = javaCompilerService.compileSources(javaSources, classesDir);
            if (!outcome.succeeded()) {
                return SolutionImportChallengeResult.skipped(number, compileErrorText(outcome));
            }

            List<ParsedClass> parsedClasses = reflectionClassParser.parseClasses(classesDir);
            if (parsedClasses.isEmpty()) {
                return SolutionImportChallengeResult.skipped(number, "Compile produced no class files");
            }

            boolean hasMmd = sources.mmdSource != null && !sources.mmdSource.isBlank();
            List<ParsedMmdRelation> mmdRelations = List.of();
            if (hasMmd) {
                try {
                    ParsedMmdDiagram diagram = mmdParser.parse(sources.mmdSource);
                    mmdRelations = diagram.relations != null ? diagram.relations : List.of();
                } catch (RuntimeException e) {
                    return SolutionImportChallengeResult.skipped(
                            number,
                            "MMD parse error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            }

            ChallengeStructureDTO challenge = deriveChallenge(number, parsedClasses, mmdRelations, hasMmd, masterData);
            return SolutionImportChallengeResult.applied(number, challenge);
        } catch (IOException e) {
            return SolutionImportChallengeResult.skipped(number, "Import I/O error: " + e.getMessage());
        } finally {
            if (tempRoot != null) {
                deleteRecursively(tempRoot);
            }
        }
    }

    private ChallengeStructureDTO deriveChallenge(int number,
                                                  List<ParsedClass> parsedClasses,
                                                  List<ParsedMmdRelation> mmdRelations,
                                                  boolean hasMmd,
                                                  MasterDataIndex masterData) {
        Map<String, UUID> classIdBySimple = new LinkedHashMap<>();
        List<ClassStructureDTO> classes = new ArrayList<>();

        List<ParsedClass> ordered = new ArrayList<>(parsedClasses);
        ordered.sort(Comparator
                .comparing((ParsedClass c) -> c.outerSimpleName == null ? 0 : 1)
                .thenComparing(c -> c.simpleName, String.CASE_INSENSITIVE_ORDER));

        for (ParsedClass parsed : ordered) {
            UUID id = UUID.randomUUID();
            classIdBySimple.put(parsed.simpleName, id);
        }

        for (ParsedClass parsed : ordered) {
            UUID id = classIdBySimple.get(parsed.simpleName);
            UUID outerId = parsed.outerSimpleName == null
                    ? null
                    : classIdBySimple.get(parsed.outerSimpleName);
            Integer scopeId = masterData.scopeId(parsed.scope);
            Integer declaringTypeId = masterData.declaringTypeId(parsed.declaringType);
            classes.add(new ClassStructureDTO(
                    id,
                    parsed.simpleName,
                    scopeId,
                    declaringTypeId,
                    parsed.isAbstract,
                    parsed.isStatic,
                    mapFields(parsed.fields, masterData),
                    mapMethods(parsed.methods, masterData),
                    mapConstructors(parsed, masterData),
                    outerId,
                    1));
        }

        List<RelationStructureDTO> relations = new ArrayList<>();
        relations.addAll(heritageRelations(parsedClasses, classIdBySimple, masterData));
        relations.addAll(mmdToRelations(mmdRelations, classIdBySimple, masterData));

        return new ChallengeStructureDTO(
                UUID.randomUUID(),
                "Problem " + number,
                number,
                classes,
                relations,
                hasMmd,
                1,
                1,
                1,
                1);
    }

    private List<RelationStructureDTO> heritageRelations(List<ParsedClass> parsedClasses,
                                                         Map<String, UUID> classIdBySimple,
                                                         MasterDataIndex masterData) {
        List<RelationStructureDTO> relations = new ArrayList<>();
        Integer inheritanceId = masterData.relationTypeId("inheritance");
        Integer realizationId = masterData.relationTypeId("realization");
        for (ParsedClass parsed : parsedClasses) {
            UUID sourceId = classIdBySimple.get(parsed.simpleName);
            if (sourceId == null) {
                continue;
            }
            if (parsed.superclassSimpleName != null && inheritanceId != null) {
                UUID targetId = classIdBySimple.get(parsed.superclassSimpleName);
                if (targetId != null) {
                    relations.add(new RelationStructureDTO(UUID.randomUUID(), sourceId, targetId, inheritanceId));
                }
            }
            if (parsed.interfaceSimpleNames != null && realizationId != null) {
                for (String iface : parsed.interfaceSimpleNames) {
                    UUID targetId = classIdBySimple.get(iface);
                    if (targetId != null) {
                        relations.add(new RelationStructureDTO(
                                UUID.randomUUID(), sourceId, targetId, realizationId));
                    }
                }
            }
        }
        return relations;
    }

    private List<RelationStructureDTO> mmdToRelations(List<ParsedMmdRelation> mmdRelations,
                                                      Map<String, UUID> classIdBySimple,
                                                      MasterDataIndex masterData) {
        List<RelationStructureDTO> relations = new ArrayList<>();
        for (ParsedMmdRelation rel : mmdRelations) {
            if (rel == null || rel.sourceClassName == null || rel.targetClassName == null) {
                continue;
            }
            UUID sourceId = classIdBySimple.get(simpleName(rel.sourceClassName));
            UUID targetId = classIdBySimple.get(simpleName(rel.targetClassName));
            Integer typeId = masterData.relationTypeId(rel.relationType);
            if (sourceId == null || targetId == null || typeId == null) {
                continue;
            }
            relations.add(new RelationStructureDTO(UUID.randomUUID(), sourceId, targetId, typeId));
        }
        return relations;
    }

    private static String simpleName(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    private List<FieldStructureDTO> mapFields(List<ParsedField> fields, MasterDataIndex masterData) {
        if (fields == null) {
            return List.of();
        }
        List<FieldStructureDTO> out = new ArrayList<>();
        for (ParsedField field : fields) {
            out.add(new FieldStructureDTO(
                    UUID.randomUUID(),
                    field.name,
                    field.dataType,
                    masterData.scopeId(field.scope)));
        }
        return out;
    }

    private List<MethodStructureDTO> mapMethods(List<ParsedMethod> methods, MasterDataIndex masterData) {
        if (methods == null) {
            return List.of();
        }
        List<MethodStructureDTO> out = new ArrayList<>();
        for (ParsedMethod method : methods) {
            out.add(new MethodStructureDTO(
                    UUID.randomUUID(),
                    method.name,
                    method.returnType,
                    masterData.scopeId(method.scope),
                    method.isStatic,
                    method.isAbstract,
                    mapParameters(method.parameterTypes)));
        }
        return out;
    }

    private List<ConstructorStructureDTO> mapConstructors(ParsedClass parsed, MasterDataIndex masterData) {
        if (parsed.constructors == null) {
            return List.of();
        }
        List<ConstructorStructureDTO> out = new ArrayList<>();
        for (ParsedConstructor constructor : parsed.constructors) {
            List<String> params = constructor.parameterTypes == null ? List.of() : constructor.parameterTypes;
            boolean isDefault = params.isEmpty();
            out.add(new ConstructorStructureDTO(
                    UUID.randomUUID(),
                    parsed.simpleName,
                    masterData.scopeId(constructor.scope),
                    isDefault,
                    mapParameters(params)));
        }
        return out;
    }

    private List<ParameterStructureDTO> mapParameters(List<String> parameterTypes) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return List.of();
        }
        List<ParameterStructureDTO> out = new ArrayList<>();
        for (int i = 0; i < parameterTypes.size(); i++) {
            out.add(new ParameterStructureDTO(
                    UUID.randomUUID(),
                    "arg" + i,
                    parameterTypes.get(i),
                    i,
                    false));
        }
        return out;
    }

    private static String compileErrorText(CompileOutcome outcome) {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = outcome.diagnostics();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                return CompileErrorMessage.forDiagnostic(diagnostic);
            }
        }
        List<String> messages = outcome.messages();
        if (!messages.isEmpty()) {
            return CompileErrorMessage.summarize(messages.get(0));
        }
        return "Compile failed";
    }

    private Map<Integer, ChallengeSources> groupByChallenge(List<MultipartFile> files) {
        Map<Integer, ChallengeSources> byNumber = new LinkedHashMap<>();
        List<FileEntry> entries = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = relativePath(file);
            if (path.isBlank()) {
                continue;
            }
            String lower = path.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".java") && !lower.endsWith(".mmd")) {
                continue;
            }
            entries.add(new FileEntry(path, file));
        }
        if (entries.isEmpty()) {
            return byNumber;
        }

        boolean singleChallengeRoot = entries.stream().allMatch(e -> {
            String[] parts = e.path.split("/");
            return parts.length >= 2 && CHALLENGE_PATTERN.matcher(parts[0]).matches();
        }) && entries.stream().map(e -> e.path.split("/")[0]).distinct().count() == 1
                && CHALLENGE_PATTERN.matcher(entries.get(0).path.split("/")[0]).matches();

        boolean labRoot = !singleChallengeRoot && entries.stream().anyMatch(e -> {
            String[] parts = e.path.split("/");
            return parts.length >= 3 && CHALLENGE_PATTERN.matcher(parts[1]).matches();
        });

        if (!singleChallengeRoot && !labRoot) {
            // Also accept paths where challenge is any segment
            for (FileEntry entry : entries) {
                Integer number = findChallengeNumber(entry.path);
                if (number == null) {
                    continue;
                }
                byNumber.computeIfAbsent(number, n -> new ChallengeSources()).add(entry);
            }
            return byNumber;
        }

        for (FileEntry entry : entries) {
            String[] parts = entry.path.split("/");
            int challengeIdx = singleChallengeRoot ? 0 : 1;
            if (parts.length <= challengeIdx) {
                continue;
            }
            Matcher matcher = CHALLENGE_PATTERN.matcher(parts[challengeIdx]);
            if (!matcher.matches()) {
                continue;
            }
            int number = Integer.parseInt(matcher.group(1));
            byNumber.computeIfAbsent(number, n -> new ChallengeSources()).add(entry);
        }
        return byNumber;
    }

    private static Integer findChallengeNumber(String path) {
        for (String part : path.split("/")) {
            Matcher matcher = CHALLENGE_PATTERN.matcher(part);
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return null;
    }

    private static String relativePath(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            name = file.getName();
        }
        return name == null ? "" : name.replace('\\', '/').replaceFirst("^/+", "");
    }

    private static void deleteRecursively(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static final class FileEntry {
        final String path;
        final MultipartFile file;

        FileEntry(String path, MultipartFile file) {
            this.path = path;
            this.file = file;
        }
    }

    private static final class ChallengeSources {
        final Map<String, byte[]> javaFiles = new LinkedHashMap<>();
        String mmdSource;

        void add(FileEntry entry) {
            try {
                byte[] bytes = entry.file.getBytes();
                String lower = entry.path.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".java")) {
                    javaFiles.put(entry.path, bytes);
                } else if (lower.endsWith(".mmd")) {
                    mmdSource = new String(bytes, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read " + entry.path);
            }
        }
    }

    private static final class MasterDataIndex {
        private final Map<String, Integer> scopeByName = new HashMap<>();
        private final Map<String, Integer> declaringByName = new HashMap<>();
        private final Map<String, Integer> relationByCanonical = new HashMap<>();

        static MasterDataIndex load(MasterDataRepository repository) {
            MasterDataIndex index = new MasterDataIndex();
            for (MasterData row : repository.findByCategoryOrderByNameAsc("SCOPE")) {
                index.scopeByName.put(row.getName().toLowerCase(Locale.ROOT), row.getId());
            }
            for (MasterData row : repository.findByCategoryOrderByNameAsc("DECLARING_TYPE")) {
                index.declaringByName.put(row.getName().toLowerCase(Locale.ROOT), row.getId());
            }
            for (MasterData row : repository.findByCategoryOrderByNameAsc("RELATION_TYPE")) {
                String canonical = MmdComparisonService.normalizeRelationTypeName(row.getName());
                if (canonical != null) {
                    index.relationByCanonical.putIfAbsent(canonical, row.getId());
                }
                index.relationByCanonical.putIfAbsent(row.getName().toLowerCase(Locale.ROOT), row.getId());
            }
            return index;
        }

        Integer scopeId(String scope) {
            if (scope == null) {
                return scopeByName.get("public");
            }
            return scopeByName.get(scope.toLowerCase(Locale.ROOT));
        }

        Integer declaringTypeId(String declaringType) {
            if (declaringType == null) {
                return declaringByName.get("class");
            }
            return declaringByName.get(declaringType.toLowerCase(Locale.ROOT));
        }

        Integer relationTypeId(String relationType) {
            if (relationType == null) {
                return null;
            }
            String canonical = MmdComparisonService.normalizeRelationTypeName(relationType);
            if (canonical != null && relationByCanonical.containsKey(canonical)) {
                return relationByCanonical.get(canonical);
            }
            return relationByCanonical.get(relationType.toLowerCase(Locale.ROOT));
        }
    }
}
