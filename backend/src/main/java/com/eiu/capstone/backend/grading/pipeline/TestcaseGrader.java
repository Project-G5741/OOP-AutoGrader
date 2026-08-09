package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.ParsedField;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;
import com.eiu.capstone.backend.model.TestcaseCheckType;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseTargetType;

@Component
public class TestcaseGrader {

    public TestcasePillarResult grade(ChallengeGradingContext context) {
        List<WeightedAccuracy> weighted = new ArrayList<>();
        List<PendingTestcaseResult> results = new ArrayList<>();
        ChallengeRubric rubric = context.challengeRubric();

        for (TestcaseRubric testcase : rubric.testcases()) {
            int weight = MemberWeightCalculator.testcaseWeight(testcase.weight());
            Evaluation evaluation = evaluate(testcase, rubric, context);
            weighted.add(new WeightedAccuracy(weight, evaluation.accuracy()));
            results.add(new PendingTestcaseResult(
                    testcase.id(),
                    evaluation.status(),
                    evaluation.feedback()));
        }

        BigDecimal pillarPct = rubric.testcases().isEmpty()
                ? BigDecimal.ZERO
                : PillarScoreAggregator.pillarPercentage(weighted);
        return new TestcasePillarResult(pillarPct, results);
    }

    private Evaluation evaluate(TestcaseRubric testcase,
                                ChallengeRubric rubric,
                                ChallengeGradingContext context) {
        if (context.compileError() != null && !context.compileError().isBlank()) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Compilation error: " + context.compileError());
        }

        return switch (testcase.targetType()) {
            case CLASS -> evaluateClass(testcase, rubric, context);
            case FIELD -> evaluateField(testcase, rubric, context);
            case METHOD -> evaluateMethod(testcase, rubric, context);
            case CONSTRUCTOR -> evaluateConstructor(testcase, rubric, context);
        };
    }

    private Evaluation evaluateClass(TestcaseRubric testcase, ChallengeRubric rubric, ChallengeGradingContext context) {
        Optional<ClassRubric> expected = rubric.classes().stream()
                .filter(c -> c.id().equals(testcase.targetId()))
                .findFirst();
        if (expected.isEmpty()) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Rubric class not found for testcase");
        }
        ClassRubric classRubric = expected.get();
        ParsedClass parsed = context.parsedByName().get(classRubric.name());
        if (parsed == null) {
            return new Evaluation(0, TestcaseResultStatus.FAILED, "Class not found: " + classRubric.name());
        }
        if (testcase.checkType() == TestcaseCheckType.EXISTENCE) {
            return new Evaluation(1, TestcaseResultStatus.PASSED, "Class exists");
        }
        double accuracy = PartialCreditEvaluator.accuracy(List.of(
                PartialCreditEvaluator.matches(classRubric.scope(), parsed.scope).get(0),
                PartialCreditEvaluator.matches(classRubric.declaringType(), parsed.declaringType).get(0),
                classRubric.isAbstract() == parsed.isAbstract));
        return toEvaluation(accuracy, "Class declaration");
    }

    private Evaluation evaluateField(TestcaseRubric testcase, ChallengeRubric rubric, ChallengeGradingContext context) {
        FieldTarget target = resolveField(testcase.targetId(), rubric);
        if (target == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Rubric field not found for testcase");
        }
        ParsedClass parsed = context.parsedByName().get(target.classRubric().name());
        if (parsed == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Class failed to compile or is missing");
        }
        ParsedField pf = parsed.fields.stream().filter(f -> f.name.equals(target.field().name())).findFirst().orElse(null);
        if (pf == null) {
            return new Evaluation(0, TestcaseResultStatus.FAILED, "Field not found: " + target.field().name());
        }
        if (testcase.checkType() == TestcaseCheckType.EXISTENCE) {
            return new Evaluation(1, TestcaseResultStatus.PASSED, "Field exists");
        }
        double accuracy = PartialCreditEvaluator.accuracy(List.of(
                PartialCreditEvaluator.matches(target.field().scope(), pf.scope).get(0),
                PartialCreditEvaluator.matches(target.field().dataType(), pf.dataType).get(0)));
        return toEvaluation(accuracy, "Field declaration");
    }

    private Evaluation evaluateMethod(TestcaseRubric testcase, ChallengeRubric rubric, ChallengeGradingContext context) {
        MethodTarget target = resolveMethod(testcase.targetId(), rubric);
        if (target == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Rubric method not found for testcase");
        }
        ParsedClass parsed = context.parsedByName().get(target.classRubric().name());
        if (parsed == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Class failed to compile or is missing");
        }
        ParsedMethod match = findMatchingMethod(parsed.methods, target.method().name(), target.method().parameterTypes());
        if (match == null) {
            return new Evaluation(0, TestcaseResultStatus.FAILED, "Method not found: " + target.method().name());
        }
        if (testcase.checkType() == TestcaseCheckType.EXISTENCE) {
            return new Evaluation(1, TestcaseResultStatus.PASSED, "Method exists");
        }
        double accuracy = PartialCreditEvaluator.accuracy(List.of(
                PartialCreditEvaluator.matches(target.method().scope(), match.scope).get(0),
                PartialCreditEvaluator.matches(target.method().returnType(), match.returnType).get(0),
                target.method().isStatic() == match.isStatic,
                target.method().isAbstract() == match.isAbstract,
                target.method().isFinal() == match.isFinal));
        return toEvaluation(accuracy, "Method declaration");
    }

    private Evaluation evaluateConstructor(TestcaseRubric testcase, ChallengeRubric rubric, ChallengeGradingContext context) {
        ConstructorTarget target = resolveConstructor(testcase.targetId(), rubric);
        if (target == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Rubric constructor not found for testcase");
        }
        ParsedClass parsed = context.parsedByName().get(target.classRubric().name());
        if (parsed == null) {
            return new Evaluation(0, TestcaseResultStatus.ERROR, "Class failed to compile or is missing");
        }
        ParsedConstructor match = findMatchingConstructor(parsed.constructors, target.constructor().parameterTypes());
        if (match == null) {
            return new Evaluation(0, TestcaseResultStatus.FAILED, "Constructor not found");
        }
        if (testcase.checkType() == TestcaseCheckType.EXISTENCE) {
            return new Evaluation(1, TestcaseResultStatus.PASSED, "Constructor exists");
        }
        boolean actualDefault = match.parameterTypes.isEmpty();
        double accuracy = PartialCreditEvaluator.accuracy(List.of(
                PartialCreditEvaluator.matches(target.constructor().scope(), match.scope).get(0),
                target.constructor().isDefault() == actualDefault));
        return toEvaluation(accuracy, "Constructor declaration");
    }

    private Evaluation toEvaluation(double accuracy, String label) {
        if (accuracy >= 1.0) {
            return new Evaluation(1, TestcaseResultStatus.PASSED, label + " matches");
        }
        if (accuracy <= 0) {
            return new Evaluation(0, TestcaseResultStatus.FAILED, label + " mismatch");
        }
        return new Evaluation(accuracy, TestcaseResultStatus.FAILED,
                label + " partial match (" + Math.round(accuracy * 100) + "%)");
    }

    private ParsedMethod findMatchingMethod(List<ParsedMethod> candidates, String name, List<String> paramTypes) {
        for (ParsedMethod pm : candidates) {
            if (pm.name.equals(name) && sameTypes(pm.parameterTypes, paramTypes)) {
                return pm;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates, List<String> paramTypes) {
        for (ParsedConstructor pc : candidates) {
            if (sameTypes(pc.parameterTypes, paramTypes)) {
                return pc;
            }
        }
        return null;
    }

    private boolean sameTypes(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equalsIgnoreCase(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private FieldTarget resolveField(UUID fieldId, ChallengeRubric rubric) {
        for (ClassRubric classRubric : rubric.classes()) {
            for (FieldRubric field : classRubric.fields()) {
                if (field.id().equals(fieldId)) {
                    return new FieldTarget(classRubric, field);
                }
            }
        }
        return null;
    }

    private MethodTarget resolveMethod(UUID methodId, ChallengeRubric rubric) {
        for (ClassRubric classRubric : rubric.classes()) {
            for (MethodRubric method : classRubric.methods()) {
                if (method.id().equals(methodId)) {
                    return new MethodTarget(classRubric, method);
                }
            }
        }
        return null;
    }

    private ConstructorTarget resolveConstructor(UUID constructorId, ChallengeRubric rubric) {
        for (ClassRubric classRubric : rubric.classes()) {
            for (ConstructorRubric constructor : classRubric.constructors()) {
                if (constructor.id().equals(constructorId)) {
                    return new ConstructorTarget(classRubric, constructor);
                }
            }
        }
        return null;
    }

    private record Evaluation(double accuracy, TestcaseResultStatus status, String feedback) {}
    private record FieldTarget(ClassRubric classRubric, FieldRubric field) {}
    private record MethodTarget(ClassRubric classRubric, MethodRubric method) {}
    private record ConstructorTarget(ClassRubric classRubric, ConstructorRubric constructor) {}

    public record TestcasePillarResult(BigDecimal pillarPercentage, List<PendingTestcaseResult> results) {}

    public record PendingTestcaseResult(UUID testcaseId, TestcaseResultStatus status, String feedback) {}
}
