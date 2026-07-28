package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.ConstructorDeclaration;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.FieldDeclaration;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.service.SubmissionStorageService;

@Service
public class GradingService {

    private static final Pattern CHALLENGE_NUMBER_PATTERN = Pattern.compile("challenge_(\\d+)");

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;
    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final ReflectionClassParser reflectionClassParser;

    public GradingService(ChallengeRepository challengeRepository,
                           ClassEntityRepository classEntityRepository,
                           FieldRepository fieldRepository,
                           MethodRepository methodRepository,
                           ConstructorRepository constructorRepository,
                           ParameterRepository parameterRepository,
                           SubmissionChallengeResultRepository submissionChallengeResultRepository,
                           SubmissionFieldResultRepository submissionFieldResultRepository,
                           SubmissionMethodResultRepository submissionMethodResultRepository,
                           SubmissionConstructorResultRepository submissionConstructorResultRepository,
                           ReflectionClassParser reflectionClassParser) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.reflectionClassParser = reflectionClassParser;
    }

    @Transactional
    public BigDecimal gradeSubmission(LabSubmission submission, Lab lab,
                                       List<SubmissionStorageService.ChallengeResult> challengeFolderResults) {

        String irn = submission.getUser() != null ? submission.getUser().getIrn() : "unknown";
        System.out.println("=========================================");
        System.out.println("Grading submission " + submission.getId() + " (IRN: " + irn + ", lab: " + lab.getId() + ")");

        Map<UUID, SubmissionFieldResult> existingFieldResults = submissionFieldResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getField().getId(), r -> r));
        Map<UUID, SubmissionMethodResult> existingMethodResults = submissionMethodResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getMethod().getId(), r -> r));
        Map<UUID, SubmissionConstructorResult> existingConstructorResults = submissionConstructorResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getConstructor().getId(), r -> r));
        Map<UUID, SubmissionChallengeResult> existingChallengeResults = submissionChallengeResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getChallenge().getId(), r -> r));

        List<SubmissionFieldResult> fieldResultsToSave = new ArrayList<>();
        List<SubmissionMethodResult> methodResultsToSave = new ArrayList<>();
        List<SubmissionConstructorResult> constructorResultsToSave = new ArrayList<>();
        List<SubmissionChallengeResult> challengeResultsToSave = new ArrayList<>();

        List<BigDecimal> challengePercentages = new ArrayList<>();

        for (SubmissionStorageService.ChallengeResult folderResult : challengeFolderResults) {
            Integer challengeNumber = extractChallengeNumber(folderResult.challengeName);
            if (challengeNumber == null) {
                System.out.println("  [skip] Folder '" + folderResult.challengeName
                        + "' doesn't look like challenge_<N> — cannot map to a Challenge row.");
                continue;
            }

            Optional<Challenge> challengeOpt = challengeRepository.findByLabAndChallengeNumber(lab, challengeNumber);
            if (challengeOpt.isEmpty()) {
                System.out.println("  [skip] No Challenge found for lab=" + lab.getId()
                        + " challenge_number=" + challengeNumber + ".");
                continue;
            }

            Challenge challenge = challengeOpt.get();
            Path classesDir = folderResult.folder.resolve("classes");

            GradingOutcome outcome = gradeChallenge(
                    submission, challenge, classesDir,
                    existingFieldResults, existingMethodResults, existingConstructorResults, existingChallengeResults,
                    fieldResultsToSave, methodResultsToSave, constructorResultsToSave, challengeResultsToSave);

            challengePercentages.add(outcome.percentage);
            printChallengeReport(challenge, outcome);
        }

        submissionFieldResultRepository.saveAll(fieldResultsToSave);
        submissionMethodResultRepository.saveAll(methodResultsToSave);
        submissionConstructorResultRepository.saveAll(constructorResultsToSave);
        submissionChallengeResultRepository.saveAll(challengeResultsToSave);

        BigDecimal overallScore = challengePercentages.isEmpty()
                ? BigDecimal.ZERO
                : average(challengePercentages);

        System.out.println("Overall score (simple average across " + challengePercentages.size()
                + " challenge(s)): " + overallScore + " / 100");
        System.out.println("=========================================");

        return overallScore;
    }

    private GradingOutcome gradeChallenge(
            LabSubmission submission, Challenge challenge, Path classesDir,
            Map<UUID, SubmissionFieldResult> existingFieldResults,
            Map<UUID, SubmissionMethodResult> existingMethodResults,
            Map<UUID, SubmissionConstructorResult> existingConstructorResults,
            Map<UUID, SubmissionChallengeResult> existingChallengeResults,
            List<SubmissionFieldResult> fieldResultsToSave,
            List<SubmissionMethodResult> methodResultsToSave,
            List<SubmissionConstructorResult> constructorResultsToSave,
            List<SubmissionChallengeResult> challengeResultsToSave) {

        List<ParsedClass> parsedClasses = Files.exists(classesDir)
                ? reflectionClassParser.parseClasses(classesDir)
                : List.of();
        Map<String, ParsedClass> parsedByName = parsedClasses.stream()
                .collect(Collectors.toMap(pc -> pc.simpleName, pc -> pc, (a, b) -> a));

        List<ClassEntity> expectedClasses = classEntityRepository.findByChallengeWithAttributes(challenge);

        List<Field> allFields = expectedClasses.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(expectedClasses);
        List<Method> allMethods = expectedClasses.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(expectedClasses);
        List<Constructor> allConstructors = expectedClasses.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(expectedClasses);

        List<Parameter> allMethodParams = allMethods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(allMethods);
        List<Parameter> allConstructorParams = allConstructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);

        Map<UUID, List<Field>> fieldsByClass = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));

        Map<UUID, List<String>> paramTypesByMethod = groupParamTypesByMethod(allMethodParams);
        Map<UUID, List<String>> paramTypesByConstructor = groupParamTypesByConstructor(allConstructorParams);

        int totalElements = 0;
        int correctElements = 0;
        boolean challengeFullyCorrect = true;
        List<ClassGradeReport> classReports = new ArrayList<>();

        for (ClassEntity expectedClass : expectedClasses) {
            ClassGradeReport report = new ClassGradeReport();
            report.className = expectedClass.getName();

            ParsedClass parsed = parsedByName.get(expectedClass.getName());
            report.matched = parsed != null;
            totalElements++;

            List<Field> expectedFields = fieldsByClass.getOrDefault(expectedClass.getId(), List.of());
            List<Method> expectedMethods = methodsByClass.getOrDefault(expectedClass.getId(), List.of());
            List<Constructor> expectedConstructors = constructorsByClass.getOrDefault(expectedClass.getId(), List.of());

            if (parsed == null) {
                challengeFullyCorrect = false;
                for (Field f : expectedFields) {
                    totalElements++;
                    report.missingFields.add(f.getName());
                    fieldResultsToSave.add(buildFieldResult(existingFieldResults, submission, f, false));
                }
                for (Method m : expectedMethods) {
                    totalElements++;
                    report.missingMethods.add(signatureLabel(m.getName(), paramTypesByMethod.getOrDefault(m.getId(), List.of())));
                    methodResultsToSave.add(buildMethodResult(existingMethodResults, submission, m, false));
                }
                for (Constructor c : expectedConstructors) {
                    totalElements++;
                    report.missingConstructors.add(signatureLabel(expectedClass.getName(), paramTypesByConstructor.getOrDefault(c.getId(), List.of())));
                    constructorResultsToSave.add(buildConstructorResult(existingConstructorResults, submission, c, false));
                }
                classReports.add(report);
                continue;
            }

            String expectedClassHash = hash(
                    expectedClass.getScope().getName(),
                    expectedClass.getDeclaringType().getName(),
                    String.valueOf(expectedClass.isAbstract()));
            String actualClassHash = hash(parsed.scope, parsed.declaringType, String.valueOf(parsed.isAbstract));
            report.classAttributesCorrect = expectedClassHash.equals(actualClassHash);
            if (report.classAttributesCorrect) {
                correctElements++;
            } else {
                challengeFullyCorrect = false;
            }

            Map<String, ParsedField> parsedFieldsByName = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name, f -> f, (a, b) -> a));
            for (Field expectedField : expectedFields) {
                totalElements++;
                ParsedField pf = parsedFieldsByName.get(expectedField.getName());
                boolean correct = false;
                if (pf != null) {
                    FieldDeclaration fd = expectedField.getFieldDeclaration();
                    correct = hash(fd.getScope().getName(), fd.getDataType()).equals(hash(pf.scope, pf.dataType));
                }
                if (correct) {
                    correctElements++;
                } else {
                    challengeFullyCorrect = false;
                    (pf == null ? report.missingFields : report.incorrectFields).add(expectedField.getName());
                }
                fieldResultsToSave.add(buildFieldResult(existingFieldResults, submission, expectedField, correct));
            }

            for (Method expectedMethod : expectedMethods) {
                totalElements++;
                List<String> expectedParamTypes = paramTypesByMethod.getOrDefault(expectedMethod.getId(), List.of());
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.getName(), expectedParamTypes);
                boolean correct = false;
                if (match != null) {
                    MethodDeclaration md = expectedMethod.getMethodDeclaration();
                    String expectedHash = hash(md.getScope().getName(), md.getReturnType(),
                            String.valueOf(md.isStatic()), String.valueOf(md.isAbstract()), String.valueOf(md.isFinal()));
                    String actualHash = hash(match.scope, match.returnType,
                            String.valueOf(match.isStatic), String.valueOf(match.isAbstract), String.valueOf(match.isFinal));
                    correct = expectedHash.equals(actualHash);
                }
                if (correct) {
                    correctElements++;
                } else {
                    challengeFullyCorrect = false;
                    (match == null ? report.missingMethods : report.incorrectMethods)
                            .add(signatureLabel(expectedMethod.getName(), expectedParamTypes));
                }
                methodResultsToSave.add(buildMethodResult(existingMethodResults, submission, expectedMethod, correct));
            }

            for (Constructor expectedConstructor : expectedConstructors) {
                totalElements++;
                List<String> expectedParamTypes = paramTypesByConstructor.getOrDefault(expectedConstructor.getId(), List.of());
                ParsedConstructor match = findMatchingConstructor(parsed.constructors, expectedParamTypes);
                boolean correct = false;
                if (match != null) {
                    ConstructorDeclaration cd = expectedConstructor.getConstructorDeclaration();
                    correct = hash(cd.getScope().getName(), String.valueOf(cd.isDefault()))
                            .equals(hash(match.scope, String.valueOf(match.parameterTypes.isEmpty())));
                }
                if (correct) {
                    correctElements++;
                } else {
                    challengeFullyCorrect = false;
                    (match == null ? report.missingConstructors : report.incorrectConstructors)
                            .add(signatureLabel(expectedClass.getName(), expectedParamTypes));
                }
                constructorResultsToSave.add(buildConstructorResult(existingConstructorResults, submission, expectedConstructor, correct));
            }

            classReports.add(report);
        }

        challengeResultsToSave.add(buildChallengeResult(existingChallengeResults, submission, challenge, challengeFullyCorrect));

        BigDecimal percentage = totalElements == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctElements)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalElements), 2, RoundingMode.HALF_UP);

        GradingOutcome outcome = new GradingOutcome();
        outcome.percentage = percentage;
        outcome.fullyCorrect = challengeFullyCorrect;
        outcome.classReports = classReports;
        return outcome;
    }

    private Map<UUID, List<String>> groupParamTypesByMethod(List<Parameter> params) {
        Map<UUID, List<Parameter>> grouped = params.stream()
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        return grouped.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                        .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                        .map(Parameter::getDataType)
                        .collect(Collectors.toList())));
    }

    private Map<UUID, List<String>> groupParamTypesByConstructor(List<Parameter> params) {
        Map<UUID, List<Parameter>> grouped = params.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));
        return grouped.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                        .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                        .map(Parameter::getDataType)
                        .collect(Collectors.toList())));
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
            if (!a.get(i).trim().equalsIgnoreCase(b.get(i).trim())) return false;
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

    private String hash(String... parts) {
        String combined = String.join("|", parts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private SubmissionFieldResult buildFieldResult(Map<UUID, SubmissionFieldResult> existing,
                                                     LabSubmission submission, Field field, boolean correct) {
        SubmissionFieldResult result = existing.getOrDefault(field.getId(), new SubmissionFieldResult());
        result.setSubmission(submission);
        result.setField(field);
        result.setCorrect(correct);
        return result;
    }

    private SubmissionMethodResult buildMethodResult(Map<UUID, SubmissionMethodResult> existing,
                                                       LabSubmission submission, Method method, boolean correct) {
        SubmissionMethodResult result = existing.getOrDefault(method.getId(), new SubmissionMethodResult());
        result.setSubmission(submission);
        result.setMethod(method);
        result.setCorrect(correct);
        return result;
    }

    private SubmissionConstructorResult buildConstructorResult(Map<UUID, SubmissionConstructorResult> existing,
                                                                 LabSubmission submission, Constructor constructorEntity, boolean correct) {
        SubmissionConstructorResult result = existing.getOrDefault(constructorEntity.getId(), new SubmissionConstructorResult());
        result.setSubmission(submission);
        result.setConstructor(constructorEntity);
        result.setCorrect(correct);
        return result;
    }

    private SubmissionChallengeResult buildChallengeResult(Map<UUID, SubmissionChallengeResult> existing,
                                                             LabSubmission submission, Challenge challenge, boolean correct) {
        SubmissionChallengeResult result = existing.getOrDefault(challenge.getId(), new SubmissionChallengeResult());
        result.setSubmission(submission);
        result.setChallenge(challenge);
        result.setCorrect(correct);
        return result;
    }

    private void printChallengeReport(Challenge challenge, GradingOutcome outcome) {
        System.out.println("--- Challenge #" + challenge.getChallengeNumber() + ": " + challenge.getName() + " ---");
        System.out.println("  Score: " + outcome.percentage + "% | Fully correct: " + outcome.fullyCorrect);
        for (ClassGradeReport cr : outcome.classReports) {
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

    public static class GradingOutcome {
        public BigDecimal percentage;
        public boolean fullyCorrect;
        public List<ClassGradeReport> classReports;
    }

    public static class ClassGradeReport {
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