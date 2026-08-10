package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.*;
import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassConstructorEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassFieldEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassMethodEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdRelationEntry;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassStructureService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final ClassRelationRepository classRelationRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final SubmissionResultLoader submissionResultLoader;
    private final MasterDataCache masterDataCache;
    private final SubmissionCompileErrorStore compileErrorStore;
    private final SubmissionMmdMetaStore submissionMmdMetaStore;
    private final ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;
    private final boolean timingLog;

    public ClassStructureService(ChallengeRepository challengeRepository,
                                  ClassEntityRepository classEntityRepository,
                                  FieldRepository fieldRepository,
                                  MethodRepository methodRepository,
                                  ConstructorRepository constructorRepository,
                                  ParameterRepository parameterRepository,
                                  ClassRelationRepository classRelationRepository,
                                  SubmissionResolutionService submissionResolutionService,
                                  SubmissionResultLoader submissionResultLoader,
                                  MasterDataCache masterDataCache,
                                  SubmissionCompileErrorStore compileErrorStore,
                                  SubmissionMmdMetaStore submissionMmdMetaStore,
                                  ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore,
                                  @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.classRelationRepository = classRelationRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.submissionResultLoader = submissionResultLoader;
        this.masterDataCache = masterDataCache;
        this.compileErrorStore = compileErrorStore;
        this.submissionMmdMetaStore = submissionMmdMetaStore;
        this.parsedSubmissionSnapshotStore = parsedSubmissionSnapshotStore;
        this.timingLog = timingLog;
    }

    /**
     * Loads rubric structure for many challenges in batched queries (one round-trip per entity type).
     */
    public LabChallengeStructureBundle loadChallengeStructures(Collection<UUID> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return emptyStructureBundle();
        }

        List<Challenge> challenges = challengeRepository.findAllById(challengeIds);
        if (challenges.isEmpty()) {
            return emptyStructureBundle();
        }

        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        if (classes.isEmpty()) {
            return new LabChallengeStructureBundle(
                    masterDataCache.get(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of());
        }

        List<Field> allFields = fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> allMethods = methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Constructor> allConstructors = constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<ClassRelation> allRelations = classRelationRepository.findByClassEntityInWithEndpoints(classes);
        List<Parameter> constructorParams = allConstructors.isEmpty()
                ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);
        List<Parameter> methodParams = allMethods.isEmpty()
                ? List.of()
                : parameterRepository.findByMethodIn(allMethods);

        Map<UUID, List<ClassEntity>> classesByChallengeId = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        Map<UUID, List<Field>> fieldsByClassId = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClassId = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClassId = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<Parameter>> paramsByConstructorId = constructorParams.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));
        Map<UUID, List<Parameter>> paramsByMethodId = methodParams.stream()
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        Map<UUID, List<ClassRelation>> relationsBySourceClassId = allRelations.stream()
                .collect(Collectors.groupingBy(r -> r.getClassEntity().getId()));

        return new LabChallengeStructureBundle(
                masterDataCache.get(),
                classesByChallengeId,
                fieldsByClassId,
                methodsByClassId,
                constructorsByClassId,
                paramsByConstructorId,
                paramsByMethodId,
                relationsBySourceClassId);
    }

    private static LabChallengeStructureBundle emptyStructureBundle() {
        return new LabChallengeStructureBundle(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());
    }

    public List<MmdClassDTO> getMmdData(UUID labId, UUID challengeId, UUID studentId, UUID submissionId) {
        long start = System.currentTimeMillis();
        UUID resolvedSubmissionId = submissionId != null
                ? submissionId
                : submissionResolutionService.resolveLatestSubmissionId(labId, studentId);
        if (resolvedSubmissionId == null) {
            return List.of();
        }
        List<MmdClassDTO> result = buildMmdDataForSubmission(resolvedSubmissionId, challengeId);
        if (timingLog) {
            System.out.printf("read_timing mmd_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }

    public List<MmdClassDTO> buildMmdDataForSubmission(UUID submissionId, UUID challengeId) {
        return buildMmdDataForSubmission(submissionId, challengeId, null, null);
    }

    public List<MmdClassDTO> buildMmdDataForSubmission(UUID submissionId,
                                                      UUID challengeId,
                                                      MmdGradingOutcome mmdOutcome,
                                                      Boolean mmdSubmittedOverride) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }
        LabChallengeStructureBundle structure = loadChallengeStructures(List.of(challengeId));
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);
        ChallengeMmdMeta mmdMeta = submissionMmdMetaStore.get(submissionId, challengeId);
        ChallengeSnapshot snapshot = parsedSubmissionSnapshotStore.get(submissionId, challengeId);
        return buildMmdData(structure, challengeId, correctIds, mmdOutcome, mmdSubmittedOverride, mmdMeta, submissionId, snapshot);
    }

    public List<MmdClassDTO> buildMmdData(LabChallengeStructureBundle structure,
                                          UUID challengeId,
                                          SubmissionCorrectIds correctIds,
                                          MmdGradingOutcome mmdOutcome,
                                          Boolean mmdSubmittedOverride,
                                          ChallengeMmdMeta mmdMeta,
                                          UUID submissionId) {
        return buildMmdData(structure, challengeId, correctIds, mmdOutcome, mmdSubmittedOverride, mmdMeta, submissionId, null);
    }

    public List<MmdClassDTO> buildMmdData(LabChallengeStructureBundle structure,
                                          UUID challengeId,
                                          SubmissionCorrectIds correctIds,
                                          MmdGradingOutcome mmdOutcome,
                                          Boolean mmdSubmittedOverride,
                                          ChallengeMmdMeta mmdMeta,
                                          UUID submissionId,
                                          ChallengeSnapshot snapshot) {
        List<ClassEntity> classes = structure.classesForChallenge(challengeId);
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> masterData = structure.masterData();
        ChallengeMmdMeta effectiveMeta = mmdMeta != null ? mmdMeta : new ChallengeMmdMeta();
        boolean effectiveMmdSubmitted = mmdSubmittedOverride != null
                ? mmdSubmittedOverride
                : resolveEffectiveMmdSubmitted(submissionId, effectiveMeta, correctIds);

        ParsedSubmissionSnapshot.MmdSnapshot mmdSnapshot = snapshot != null ? snapshot.mmdSnapshot : null;

        List<MmdClassDTO> result = new ArrayList<>();
        for (ClassEntity classEntity : classes) {
            UUID classId = classEntity.getId();
            String classIdStr = classId.toString();
            boolean stereotypeOk;
            if (mmdOutcome != null) {
                stereotypeOk = mmdOutcome.isClassPresent(classId) && mmdOutcome.isClassCorrect(classId);
            } else {
                stereotypeOk = effectiveMeta.classStereotypeCorrect.getOrDefault(classIdStr, false);
                if (!stereotypeOk && effectiveMmdSubmitted && effectiveMeta.classStereotypeCorrect.isEmpty()) {
                    stereotypeOk = classHasMergedCorrectMember(
                            classId,
                            structure.fieldsByClassId(),
                            structure.methodsByClassId(),
                            structure.constructorsByClassId(),
                            correctIds);
                }
            }

            List<MmdAttributeDTO> attributes = new ArrayList<>();
            String stereotypeDisplay = mmdSnapshot != null
                    ? mmdSnapshot.stereotypes.get(classIdStr)
                    : null;
            attributes.add(new MmdAttributeDTO(
                    stereotypeDisplay != null
                            ? stereotypeDisplay
                            : "<<" + resolveClassTypeLabel(classEntity, masterData).toLowerCase() + ">>",
                    "stereotype",
                    stereotypeOk,
                    stereotypeOk ? null : (effectiveMmdSubmitted ? "Class missing from diagram" : "Missing MMD file")));

            structure.fieldsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(field -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isFieldCorrect(field.getId())
                        : correctIds.fieldIds().contains(field.getId());
                String displayName = snapshotAttributeName(mmdSnapshot, field.getId(), formatFieldName(field));
                attributes.add(new MmdAttributeDTO(
                        displayName,
                        "field",
                        ok,
                        ok ? null : "Field mismatch"));
            });

            structure.constructorsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(constructor -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isConstructorCorrect(constructor.getId())
                        : correctIds.constructorIds().contains(constructor.getId());
                String rubricName = formatConstructorName(constructor,
                        structure.paramsByConstructorId().getOrDefault(constructor.getId(), List.of()));
                attributes.add(new MmdAttributeDTO(
                        snapshotAttributeName(mmdSnapshot, constructor.getId(), rubricName),
                        "constructor",
                        ok,
                        ok ? null : "Constructor mismatch"));
            });

            structure.methodsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(method -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isMethodCorrect(method.getId())
                        : correctIds.methodIds().contains(method.getId());
                String rubricName = formatMethodName(method,
                        structure.paramsByMethodId().getOrDefault(method.getId(), List.of()));
                attributes.add(new MmdAttributeDTO(
                        snapshotAttributeName(mmdSnapshot, method.getId(), rubricName),
                        "method",
                        ok,
                        ok ? null : "Method mismatch"));
            });

            List<MmdRelationDTO> relations = structure.relationsBySourceClassId()
                    .getOrDefault(classEntity.getId(), List.of()).stream()
                    .map(relation -> {
                        boolean ok = mmdOutcome != null
                                ? mmdOutcome.isRelationCorrect(relation.getId())
                                : correctIds.relationIds().contains(relation.getId());
                        String error = ok
                                ? null
                                : effectiveMeta.relationErrors.getOrDefault(
                                        relation.getId().toString(),
                                        effectiveMmdSubmitted ? "Relation mismatch" : "Missing relationship");
                        MmdRelationEntry relationEntry = mmdSnapshot != null
                                ? mmdSnapshot.relations.get(relation.getId().toString())
                                : null;
                        String from = relationEntry != null
                                ? relationEntry.from
                                : relation.getClassEntity().getName();
                        String to = relationEntry != null
                                ? relationEntry.to
                                : relation.getTargetClassEntity().getName();
                        String relType = relationEntry != null
                                ? relationEntry.relType
                                : MmdComparisonService.normalizeRelationTypeName(relation.getRelationType().getName());
                        return new MmdRelationDTO(from, to, relType, ok, error);
                    })
                    .toList();

            result.add(new MmdClassDTO(classEntity.getName(), attributes, relations));
        }
        return result;
    }

    /**
     * MMD meta is stored in ephemeral {@code SUBMISSION_BASE_DIR/_mmd_meta}. When that file is
     * missing (deploy wipe, submissions graded before meta existed), infer submission from persisted
     * grading results so the MMD tab does not show "Missing MMD file" for every class.
     */
    private boolean resolveEffectiveMmdSubmitted(UUID submissionId,
                                                 ChallengeMmdMeta mmdMeta,
                                                 SubmissionCorrectIds correctIds) {
        if (mmdMeta.mmdSubmitted) {
            return true;
        }
        if (!mmdMeta.relationErrors.isEmpty()) {
            return true;
        }
        if (!mmdMeta.classStereotypeCorrect.isEmpty()) {
            return mmdMeta.mmdSubmitted;
        }
        if (!correctIds.fieldIds().isEmpty()
                || !correctIds.methodIds().isEmpty()
                || !correctIds.constructorIds().isEmpty()
                || !correctIds.relationIds().isEmpty()) {
            return true;
        }
        return submissionResultLoader.hasAnyResults(submissionId);
    }

    private static boolean classHasMergedCorrectMember(
            UUID classId,
            Map<UUID, List<Field>> fieldsByClass,
            Map<UUID, List<Method>> methodsByClass,
            Map<UUID, List<Constructor>> constructorsByClass,
            SubmissionCorrectIds correctIds) {
        return fieldsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(f -> correctIds.fieldIds().contains(f.getId()))
                || methodsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(m -> correctIds.methodIds().contains(m.getId()))
                || constructorsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(c -> correctIds.constructorIds().contains(c.getId()));
    }

    private String resolveClassTypeLabel(ClassEntity classEntity, Map<Integer, String> masterData) {
        String declaringType = masterData.getOrDefault(classEntity.getDeclaringType(), "CLASS");
        return declaringType;
    }

    private String formatFieldName(Field field) {
        return field.getName() + ": " + field.getFieldDeclaration().getDataType();
    }

    private String formatConstructorName(Constructor constructor, List<Parameter> params) {
        String paramList = params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(Parameter::getName)
                .collect(Collectors.joining(", "));
        return constructor.getName() + "(" + paramList + ")";
    }

    private String formatMethodName(Method method, List<Parameter> params) {
        String paramList = params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(Parameter::getName)
                .collect(Collectors.joining(", "));
        return method.getName() + "(" + paramList + ") " + method.getMethodDeclaration().getReturnType();
    }

    private String snapshotAttributeName(ParsedSubmissionSnapshot.MmdSnapshot mmdSnapshot,
                                         UUID elementId,
                                         String rubricFallback) {
        if (mmdSnapshot == null || elementId == null) {
            return rubricFallback;
        }
        return mmdSnapshot.attributes.getOrDefault(elementId.toString(), rubricFallback);
    }

    /** Powers the "Class" tab for the student's latest attempt. */
    public List<ClassDetailDTO> getClassData(UUID labId, UUID challengeId, UUID studentId, UUID submissionId) {
        long start = System.currentTimeMillis();
        UUID resolvedSubmissionId = submissionId != null
                ? submissionId
                : submissionResolutionService.resolveLatestSubmissionId(labId, studentId);
        if (resolvedSubmissionId == null) {
            return List.of();
        }
        List<ClassDetailDTO> result = buildClassDataForSubmission(resolvedSubmissionId, challengeId);
        if (timingLog) {
            System.out.printf("read_timing class_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }

    public List<ClassDetailDTO> buildClassDataForSubmission(UUID submissionId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }
        LabChallengeStructureBundle structure = loadChallengeStructures(List.of(challengeId));
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);
        String compileError = compileErrorStore.get(submissionId, challengeId);
        ChallengeSnapshot snapshot = parsedSubmissionSnapshotStore.get(submissionId, challengeId);
        return buildClassData(structure, challengeId, correctIds, compileError, snapshot);
    }

    public List<ClassDetailDTO> buildClassData(LabChallengeStructureBundle structure,
                                               UUID challengeId,
                                               SubmissionCorrectIds correctIds,
                                               String compileError) {
        return buildClassData(structure, challengeId, correctIds, compileError, null);
    }

    public List<ClassDetailDTO> buildClassData(LabChallengeStructureBundle structure,
                                               UUID challengeId,
                                               SubmissionCorrectIds correctIds,
                                               String compileError,
                                               ChallengeSnapshot snapshot) {
        List<ClassEntity> classes = structure.classesForChallenge(challengeId);
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> masterData = structure.masterData();
        ParsedSubmissionSnapshot.ClassSnapshot classSnapshot = snapshot != null ? snapshot.classSnapshot : null;
        List<ClassDetailDTO> result = new ArrayList<>();
        for (ClassEntity ce : classes) {
            List<ClassFieldDetailDTO> fields = structure.fieldsByClassId().getOrDefault(ce.getId(), List.of()).stream()
                    .map(f -> {
                        boolean ok = correctIds.fieldIds().contains(f.getId());
                        ClassFieldEntry entry = classSnapshot != null
                                ? classSnapshot.fields.get(f.getId().toString())
                                : null;
                        if (entry != null) {
                            return new ClassFieldDetailDTO(entry.name, entry.scope, entry.dataType, ok);
                        }
                        return new ClassFieldDetailDTO(
                                f.getName(),
                                resolveMasterDataLabel(f.getFieldDeclaration().getScope(), masterData),
                                f.getFieldDeclaration().getDataType(),
                                ok);
                    })
                    .toList();

            List<ClassConstructorDetailDTO> constructors = structure.constructorsByClassId()
                    .getOrDefault(ce.getId(), List.of()).stream()
                    .map(c -> {
                        boolean ok = correctIds.constructorIds().contains(c.getId());
                        ClassConstructorEntry entry = classSnapshot != null
                                ? classSnapshot.constructors.get(c.getId().toString())
                                : null;
                        if (entry != null) {
                            return new ClassConstructorDetailDTO(entry.name, entry.scope, entry.params, ok);
                        }
                        return new ClassConstructorDetailDTO(
                                c.getName(),
                                resolveMasterDataLabel(c.getConstructorDeclaration().getScope(), masterData),
                                formatParams(structure.paramsByConstructorId().getOrDefault(c.getId(), List.of()), true),
                                ok);
                    })
                    .toList();

            List<ClassMethodDetailDTO> methods = structure.methodsByClassId().getOrDefault(ce.getId(), List.of()).stream()
                    .map(m -> {
                        boolean ok = correctIds.methodIds().contains(m.getId());
                        ClassMethodEntry entry = classSnapshot != null
                                ? classSnapshot.methods.get(m.getId().toString())
                                : null;
                        if (entry != null) {
                            return new ClassMethodDetailDTO(entry.name, entry.scope, entry.returnType, ok);
                        }
                        return new ClassMethodDetailDTO(
                                m.getName(),
                                resolveMasterDataLabel(m.getMethodDeclaration().getScope(), masterData),
                                m.getMethodDeclaration().getReturnType(),
                                ok);
                    })
                    .toList();

            result.add(new ClassDetailDTO(
                    ce.getName(),
                    resolveClassType(ce, masterData),
                    compileError != null ? "error" : resolveStatus(fields, constructors, methods),
                    compileError,
                    fields, constructors, methods));
        }
        return result;
    }

    private String resolveClassType(ClassEntity ce, Map<Integer, String> masterData) {
        String declaringType = masterData.getOrDefault(ce.getDeclaringType(), "CLASS");
        return ce.isAbstract() ? "ABSTRACT " + declaringType : declaringType;
    }

    private String resolveMasterDataLabel(MasterData masterData, Map<Integer, String> valueMap) {
        if (masterData == null) {
            return "-";
        }
        Integer id = masterData.getId();
        if (id == null) {
            return masterData.getName() != null ? masterData.getName() : "-";
        }
        return valueMap.getOrDefault(id, masterData.getName() != null ? masterData.getName() : "-");
    }

    private String resolveStatus(List<ClassFieldDetailDTO> fields,
                                  List<ClassConstructorDetailDTO> constructors,
                                  List<ClassMethodDetailDTO> methods) {
        long total = fields.size() + constructors.size() + methods.size();
        if (total == 0) return "info";

        long correct = fields.stream().filter(ClassFieldDetailDTO::ok).count()
                + constructors.stream().filter(ClassConstructorDetailDTO::ok).count()
                + methods.stream().filter(ClassMethodDetailDTO::ok).count();

        if (correct == total) return "success";
        if (correct == 0) return "error";
        return "warning";
    }

    private String formatParams(List<Parameter> params, boolean includeType) {
        return params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(p -> includeType ? (p.getDataType() + " " + p.getName()) : p.getName())
                .collect(Collectors.joining(", "));
    }
}