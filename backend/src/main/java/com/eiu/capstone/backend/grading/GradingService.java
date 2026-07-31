package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.repository.ChallengeRepository;
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
    private final ExecutorService gradingExecutor;
    private final GradingResultStore gradingResultStore;

    public GradingService(ChallengeRepository challengeRepository,
                          FieldRepository fieldRepository,
                          MethodRepository methodRepository,
                          ConstructorRepository constructorRepository,
                          ReflectionClassParser reflectionClassParser,
                          ExecutorService gradingExecutor,
                          GradingResultStore gradingResultStore) {
        this.challengeRepository = challengeRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.reflectionClassParser = reflectionClassParser;
        this.gradingExecutor = gradingExecutor;
        this.gradingResultStore = gradingResultStore;
    }

    public BigDecimal gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<SubmissionStorageService.ChallengeResult> challengeFolderResults) {

        String irn = submission.getUser() != null ? submission.getUser().getIrn() : "unknown";
        System.out.println("=========================================");
        System.out.println("Grading submission " + submission.getId() + " (IRN: " + irn + ")");

        GradingService.ExistingResults existing = gradingResultStore.loadExisting(submission);
        GradingComputationResult computed = computeAgainstSnapshot(
                rubric, challengeFolderResults, submission, existing);
        gradingResultStore.save(computed);

        System.out.println("Overall score (simple average across " + computed.challengePercentages.size()
                + " challenge(s)): " + computed.overallScore + " / 100");
        System.out.println("=========================================");

        return computed.overallScore;
    }

    private GradingComputationResult computeAgainstSnapshot(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challengeFolderResults,
            LabSubmission submission,
            ExistingResults existing) {

        List<CompletableFuture<ChallengeComputation>> futures = challengeFolderResults.stream()
                .map(folderResult -> CompletableFuture.supplyAsync(
                        () -> gradeChallengeFolder(rubric, folderResult),
                        gradingExecutor))
                .collect(Collectors.toList());

        List<ChallengeComputation> challengeComputations = CompletableFutures.joinAll(futures);

        GradingComputationResult result = new GradingComputationResult();
        result.fieldResults = new ArrayList<>();
        result.methodResults = new ArrayList<>();
        result.constructorResults = new ArrayList<>();
        result.challengeResults = new ArrayList<>();
        result.challengePercentages = new ArrayList<>();

        for (ChallengeComputation cc : challengeComputations) {
            if (cc == null) continue;
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
            if (cc.pendingChallenge != null) {
                result.challengeResults.add(buildChallengeResult(
                        existing.challengeResults, submission, cc.pendingChallenge.challengeId(), cc.pendingChallenge.correct()));
            }
            if (cc.percentage != null) {
                result.challengePercentages.add(cc.percentage);
            }
            if (cc.challengeNumber != null) {
                printChallengeReport(cc);
            }
        }

        result.overallScore = result.challengePercentages.isEmpty()
                ? BigDecimal.ZERO
                : average(result.challengePercentages);
        return result;
    }

    private ChallengeComputation gradeChallengeFolder(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult) {

        Integer challengeNumber = extractChallengeNumber(folderResult.challengeName);
        if (challengeNumber == null) {
            System.out.println("  [skip] Folder '" + folderResult.challengeName
                    + "' doesn't look like challenge_<N> — cannot map to a Challenge row.");
            return null;
        }

        ChallengeRubric challengeRubric = rubric.challenge(challengeNumber).orElse(null);
        if (challengeRubric == null) {
            System.out.println("  [skip] No Challenge rubric for challenge_number=" + challengeNumber + ".");
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
        computation.challengeName = challengeRubric.name();
        computation.pendingFields = new ArrayList<>();
        computation.pendingMethods = new ArrayList<>();
        computation.pendingConstructors = new ArrayList<>();

        int totalElements = 0;
        int correctElements = 0;
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

            report.classAttributesCorrect = classAttributesMatch(expectedClass, parsed);
            if (report.classAttributesCorrect) {
                correctElements++;
            } else {
                challengeFullyCorrect = false;
            }

            Map<String, ParsedField> parsedFieldsByName = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name, f -> f, (a, b) -> a));
            for (FieldRubric expectedField : expectedClass.fields()) {
                totalElements++;
                ParsedField pf = parsedFieldsByName.get(expectedField.name());
                boolean correct = pf != null && fieldAttributesMatch(expectedField, pf);
                if (correct) {
                    correctElements++;
                } else {
                    challengeFullyCorrect = false;
                    (pf == null ? report.missingFields : report.incorrectFields).add(expectedField.name());
                }
                computation.pendingFields.add(new PendingFieldResult(expectedField.id(), correct));
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                totalElements++;
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.name(), expectedMethod.parameterTypes());
                boolean correct = match != null && methodAttributesMatch(expectedMethod, match);
                if (correct) {
                    correctElements++;
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
                boolean correct = match != null && constructorAttributesMatch(expectedConstructor, match);
                if (correct) {
                    correctElements++;
                } else {
                    challengeFullyCorrect = false;
                    (match == null ? report.missingConstructors : report.incorrectConstructors)
                            .add(signatureLabel(expectedClass.name(), expectedConstructor.parameterTypes()));
                }
                computation.pendingConstructors.add(new PendingConstructorResult(expectedConstructor.id(), correct));
            }

            classReports.add(report);
        }

        computation.pendingChallenge = new PendingChallengeResult(challengeRubric.challengeId(), challengeFullyCorrect);
        computation.percentage = totalElements == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctElements)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalElements), 2, RoundingMode.HALF_UP);
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

    private SubmissionChallengeResult buildChallengeResult(Map<UUID, SubmissionChallengeResult> existing,
                                                           LabSubmission submission,
                                                           UUID challengeId,
                                                           boolean correct) {
        SubmissionChallengeResult result = existing.getOrDefault(challengeId, new SubmissionChallengeResult());
        result.setSubmission(submission);
        result.setChallenge(challengeRepository.getReferenceById(challengeId));
        result.setCorrect(correct);
        return result;
    }

    private void printChallengeReport(ChallengeComputation computation) {
        System.out.println("--- Challenge #" + computation.challengeNumber + ": " + computation.challengeName + " ---");
        System.out.println("  Score: " + computation.percentage + "% | Fully correct: " + computation.fullyCorrect);
        for (ClassGradeReport cr : computation.classReports) {
            String status = !cr.matched ? "MISSING" : (cr.classAttributesCorrect ? "OK" : "class declaration incorrect");
            System.out.println("  Class " + cr.className + ": " + status);
            printIfNotEmpty("    Missing fields", cr.missingFields);
            printIfNotEmpty("    Incorrect fields", cr.incorrectFields);
            printIfNotEmpty("    Missing methods", cr.missingMethods);
            printIfNotEmpty("    Incorrect methods", cr.incorrectMethods);
            printIfNotEmpty("    Missing constructors", cr.missingConstructors);
            printIfNotEmpty("    Incorrect constructors", cr.incorrectConstructors);
        }
    }

    private void printIfNotEmpty(String label, List<String> items) {
        if (!items.isEmpty()) {
            System.out.println(label + ": " + items);
        }
    }

    static class ExistingResults {
        Map<UUID, SubmissionFieldResult> fieldResults;
        Map<UUID, SubmissionMethodResult> methodResults;
        Map<UUID, SubmissionConstructorResult> constructorResults;
        Map<UUID, SubmissionChallengeResult> challengeResults;
    }

    static class GradingComputationResult {
        List<SubmissionFieldResult> fieldResults;
        List<SubmissionMethodResult> methodResults;
        List<SubmissionConstructorResult> constructorResults;
        List<SubmissionChallengeResult> challengeResults;
        List<BigDecimal> challengePercentages;
        BigDecimal overallScore;
    }

    private static class ChallengeComputation {
        Integer challengeNumber;
        String challengeName;
        BigDecimal percentage;
        boolean fullyCorrect;
        List<ClassGradeReport> classReports;
        PendingChallengeResult pendingChallenge;
        List<PendingFieldResult> pendingFields;
        List<PendingMethodResult> pendingMethods;
        List<PendingConstructorResult> pendingConstructors;
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
