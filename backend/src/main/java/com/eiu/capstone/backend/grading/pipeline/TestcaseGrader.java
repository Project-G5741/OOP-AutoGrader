package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluation;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluator;
import com.eiu.capstone.backend.grading.testcase.ComparisonOutcome;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcomeKind;
import com.eiu.capstone.backend.grading.testcase.InvocationRunner;
import com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

@Component
public class TestcaseGrader {

    private final InvocationRunner invocationRunner;
    private final AssertionEvaluator assertionEvaluator;
    private final PrimaryAssertionSelector primaryAssertionSelector;
    private final TestcaseDisplayFormatter displayFormatter;

    public TestcaseGrader(InvocationRunner invocationRunner,
                          AssertionEvaluator assertionEvaluator,
                          PrimaryAssertionSelector primaryAssertionSelector,
                          TestcaseDisplayFormatter displayFormatter) {
        this.invocationRunner = invocationRunner;
        this.assertionEvaluator = assertionEvaluator;
        this.primaryAssertionSelector = primaryAssertionSelector;
        this.displayFormatter = displayFormatter;
    }

    public TestcasePillarResult grade(ChallengeGradingContext context) {
        List<WeightedAccuracy> weighted = new ArrayList<>();
        List<PendingTestcaseResult> results = new ArrayList<>();
        ChallengeRubric rubric = context.challengeRubric();

        for (TestcaseRubric testcase : rubric.testcases()) {
            int weight = MemberWeightCalculator.testcaseWeight(testcase.weight());
            Evaluation evaluation = evaluate(testcase, context);
            weighted.add(new WeightedAccuracy(weight, evaluation.accuracy()));
            results.add(evaluation.pending());
        }

        BigDecimal pillarPct = rubric.testcases().isEmpty()
                ? BigDecimal.ZERO
                : PillarScoreAggregator.pillarPercentage(weighted);
        return new TestcasePillarResult(pillarPct, results);
    }

    private Evaluation evaluate(TestcaseRubric testcase, ChallengeGradingContext context) {
        if (context.compileError() != null && !context.compileError().isBlank()) {
            return compileErrorEvaluation(testcase, context.compileError());
        }

        InvocationOutcome invocationOutcome = null;
        ComparisonOutcome comparisonOutcome = null;

        if (testcase.testcaseType() == TestcaseType.COMPARISON) {
            List<InstanceRubric> instances = testcase.instances().stream()
                    .sorted(Comparator.comparing(InstanceRubric::label))
                    .toList();
            comparisonOutcome = invocationRunner.invokeComparison(
                    context.classesDir(), testcase.comparisonMethod(), instances);
            if (comparisonOutcome.kind() == InvocationOutcomeKind.ERROR) {
                return infrastructureError(testcase, comparisonOutcome.errorMessage());
            }
        } else {
            if (testcase.invocation() == null) {
                return infrastructureError(testcase, "Missing invocation rubric");
            }
            invocationOutcome = invocationRunner.invokeSingle(context.classesDir(), testcase.invocation());
            if (invocationOutcome.kind() == InvocationOutcomeKind.TIMED_OUT
                    || invocationOutcome.kind() == InvocationOutcomeKind.ERROR) {
                return infrastructureError(testcase,
                        invocationOutcome.errorMessage() != null
                                ? invocationOutcome.errorMessage()
                                : "Invocation failed");
            }
        }

        Map<UUID, AssertionEvaluation> evaluations = new HashMap<>();
        for (AssertionRubric assertion : testcase.assertions()) {
            AssertionEvaluation evaluation = assertionEvaluator.evaluate(
                    assertion, invocationOutcome, comparisonOutcome);
            evaluations.put(assertion.id(), evaluation);
        }

        boolean anyError = evaluations.values().stream()
                .anyMatch(e -> e.status() == TestcaseResultStatus.ERROR);
        boolean allPassed = !evaluations.isEmpty() && evaluations.values().stream()
                .allMatch(e -> e.status() == TestcaseResultStatus.PASSED);

        TestcaseResultStatus status;
        double accuracy;
        String feedback;
        if (anyError) {
            status = TestcaseResultStatus.ERROR;
            accuracy = 0;
            feedback = "Testcase error";
        } else if (allPassed) {
            status = TestcaseResultStatus.PASSED;
            accuracy = 1;
            feedback = "All assertions passed";
        } else {
            status = TestcaseResultStatus.FAILED;
            accuracy = 0;
            feedback = firstFailureFeedback(evaluations);
        }

        AssertionRubric primary = primaryAssertionSelector.select(testcase.assertions());
        AssertionEvaluation primaryEvaluation = primary != null ? evaluations.get(primary.id()) : null;

        String inputDisplay = displayFormatter.formatInput(testcase);
        String expectedDisplay = primary != null ? displayFormatter.formatExpected(primary) : null;
        String actualDisplay = primary != null
                ? displayFormatter.formatActual(primary, primaryEvaluation, invocationOutcome, comparisonOutcome)
                : null;

        List<PendingAssertionResult> assertionResults = testcase.assertions().stream()
                .map(assertion -> {
                    AssertionEvaluation evaluation = evaluations.get(assertion.id());
                    return new PendingAssertionResult(
                            assertion.id(),
                            evaluation.status(),
                            evaluation.actualValueJson(),
                            evaluation.feedback());
                })
                .toList();

        return new Evaluation(accuracy, new PendingTestcaseResult(
                testcase.id(),
                status,
                feedback,
                inputDisplay,
                expectedDisplay,
                actualDisplay,
                assertionResults));
    }

    private Evaluation compileErrorEvaluation(TestcaseRubric testcase, String compileError) {
        return new Evaluation(0, new PendingTestcaseResult(
                testcase.id(),
                TestcaseResultStatus.ERROR,
                "Compilation error: " + compileError,
                null,
                null,
                null,
                List.of()));
    }

    private Evaluation infrastructureError(TestcaseRubric testcase, String message) {
        AssertionRubric primary = primaryAssertionSelector.select(testcase.assertions());
        String inputDisplay = displayFormatter.formatInput(testcase);
        String expectedDisplay = primary != null ? displayFormatter.formatExpected(primary) : null;
        return new Evaluation(0, new PendingTestcaseResult(
                testcase.id(),
                TestcaseResultStatus.ERROR,
                message,
                inputDisplay,
                expectedDisplay,
                message,
                List.of()));
    }

    private String firstFailureFeedback(Map<UUID, AssertionEvaluation> evaluations) {
        return evaluations.values().stream()
                .filter(e -> e.status() == TestcaseResultStatus.FAILED)
                .map(AssertionEvaluation::feedback)
                .findFirst()
                .orElse("Assertion failed");
    }

    private record Evaluation(double accuracy, PendingTestcaseResult pending) {}

    public record TestcasePillarResult(BigDecimal pillarPercentage, List<PendingTestcaseResult> results) {}

    public record PendingTestcaseResult(
            UUID testcaseId,
            TestcaseResultStatus status,
            String feedback,
            String inputDisplay,
            String expectedDisplay,
            String actualDisplay,
            List<PendingAssertionResult> assertions) {}

    public record PendingAssertionResult(
            UUID assertionId,
            TestcaseResultStatus status,
            String actualValueJson,
            String feedback) {}
}
