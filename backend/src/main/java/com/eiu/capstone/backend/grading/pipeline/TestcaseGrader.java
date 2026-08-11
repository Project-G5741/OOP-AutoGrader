package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
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
            comparisonOutcome = invocationRunner.invokeComparison(
                    context.classesDir(), testcase.comparisonMethod(), testcase.instances());
            if (comparisonOutcome.kind() == InvocationOutcomeKind.ERROR) {
                return infrastructureError(testcase, comparisonOutcome.errorMessage());
            }
        } else if (testcase.invocation() == null) {
            return infrastructureError(testcase, "Missing invocation rubric");
        } else {
            invocationOutcome = invocationRunner.invokeSingle(context.classesDir(), testcase.invocation());
            if (invocationOutcome.kind() == InvocationOutcomeKind.TIMED_OUT
                    || invocationOutcome.kind() == InvocationOutcomeKind.ERROR) {
                return infrastructureError(testcase,
                        invocationOutcome.errorMessage() != null
                                ? invocationOutcome.errorMessage()
                                : "Invocation failed");
            }
        }

        if (testcase.assertions().isEmpty()) {
            return infrastructureError(testcase, "No assertions configured");
        }

        Map<UUID, AssertionEvaluation> evaluations = new HashMap<>();
        for (AssertionRubric assertion : testcase.assertions()) {
            AssertionEvaluation evaluation = assertionEvaluator.evaluate(
                    assertion, invocationOutcome, comparisonOutcome);
            evaluations.put(assertion.id(), evaluation);
        }

        boolean allPassed = !evaluations.isEmpty() && evaluations.values().stream()
                .allMatch(e -> e.status() == TestcaseResultStatus.PASSED);

        TestcaseResultStatus status;
        double accuracy;
        String feedback;
        if (allPassed) {
            status = TestcaseResultStatus.PASSED;
            accuracy = 1;
            feedback = "All assertions passed";
        } else {
            status = TestcaseResultStatus.FAILED;
            accuracy = 0;
            feedback = firstFailureFeedback(testcase.assertions(), evaluations);
        }

        PrimaryDisplays displays = primaryDisplays(
                testcase, evaluations, invocationOutcome, comparisonOutcome, null);

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
                displays.input(),
                displays.expected(),
                displays.actual(),
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
        PrimaryDisplays displays = primaryDisplays(testcase, Map.of(), null, null, message);
        return new Evaluation(0, new PendingTestcaseResult(
                testcase.id(),
                TestcaseResultStatus.ERROR,
                message,
                displays.input(),
                displays.expected(),
                displays.actual(),
                List.of()));
    }

    private PrimaryDisplays primaryDisplays(TestcaseRubric testcase,
                                            Map<UUID, AssertionEvaluation> evaluations,
                                            InvocationOutcome invocationOutcome,
                                            ComparisonOutcome comparisonOutcome,
                                            String fallbackActual) {
        AssertionRubric primary = primaryAssertionSelector.select(testcase.assertions());
        String inputDisplay = displayFormatter.formatInput(testcase);
        if (primary == null) {
            return new PrimaryDisplays(inputDisplay, null, fallbackActual);
        }
        AssertionEvaluation primaryEvaluation = evaluations.get(primary.id());
        String expectedDisplay = displayFormatter.formatExpected(primary);
        String actualDisplay = fallbackActual != null
                ? fallbackActual
                : displayFormatter.formatActual(
                        primary, primaryEvaluation, invocationOutcome, comparisonOutcome);
        return new PrimaryDisplays(inputDisplay, expectedDisplay, actualDisplay);
    }

    private String firstFailureFeedback(List<AssertionRubric> assertions,
                                        Map<UUID, AssertionEvaluation> evaluations) {
        for (AssertionRubric assertion : assertions) {
            AssertionEvaluation evaluation = evaluations.get(assertion.id());
            if (evaluation != null && evaluation.status() == TestcaseResultStatus.FAILED) {
                return evaluation.feedback();
            }
        }
        return "Assertion failed";
    }

    private record PrimaryDisplays(String input, String expected, String actual) {}

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
