package com.eiu.capstone.backend.grading;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.model.SubmissionRelationResult;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.CompletableFutures;

@Service
public class GradingService {

    private static final Pattern CHALLENGE_NUMBER_PATTERN = Pattern.compile("challenge_(\\d+)");

    private final ChallengeRepository challengeRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ReflectionClassParser reflectionClassParser;
    private final MmdParser mmdParser;
    private final MmdComparisonService mmdComparisonService;
    private final ExecutorService gradingExecutor;
    private final GradingResultStore gradingResultStore;
    private final ClassRelationRepository classRelationRepository;

    public GradingService(ChallengeRepository challengeRepository,
                          FieldRepository fieldRepository,
                          MethodRepository methodRepository,
                          ConstructorRepository constructorRepository,
                          ReflectionClassParser reflectionClassParser,
                          MmdParser mmdParser,
                          MmdComparisonService mmdComparisonService,
                          @Qualifier("gradingExecutor") ExecutorService gradingExecutor,
                          GradingResultStore gradingResultStore,
                          ClassRelationRepository classRelationRepository) {
        this.challengeRepository = challengeRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.reflectionClassParser = reflectionClassParser;
        this.mmdParser = mmdParser;
        this.mmdComparisonService = mmdComparisonService;
        this.gradingExecutor = gradingExecutor;
        this.gradingResultStore = gradingResultStore;
        this.classRelationRepository = classRelationRepository;
    }

    public GradingOutcome gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<SubmissionStorageService.ChallengeResult> challengeFolderResults,
                                      Map<String, List<MultipartFile>> mmdByChallenge,
                                      boolean skipExistingLoad) {

        GradingService.ExistingResults existing = skipExistingLoad
                ? emptyExistingResults()
                : gradingResultStore.loadExisting(submission);
        GradingComputationResult computed = computeAgainstSnapshot(
                rubric, challengeFolderResults, mmdByChallenge, submission, existing);
        gradingResultStore.save(computed);

        return new GradingOutcome(computed.overallScore, computed.gradedChallenges, computed.mmdMetaByChallengeId);
    }

    private static ExistingResults emptyExistingResults() {
        ExistingResults existing = new ExistingResults();
        existing.fieldResults = Map.of();
        existing.methodResults = Map.of();
        existing.constructorResults = Map.of();
        existing.relationResults = Map.of();
        existing.challengeResults = Map.of();
        return existing;
    }

    private GradingComputationResult computeAgainstSnapshot(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challengeFolderResults,
            Map<String, List<MultipartFile>> mmdByChallenge,
            LabSubmission submission,
            ExistingResults existing) {

        List<CompletableFuture<ChallengeComputation>> futures = challengeFolderResults.stream()
                .map(folderResult -> CompletableFuture.supplyAsync(
                        () -> gradeChallengeFolder(rubric, folderResult,
                                mmdByChallenge.getOrDefault(folderResult.challengeName, List.of())),
                        gradingExecutor))
                .collect(Collectors.toList());

        List<ChallengeComputation> challengeComputations = CompletableFutures.joinAll(futures);

        GradingComputationResult result = new GradingComputationResult();
        result.fieldResults = new ArrayList<>();
        result.methodResults = new ArrayList<>();
        result.constructorResults = new ArrayList<>();
        result.relationResults = new ArrayList<>();
        result.challengeResults = new ArrayList<>();
        result.challengePercentages = new ArrayList<>();
        result.gradedChallenges = new ArrayList<>();
        result.mmdMetaByChallengeId = new java.util.LinkedHashMap<>();

        Map<Integer, BigDecimal> percentagesByChallengeNumber = new java.util.LinkedHashMap<>();
        for (ChallengeComputation cc : challengeComputations) {
            if (cc == null) continue;
            if (cc.challengeId != null && cc.mmdMeta != null) {
                result.mmdMetaByChallengeId.put(cc.challengeId, cc.mmdMeta);
            }
            for (PendingFieldResult pending : cc.pendingFields) {
                result.fieldResults.add(buildFieldResult(existing.fieldResults, submission, pending.fieldId(), pending.correct()));
            }
            for (PendingMethodResult pending : cc.pendingMethods) {
                result.methodResults.add(buildMethodResult(existing.methodResults, submission, pending.methodId(), pending.correct()));
            }
            for (PendingConstructorResult pending : cc.pendingConstructors) {
                result.constructorResults.add(buildConstructorResult(
                        existing.constructorResults, submission, pending.constructorId(), pending.correct()));
            }
            for (PendingRelationResult pending : cc.pendingRelations) {
                result.relationResults.add(buildRelationResult(
                        existing.relationResults, submission, pending.relationId(), pending.correct()));
            }
            if (cc.pendingChallenge != null) {
                BigDecimal challengeScore = cc.percentage != null ? cc.percentage : BigDecimal.ZERO;
                result.challengeResults.add(buildChallengeResult(
                        existing.challengeResults,
                        submission,
                        cc.pendingChallenge.challengeId(),
                        cc.pendingChallenge.correct(),
                        challengeScore));
            }
            if (cc.challengeNumber != null && cc.percentage != null) {
                percentagesByChallengeNumber.put(cc.challengeNumber, cc.percentage);
            }
            if (cc.challengeId != null && cc.percentage != null) {
                result.gradedChallenges.add(new GradedChallengeSummary(
                        cc.challengeId,
                        cc.percentage.setScale(0, RoundingMode.HALF_UP).intValue()));
            }
        }

        List<BigDecimal> overallChallengeScores = new ArrayList<>();
        for (ChallengeRubric challengeRubric : rubric.byChallengeNumber().values().stream()
                .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
                .toList()) {
            BigDecimal challengeScore = percentagesByChallengeNumber.getOrDefault(challengeRubric.challengeNumber(), BigDecimal.ZERO);
            result.challengePercentages.add(challengeScore);
            overallChallengeScores.add(challengeScore);
        }

        result.overallScore = overallChallengeScores.isEmpty()
                ? BigDecimal.ZERO
                : average(overallChallengeScores);
        return result;
    }

    private ChallengeComputation gradeChallengeFolder(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult,
            List<MultipartFile> mmdFiles) {

        Integer challengeNumber = extractChallengeNumber(folderResult.challengeName);
        if (challengeNumber == null) {
            return null;
        }

        ChallengeRubric challengeRubric = rubric.challenge(challengeNumber).orElse(null);
        if (challengeRubric == null) {
            return null;
        }

        Path classesDir = folderResult.folder.resolve("classes");
        List<ParsedClass> parsedClasses = Files.exists(classesDir)
                ? reflectionClassParser.parseClasses(classesDir)
                : List.of();
        Map<String, ParsedClass> parsedByName = parsedClasses.stream()
                .collect(Collectors.toMap(pc -> pc.simpleName, pc -> pc, (a, b) -> a));

        ChallengeComputation computation = new ChallengeComputation();
        computation.challengeNumber = challengeNumber;
        computation.challengeId = challengeRubric.challengeId();
        computation.challengeName = challengeRubric.name();
        computation.pendingFields = new ArrayList<>();
        computation.pendingMethods = new ArrayList<>();
        computation.pendingConstructors = new ArrayList<>();
        computation.pendingRelations = new ArrayList<>();

        MmdParseBundle mmdBundle = parseAndGradeMmd(challengeRubric, mmdFiles);
        MmdGradingOutcome mmdOutcome = mmdBundle.outcome();

        int totalElements = 0;
        int javaCorrectElements = 0;
        int mmdCorrectElements = 0;
        boolean challengeFullyCorrect = true;
        List<ClassGradeReport> classReports = new ArrayList<>();

        for (ClassRubric expectedClass : challengeRubric.classes()) {
            ClassGradeReport report = new ClassGradeReport();
            report.className = expectedClass.name();

            ParsedClass parsed = parsedByName.get(expectedClass.name());
            report.matched = parsed != null;
            totalElements++;

            if (parsed == null) {
                challengeFullyCorrect = false;
                boolean mmdClassCorrect = mmdOutcome.isClassCorrect(expectedClass.id());
                if (!mmdClassCorrect) {
                    // class element already counted
                }
                for (FieldRubric f : expectedClass.fields()) {
                    totalElements++;
                    report.missingFields.add(f.name());
                    computation.pendingFields.add(new PendingFieldResult(f.id(), false));
                }
                for (MethodRubric m : expectedClass.methods()) {
                    totalElements++;
                    report.missingMethods.add(signatureLabel(m.name(), m.parameterTypes()));
                    computation.pendingMethods.add(new PendingMethodResult(m.id(), false));
                }
                for (ConstructorRubric c : expectedClass.constructors()) {
                    totalElements++;
                    report.missingConstructors.add(signatureLabel(expectedClass.name(), c.parameterTypes()));
                    computation.pendingConstructors.add(new PendingConstructorResult(c.id(), false));
                }
                classReports.add(report);
                continue;
            }

            boolean javaClassCorrect = classAttributesMatch(expectedClass, parsed);
            boolean mmdClassCorrect = mmdOutcome.isClassCorrect(expectedClass.id());
            report.classAttributesCorrect = javaClassCorrect && mmdClassCorrect;
            if (javaClassCorrect) {
                javaCorrectElements++;
            }
            if (mmdClassCorrect) {
                mmdCorrectElements++;
            }
            if (report.classAttributesCorrect) {
                challengeFullyCorrect = challengeFullyCorrect && true;
            } else {
                challengeFullyCorrect = false;
            }

            Map<String, ParsedField> parsedFieldsByName = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name, f -> f, (a, b) -> a));
            for (FieldRubric expectedField : expectedClass.fields()) {
                totalElements++;
                ParsedField pf = parsedFieldsByName.get(expectedField.name());
                boolean javaCorrect = pf != null && fieldAttributesMatch(expectedField, pf);
                boolean mmdCorrect = mmdOutcome.isFieldCorrect(expectedField.id());
                boolean correct = javaCorrect && mmdCorrect;
                if (javaCorrect) {
                    javaCorrectElements++;
                }
                if (mmdCorrect) {
                    mmdCorrectElements++;
                }
                if (correct) {
                    challengeFullyCorrect = challengeFullyCorrect && true;
                } else {
                    challengeFullyCorrect = false;
                    (pf == null ? report.missingFields : report.incorrectFields).add(expectedField.name());
                }
                computation.pendingFields.add(new PendingFieldResult(expectedField.id(), correct));
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                totalElements++;
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.name(), expectedMethod.parameterTypes());
                boolean javaCorrect = match != null && methodAttributesMatch(expectedMethod, match);
                boolean mmdCorrect = mmdOutcome.isMethodCorrect(expectedMethod.id());
                boolean correct = javaCorrect && mmdCorrect;
                if (javaCorrect) {
                    javaCorrectElements++;
                }
                if (mmdCorrect) {
                    mmdCorrectElements++;
                }
                if (correct) {
                    challengeFullyCorrect = challengeFullyCorrect && true;
                } else {
                    challengeFullyCorrect = false;
                    (match == null ? report.missingMethods : report.incorrectMethods)
                            .add(signatureLabel(expectedMethod.name(), expectedMethod.parameterTypes()));
                }
                computation.pendingMethods.add(new PendingMethodResult(expectedMethod.id(), correct));
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                totalElements++;
                ParsedConstructor match = findMatchingConstructor(parsed.constructors, expectedConstructor.parameterTypes());
                boolean javaCorrect = match != null && constructorAttributesMatch(expectedConstructor, match);
                boolean mmdCorrect = mmdOutcome.isConstructorCorrect(expectedConstructor.id());
                boolean correct = javaCorrect && mmdCorrect;
                if (javaCorrect) {
                    javaCorrectElements++;
                }
                if (mmdCorrect) {
                    mmdCorrectElements++;
                }
                if (correct) {
                    challengeFullyCorrect = challengeFullyCorrect && true;
                } else {
                    challengeFullyCorrect = false;
                    (match == null ? report.missingConstructors : report.incorrectConstructors)
                            .add(signatureLabel(expectedClass.name(), expectedConstructor.parameterTypes()));
                }
                computation.pendingConstructors.add(new PendingConstructorResult(expectedConstructor.id(), correct));
            }

            classReports.add(report);
        }

        for (RelationRubric expectedRelation : challengeRubric.relations()) {
            totalElements++;
            boolean mmdCorrect = mmdOutcome.isRelationCorrect(expectedRelation.id());
            boolean correct = mmdCorrect;
            if (mmdCorrect) {
                mmdCorrectElements++;
            } else {
                challengeFullyCorrect = false;
            }
            computation.pendingRelations.add(new PendingRelationResult(expectedRelation.id(), correct));
        }

        computation.pendingChallenge = new PendingChallengeResult(challengeRubric.challengeId(), challengeFullyCorrect);
        computation.mmdMeta = buildMmdMeta(challengeRubric, mmdBundle, mmdOutcome);
        computation.percentage = calculateChallengePercentage(javaCorrectElements, mmdCorrectElements, totalElements);
        computation.fullyCorrect = challengeFullyCorrect;
        computation.classReports = classReports;
        return computation;
    }

    private boolean classAttributesMatch(ClassRubric expected, ParsedClass actual) {
        return equalsIgnoreCase(expected.scope(), actual.scope)
                && equalsIgnoreCase(expected.declaringType(), actual.declaringType)
                && expected.isAbstract() == actual.isAbstract;
    }

    private boolean fieldAttributesMatch(FieldRubric expected, ParsedField actual) {
        return equalsIgnoreCase(expected.scope(), actual.scope)
                && equalsIgnoreCase(expected.dataType(), actual.dataType);
    }

    private boolean methodAttributesMatch(MethodRubric expected, ParsedMethod actual) {
        return equalsIgnoreCase(expected.scope(), actual.scope)
                && equalsIgnoreCase(expected.returnType(), actual.returnType)
                && expected.isStatic() == actual.isStatic
                && expected.isAbstract() == actual.isAbstract
                && expected.isFinal() == actual.isFinal;
    }

    private boolean constructorAttributesMatch(ConstructorRubric expected, ParsedConstructor actual) {
        boolean actualDefault = actual.parameterTypes.isEmpty();
        return equalsIgnoreCase(expected.scope(), actual.scope)
                && expected.isDefault() == actualDefault;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private ParsedMethod findMatchingMethod(List<ParsedMethod> candidates, String name, List<String> expectedParamTypes) {
        for (ParsedMethod pm : candidates) {
            if (pm.name.equals(name) && sameTypes(pm.parameterTypes, expectedParamTypes)) {
                return pm;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates, List<String> expectedParamTypes) {
        for (ParsedConstructor pc : candidates) {
            if (sameTypes(pc.parameterTypes, expectedParamTypes)) {
                return pc;
            }
        }
        return null;
    }

    private boolean sameTypes(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!equalsIgnoreCase(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private String signatureLabel(String name, List<String> paramTypes) {
        return name + "(" + String.join(", ", paramTypes) + ")";
    }

    private Integer extractChallengeNumber(String challengeFolderKey) {
        Matcher m = CHALLENGE_NUMBER_PATTERN.matcher(challengeFolderKey);
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

    static BigDecimal calculateChallengePercentage(int javaCorrectCount, int mmdCorrectCount, int totalElements) {
        if (totalElements <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal javaPercentage = BigDecimal.valueOf(javaCorrectCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalElements), 2, RoundingMode.HALF_UP);
        BigDecimal mmdPercentage = BigDecimal.valueOf(mmdCorrectCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalElements), 2, RoundingMode.HALF_UP);
        return javaPercentage.add(mmdPercentage)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private SubmissionFieldResult buildFieldResult(Map<UUID, SubmissionFieldResult> existing,
                                                   LabSubmission submission,
                                                   UUID fieldId,
                                                   boolean correct) {
        SubmissionFieldResult result = existing.getOrDefault(fieldId, new SubmissionFieldResult());
        result.setSubmission(submission);
        result.setField(fieldRepository.getReferenceById(fieldId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionMethodResult buildMethodResult(Map<UUID, SubmissionMethodResult> existing,
                                                     LabSubmission submission,
                                                     UUID methodId,
                                                     boolean correct) {
        SubmissionMethodResult result = existing.getOrDefault(methodId, new SubmissionMethodResult());
        result.setSubmission(submission);
        result.setMethod(methodRepository.getReferenceById(methodId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionConstructorResult buildConstructorResult(Map<UUID, SubmissionConstructorResult> existing,
                                                               LabSubmission submission,
                                                               UUID constructorId,
                                                               boolean correct) {
        SubmissionConstructorResult result = existing.getOrDefault(constructorId, new SubmissionConstructorResult());
        result.setSubmission(submission);
        result.setConstructor(constructorRepository.getReferenceById(constructorId));
        result.setCorrect(correct);
        return result;
    }

    private MmdParseBundle parseAndGradeMmd(ChallengeRubric challengeRubric, List<MultipartFile> mmdFiles) {
        MmdGradingOutcome.ChallengeRubricElements elements = collectRubricElements(challengeRubric);
        byte[] content = readFirstMmd(mmdFiles);
        boolean mmdSubmitted = content != null && content.length > 0;
        if (!mmdSubmitted) {
            return new MmdParseBundle(MmdGradingOutcome.allIncorrect(elements), null, false);
        }
        try {
            ParsedMmdDiagram diagram = mmdParser.parseBytes(content);
            return new MmdParseBundle(mmdComparisonService.compare(challengeRubric, diagram), diagram, true);
        } catch (MmdParseException ex) {
            return new MmdParseBundle(MmdGradingOutcome.allIncorrect(elements), null, true);
        }
    }

    private com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta buildMmdMeta(
            ChallengeRubric challengeRubric,
            MmdParseBundle mmdBundle,
            MmdGradingOutcome mmdOutcome) {
        com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta meta =
                new com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta();
        meta.mmdSubmitted = mmdBundle.mmdSubmitted();
        Map<String, Boolean> stereotypeMap = new java.util.HashMap<>();
        for (ClassRubric expectedClass : challengeRubric.classes()) {
            stereotypeMap.put(expectedClass.id().toString(), mmdOutcome.isClassPresent(expectedClass.id()));
        }
        meta.classStereotypeCorrect = stereotypeMap;

        Map<String, String> relationErrors = new java.util.HashMap<>();
        for (RelationRubric expectedRelation : challengeRubric.relations()) {
            if (mmdOutcome.isRelationCorrect(expectedRelation.id())) {
                continue;
            }
            relationErrors.put(
                    expectedRelation.id().toString(),
                    resolveRelationError(mmdBundle, expectedRelation));
        }
        meta.relationErrors = relationErrors;
        return meta;
    }

    private String resolveRelationError(MmdParseBundle mmdBundle, RelationRubric expectedRelation) {
        if (!mmdBundle.mmdSubmitted() || mmdBundle.diagram() == null) {
            return "Missing relationship";
        }
        if (mmdComparisonService.relationPresentInDiagram(expectedRelation, mmdBundle.diagram())) {
            return "Relation mismatch";
        }
        return "Missing relationship";
    }

    private record MmdParseBundle(MmdGradingOutcome outcome, ParsedMmdDiagram diagram, boolean mmdSubmitted) {}

    private byte[] readFirstMmd(List<MultipartFile> mmdFiles) {
        if (mmdFiles == null || mmdFiles.isEmpty()) {
            return null;
        }
        return mmdFiles.stream()
                .sorted(Comparator.comparing(MultipartFile::getOriginalFilename, String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .map(file -> {
                    try {
                        return file.getBytes();
                    } catch (IOException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private MmdGradingOutcome.ChallengeRubricElements collectRubricElements(ChallengeRubric rubric) {
        return new MmdGradingOutcome.ChallengeRubricElements(
                rubric.classes().stream().map(ClassRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.fields().stream()).map(FieldRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.methods().stream()).map(MethodRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.constructors().stream()).map(ConstructorRubric::id).toList(),
                rubric.relations().stream().map(RelationRubric::id).toList());
    }

    private SubmissionRelationResult buildRelationResult(Map<UUID, SubmissionRelationResult> existing,
                                                         LabSubmission submission,
                                                         UUID relationId,
                                                         boolean correct) {
        SubmissionRelationResult result = existing.getOrDefault(relationId, new SubmissionRelationResult());
        result.setSubmission(submission);
        result.setClassRelation(classRelationRepository.getReferenceById(relationId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionChallengeResult buildChallengeResult(Map<UUID, SubmissionChallengeResult> existing,
                                                           LabSubmission submission,
                                                           UUID challengeId,
                                                           boolean correct,
                                                           BigDecimal score) {
        SubmissionChallengeResult result = existing.getOrDefault(challengeId, new SubmissionChallengeResult());
        result.setSubmission(submission);
        result.setChallenge(challengeRepository.getReferenceById(challengeId));
        result.setCorrect(correct);
        result.setScore(score);
        return result;
    }

    static class ExistingResults {
        Map<UUID, SubmissionFieldResult> fieldResults;
        Map<UUID, SubmissionMethodResult> methodResults;
        Map<UUID, SubmissionConstructorResult> constructorResults;
        Map<UUID, SubmissionRelationResult> relationResults;
        Map<UUID, SubmissionChallengeResult> challengeResults;
    }

    static class GradingComputationResult {
        List<SubmissionFieldResult> fieldResults;
        List<SubmissionMethodResult> methodResults;
        List<SubmissionConstructorResult> constructorResults;
        List<SubmissionRelationResult> relationResults;
        List<SubmissionChallengeResult> challengeResults;
        List<BigDecimal> challengePercentages;
        List<GradedChallengeSummary> gradedChallenges;
        BigDecimal overallScore;
        Map<UUID, com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta> mmdMetaByChallengeId;
    }

    private static class ChallengeComputation {
        Integer challengeNumber;
        UUID challengeId;
        String challengeName;
        BigDecimal percentage;
        boolean fullyCorrect;
        List<ClassGradeReport> classReports;
        PendingChallengeResult pendingChallenge;
        List<PendingFieldResult> pendingFields;
        List<PendingMethodResult> pendingMethods;
        List<PendingConstructorResult> pendingConstructors;
        List<PendingRelationResult> pendingRelations;
        com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta mmdMeta;
    }

    private static class ClassGradeReport {
        public String className;
        public boolean matched;
        public boolean classAttributesCorrect;
        public List<String> missingFields = new ArrayList<>();
        public List<String> incorrectFields = new ArrayList<>();
        public List<String> missingMethods = new ArrayList<>();
        public List<String> incorrectMethods = new ArrayList<>();
        public List<String> missingConstructors = new ArrayList<>();
        public List<String> incorrectConstructors = new ArrayList<>();
    }
}
