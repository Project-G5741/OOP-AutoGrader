package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluation;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluator;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcomeKind;
import com.eiu.capstone.backend.grading.testcase.InvocationRunner;
import com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.service.compile.CompileErrorMessage;
import com.eiu.capstone.backend.utility.TimingLog;

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

    public PendingTestcaseResult gradeSingle(TestcaseRubric testcase, ChallengeGradingContext context) {
        long started = System.currentTimeMillis();
        try {
            return evaluate(testcase, context).pending();
        } finally {
            // TEMP: remove after OT warm-path timing check
            TimingLog.line(true, "OT single " + testcaseLabel(testcase), System.currentTimeMillis() - started);
        }
    }

    public TestcasePillarResult grade(ChallengeGradingContext context) {
        List<WeightedAccuracy> weighted = new ArrayList<>();
        List<PendingTestcaseResult> results = new ArrayList<>();
        ChallengeRubric rubric = context.challengeRubric();

        List<TestcaseRubric> runnable = new ArrayList<>();
        List<Evaluation> earlyByIndex = new ArrayList<>();
        for (TestcaseRubric testcase : rubric.testcases()) {
            Evaluation early = earlyShortCircuit(testcase, context);
            earlyByIndex.add(early);
            if (early == null) {
                runnable.add(testcase);
            }
        }

        List<List<InvocationOutcome>> batchOutcomes = List.of();
        if (!runnable.isEmpty()) {
            long invokeStarted = System.currentTimeMillis();
            batchOutcomes = invocationRunner.invokeBatch(
                    context, runnable.stream().map(this::toBatchItem).toList());
            // TEMP: remove after OT warm-path timing check
            if (runnable.size() == 1) {
                TimingLog.line(true, "OT single invoke " + testcaseLabel(runnable.get(0)),
                        System.currentTimeMillis() - invokeStarted);
            }
        }

        int runnableIndex = 0;
        for (int i = 0; i < rubric.testcases().size(); i++) {
            TestcaseRubric testcase = rubric.testcases().get(i);
            int weight = MemberWeightCalculator.testcaseWeight(testcase.weight());
            Evaluation evaluation = earlyByIndex.get(i);
            if (evaluation == null) {
                List<InvocationOutcome> outcomes = runnableIndex < batchOutcomes.size()
                        ? batchOutcomes.get(runnableIndex)
                        : List.of();
                runnableIndex++;
                evaluation = finishEvaluate(testcase, outcomes);
            }
            weighted.add(new WeightedAccuracy(weight, evaluation.accuracy()));
            results.add(evaluation.pending());
        }

        BigDecimal pillarPct = rubric.testcases().isEmpty()
                ? BigDecimal.ZERO
                : PillarScoreAggregator.pillarPercentage(weighted);
        return new TestcasePillarResult(pillarPct, results);
    }

    private Evaluation evaluate(TestcaseRubric testcase, ChallengeGradingContext context) {
        Evaluation early = earlyShortCircuit(testcase, context);
        if (early != null) {
            return early;
        }
        List<List<InvocationOutcome>> batch = invocationRunner.invokeBatch(context, List.of(toBatchItem(testcase)));
        List<InvocationOutcome> outcomes = batch.isEmpty() ? List.of() : batch.get(0);
        return finishEvaluate(testcase, outcomes);
    }

    private static String testcaseLabel(TestcaseRubric testcase) {
        if (testcase == null) {
            return "?";
        }
        if (testcase.name() != null && !testcase.name().isBlank()) {
            return testcase.name();
        }
        return testcase.id() != null ? testcase.id().toString() : "?";
    }

    private Evaluation earlyShortCircuit(TestcaseRubric testcase, ChallengeGradingContext context) {
        if (context.compileError() != null && !context.compileError().isBlank()) {
            return compileErrorEvaluation(testcase, context.compileError());
        }
        String failedType = firstFailedInvokedType(testcase, context.failedClassNames());
        if (failedType != null) {
            String message = CompileErrorMessage.summarize(context.compileErrorsByClassName().getOrDefault(
                    failedType, CompileErrorMessage.see(failedType)));
            return compileErrorEvaluation(testcase, message);
        }
        List<InvocationRubric> steps = prepareSteps(testcase);
        if (steps.isEmpty()) {
            return infrastructureError(testcase, "Missing invocation rubric", null);
        }
        return null;
    }

    private InvocationRunner.BatchItem toBatchItem(TestcaseRubric testcase) {
        List<InvocationRubric> steps = prepareSteps(testcase);
        List<String> snapshotFields = testcase.assertions().stream()
                .filter(assertion -> assertion.kind() == AssertionKind.FIELD_STATE)
                .map(AssertionRubric::fieldName)
                .toList();
        return new InvocationRunner.BatchItem(steps, snapshotFields);
    }

    private Evaluation finishEvaluate(TestcaseRubric testcase, List<InvocationOutcome> outcomes) {
        List<InvocationRubric> steps = prepareSteps(testcase);
        if (steps.isEmpty()) {
            return infrastructureError(testcase, "Missing invocation rubric", null);
        }
        if (outcomes == null || outcomes.isEmpty()) {
            return infrastructureError(testcase, "Invocation failed", steps.get(0));
        }
        for (int i = 0; i < outcomes.size(); i++) {
            InvocationOutcome outcome = outcomes.get(i);
            if (outcome != null && (outcome.kind() == InvocationOutcomeKind.TIMED_OUT
                    || outcome.kind() == InvocationOutcomeKind.ERROR)) {
                InvocationRubric step = i < steps.size() ? steps.get(i) : steps.get(0);
                return infrastructureError(testcase,
                        outcome.errorMessage() != null ? outcome.errorMessage() : "Invocation failed",
                        step);
            }
        }

        if (testcase.assertions().isEmpty()) {
            return infrastructureError(testcase, "No assertions configured",
                    steps.isEmpty() ? null : steps.get(0));
        }

        Map<UUID, InvocationOutcome> outcomeByStepId = zipOutcomes(steps, outcomes);
        int stopAfter = firstThrowIndex(outcomes);
        Map<UUID, AssertionEvaluation> evaluations = new HashMap<>();
        for (AssertionRubric assertion : testcase.assertions()) {
            int stepIndex = boundStepIndex(assertion, steps);
            if (stepIndex > stopAfter) {
                evaluations.put(assertion.id(), assertionEvaluator.evaluate(assertion, null, null));
                continue;
            }
            InvocationRubric boundStep = stepIndex >= 0 && stepIndex < steps.size() ? steps.get(stepIndex) : null;
            if (assertion.kind() == AssertionKind.STDOUT
                    && boundStep != null
                    && boundStep.kind() == InvocationKind.CONSTRUCTOR) {
                evaluations.put(assertion.id(), ignoredConstructorStdout(assertion));
                continue;
            }
            InvocationOutcome bound = boundOutcome(assertion, steps, outcomeByStepId);
            AssertionEvaluation evaluation = assertionEvaluator.evaluate(
                    assertion, bound, null);
            evaluations.put(assertion.id(), evaluation);
        }

        boolean unassertedThrow = hasUnassertedThrow(steps, outcomes, testcase.assertions(), evaluations);
        boolean allPassed = !evaluations.isEmpty()
                && evaluations.values().stream().allMatch(e -> e.status() == TestcaseResultStatus.PASSED)
                && !unassertedThrow;

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
            feedback = firstFailureFeedback(testcase.assertions(), evaluations, outcomes);
        }

        PrimaryDisplays displays = primaryDisplays(testcase, evaluations, steps, outcomes, null);

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

    public static List<InvocationRubric> resolveSteps(TestcaseRubric testcase) {
        if (testcase.invocations() != null && !testcase.invocations().isEmpty()) {
            return testcase.invocations();
        }
        if (testcase.invocation() != null) {
            return List.of(testcase.invocation());
        }
        return List.of();
    }

    private static List<InvocationRubric> prepareSteps(TestcaseRubric testcase) {
        List<InvocationRubric> steps = resolveSteps(testcase);
        if (testcase.testcaseType() != TestcaseType.UNIT) {
            return steps;
        }
        List<InvocationRubric> prepared = new ArrayList<>();
        for (InvocationRubric step : steps) {
            prepared.add(injectUnitHiddenReceiver(step));
        }
        return prepared;
    }

    private static InvocationRubric injectUnitHiddenReceiver(InvocationRubric step) {
        if (step == null || step.kind() != InvocationKind.METHOD) {
            return step;
        }
        if (step.instanceName() != null && !step.instanceName().isBlank()) {
            return step;
        }
        if (step.receiverClassName() != null && !step.receiverClassName().isBlank()) {
            return step;
        }
        return new InvocationRubric(
                step.id(),
                step.kind(),
                step.constructorId(),
                step.methodId(),
                step.className(),
                step.methodName(),
                step.parameterTypes(),
                step.paramsJson(),
                step.receiverConstructorId(),
                step.className(),
                List.of(),
                "[]",
                step.instanceName(),
                step.dispatchClassId(),
                step.dispatchClassName(),
                step.resultTypeName());
    }

    private static int firstThrowIndex(List<InvocationOutcome> outcomes) {
        if (outcomes == null) {
            return Integer.MAX_VALUE;
        }
        for (int i = 0; i < outcomes.size(); i++) {
            InvocationOutcome outcome = outcomes.get(i);
            if (outcome != null && outcome.kind() == InvocationOutcomeKind.THREW) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int boundStepIndex(AssertionRubric assertion, List<InvocationRubric> steps) {
        if (assertion.invocationId() != null) {
            for (int i = 0; i < steps.size(); i++) {
                if (assertion.invocationId().equals(steps.get(i).id())) {
                    return i;
                }
            }
            return Integer.MAX_VALUE;
        }
        return steps.size() == 1 ? 0 : Integer.MAX_VALUE;
    }

    private AssertionEvaluation ignoredConstructorStdout(AssertionRubric assertion) {
        return new AssertionEvaluation(
                assertion.id(),
                TestcaseResultStatus.PASSED,
                null,
                "Stdout is not evaluated on constructors");
    }

    private static String firstFailedInvokedType(TestcaseRubric testcase, Set<String> failedClassNames) {
        if (failedClassNames == null || failedClassNames.isEmpty()) {
            return null;
        }
        for (InvocationRubric invocation : resolveSteps(testcase)) {
            String hit = firstFailedName(failedClassNames,
                    invocation.className(), invocation.receiverClassName(), invocation.dispatchClassName());
            if (hit != null) {
                return hit;
            }
            hit = firstFailedTypeUse(failedClassNames, invocation.parameterTypes());
            if (hit != null) {
                return hit;
            }
            hit = firstFailedTypeUse(failedClassNames, invocation.receiverParameterTypes());
            if (hit != null) {
                return hit;
            }
        }
        if (testcase.instances() != null) {
            for (InstanceRubric instance : testcase.instances()) {
                String hit = firstFailedName(failedClassNames, instance.className());
                if (hit != null) {
                    return hit;
                }
                hit = firstFailedTypeUse(failedClassNames, instance.parameterTypes());
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static String firstFailedTypeUse(Set<String> failedClassNames, List<String> types) {
        if (types == null) {
            return null;
        }
        for (String type : types) {
            String hit = firstFailedName(failedClassNames, type);
            if (hit != null) {
                return hit;
            }
            if (type == null) {
                continue;
            }
            for (String failed : failedClassNames) {
                if (containsTypeToken(type, failed)) {
                    return failed;
                }
            }
        }
        return null;
    }

    private static String firstFailedName(Set<String> failedClassNames, String... names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            if (failedClassNames.contains(name)) {
                return name;
            }
            String simple = simpleTypeName(name);
            if (failedClassNames.contains(simple)) {
                return simple;
            }
        }
        return null;
    }

    private static String simpleTypeName(String type) {
        String trimmed = type.trim();
        int generic = trimmed.indexOf('<');
        if (generic >= 0) {
            trimmed = trimmed.substring(0, generic);
        }
        trimmed = trimmed.replace("[]", "").trim();
        int dot = trimmed.lastIndexOf('.');
        return dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
    }

    private static boolean containsTypeToken(String type, String failedName) {
        String simple = simpleTypeName(failedName);
        return type.equals(failedName)
                || type.equals(simple)
                || containsJavaIdentifier(type, failedName)
                || (!simple.equals(failedName) && containsJavaIdentifier(type, simple));
    }

    private static boolean containsJavaIdentifier(String haystack, String needle) {
        int from = 0;
        while (from <= haystack.length() - needle.length()) {
            int index = haystack.indexOf(needle, from);
            if (index < 0) {
                return false;
            }
            boolean startOk = index == 0 || !Character.isJavaIdentifierPart(haystack.charAt(index - 1));
            int end = index + needle.length();
            boolean endOk = end == haystack.length() || !Character.isJavaIdentifierPart(haystack.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            from = index + 1;
        }
        return false;
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

    private Evaluation infrastructureError(TestcaseRubric testcase, String message, InvocationRubric step) {
        String input = step != null
                ? displayFormatter.formatInput(testcase, step)
                : displayFormatter.formatInput(testcase);
        return new Evaluation(0, new PendingTestcaseResult(
                testcase.id(),
                TestcaseResultStatus.ERROR,
                message,
                input,
                null,
                message,
                List.of()));
    }

    private PrimaryDisplays primaryDisplays(TestcaseRubric testcase,
                                            Map<UUID, AssertionEvaluation> evaluations,
                                            List<InvocationRubric> steps,
                                            List<InvocationOutcome> outcomes,
                                            String fallbackActual) {
        if (steps == null || steps.isEmpty()) {
            AssertionRubric primary = primaryAssertionSelector.select(testcase.assertions());
            String inputDisplay = displayFormatter.formatInput(testcase);
            return displaysFor(primary, evaluations, null, inputDisplay, fallbackActual, null);
        }

        int primaryIndex = firstFailingOrLastRunIndex(steps, outcomes, evaluations, testcase.assertions());
        InvocationRubric primaryStep = steps.get(Math.min(Math.max(primaryIndex, 0), steps.size() - 1));
        boolean singleStep = steps.size() == 1;
        List<AssertionRubric> onStep = PrimaryAssertionSelector.boundTo(
                testcase.assertions(), primaryStep.id(), singleStep);
        List<AssertionRubric> failedOnStep = onStep.stream()
                .filter(assertion -> {
                    AssertionEvaluation evaluation = evaluations.get(assertion.id());
                    return evaluation != null && evaluation.status() == TestcaseResultStatus.FAILED;
                })
                .toList();
        AssertionRubric primary = primaryAssertionSelector.select(
                failedOnStep.isEmpty() ? onStep : failedOnStep);
        InvocationOutcome stepOutcome = primaryIndex >= 0 && primaryIndex < outcomes.size()
                ? outcomes.get(primaryIndex)
                : null;
        String inputDisplay = displayFormatter.formatInput(testcase, primaryStep);
        return displaysFor(primary, evaluations, stepOutcome, inputDisplay, fallbackActual, primaryStep);
    }

    private PrimaryDisplays displaysFor(AssertionRubric primary,
                                        Map<UUID, AssertionEvaluation> evaluations,
                                        InvocationOutcome invocationOutcome,
                                        String inputDisplay,
                                        String fallbackActual,
                                        InvocationRubric invocation) {
        if (primary == null) {
            return new PrimaryDisplays(inputDisplay, null, fallbackActual);
        }
        AssertionEvaluation primaryEvaluation = evaluations.get(primary.id());
        String expectedDisplay = displayFormatter.formatExpected(primary, invocation);
        String actualDisplay = fallbackActual != null
                ? fallbackActual
                : displayFormatter.formatActual(
                        primary, primaryEvaluation, invocationOutcome, null);
        return new PrimaryDisplays(inputDisplay, expectedDisplay, actualDisplay);
    }

    private int firstFailingOrLastRunIndex(List<InvocationRubric> steps,
                                           List<InvocationOutcome> outcomes,
                                           Map<UUID, AssertionEvaluation> evaluations,
                                           List<AssertionRubric> assertions) {
        int lastRun = outcomes == null || outcomes.isEmpty() ? 0 : outcomes.size() - 1;
        boolean singleStep = steps.size() == 1;
        for (int i = 0; i < outcomes.size() && i < steps.size(); i++) {
            InvocationRubric step = steps.get(i);
            InvocationOutcome outcome = outcomes.get(i);
            List<AssertionRubric> onStep = PrimaryAssertionSelector.boundTo(assertions, step.id(), singleStep);
            boolean failedAssert = onStep.stream().anyMatch(assertion -> {
                AssertionEvaluation evaluation = evaluations.get(assertion.id());
                return evaluation != null && evaluation.status() == TestcaseResultStatus.FAILED;
            });
            if (failedAssert) {
                return i;
            }
            if (outcome != null && outcome.kind() == InvocationOutcomeKind.THREW) {
                boolean exceptionPassed = onStep.stream()
                        .filter(assertion -> assertion.kind() == com.eiu.capstone.backend.model.AssertionKind.EXCEPTION)
                        .anyMatch(assertion -> {
                            AssertionEvaluation evaluation = evaluations.get(assertion.id());
                            return evaluation != null && evaluation.status() == TestcaseResultStatus.PASSED;
                        });
                if (!exceptionPassed) {
                    return i;
                }
            }
        }
        return lastRun;
    }

    private static Map<UUID, InvocationOutcome> zipOutcomes(List<InvocationRubric> steps,
                                                            List<InvocationOutcome> outcomes) {
        Map<UUID, InvocationOutcome> byId = new HashMap<>();
        if (steps == null || outcomes == null) {
            return byId;
        }
        for (int i = 0; i < steps.size() && i < outcomes.size(); i++) {
            byId.put(steps.get(i).id(), outcomes.get(i));
        }
        return byId;
    }

    private static InvocationOutcome boundOutcome(AssertionRubric assertion,
                                                  List<InvocationRubric> steps,
                                                  Map<UUID, InvocationOutcome> outcomeByStepId) {
        if (assertion.invocationId() != null) {
            if (outcomeByStepId.containsKey(assertion.invocationId())) {
                return outcomeByStepId.get(assertion.invocationId());
            }
            return null;
        }
        if (steps.size() == 1) {
            return outcomeByStepId.get(steps.get(0).id());
        }
        return null;
    }

    private static boolean hasUnassertedThrow(List<InvocationRubric> steps,
                                              List<InvocationOutcome> outcomes,
                                              List<AssertionRubric> assertions,
                                              Map<UUID, AssertionEvaluation> evaluations) {
        if (steps == null || outcomes == null) {
            return false;
        }
        boolean singleStep = steps.size() == 1;
        for (int i = 0; i < steps.size() && i < outcomes.size(); i++) {
            InvocationOutcome outcome = outcomes.get(i);
            if (outcome == null || outcome.kind() != InvocationOutcomeKind.THREW) {
                continue;
            }
            List<AssertionRubric> onStep = PrimaryAssertionSelector.boundTo(
                    assertions, steps.get(i).id(), singleStep);
            boolean exceptionPassed = onStep.stream()
                    .filter(assertion -> assertion.kind() == com.eiu.capstone.backend.model.AssertionKind.EXCEPTION)
                    .anyMatch(assertion -> {
                        AssertionEvaluation evaluation = evaluations.get(assertion.id());
                        return evaluation != null && evaluation.status() == TestcaseResultStatus.PASSED;
                    });
            if (!exceptionPassed) {
                return true;
            }
        }
        return false;
    }

    private String firstFailureFeedback(List<AssertionRubric> assertions,
                                        Map<UUID, AssertionEvaluation> evaluations,
                                        List<InvocationOutcome> outcomes) {
        for (AssertionRubric assertion : assertions) {
            AssertionEvaluation evaluation = evaluations.get(assertion.id());
            if (evaluation != null && evaluation.status() == TestcaseResultStatus.FAILED) {
                return evaluation.feedback();
            }
        }
        for (AssertionRubric assertion : assertions) {
            AssertionEvaluation evaluation = evaluations.get(assertion.id());
            if (evaluation != null && evaluation.status() == TestcaseResultStatus.SKIPPED) {
                return evaluation.feedback();
            }
        }
        if (outcomes != null) {
            for (InvocationOutcome outcome : outcomes) {
                if (outcome != null && outcome.kind() == InvocationOutcomeKind.THREW) {
                    return "Unexpected exception: " + outcome.exceptionSimpleName();
                }
            }
        }
        return "Assertion failed";
    }

    private record PrimaryDisplays(String input, String expected, String actual) {}

    private record Evaluation(double accuracy, PendingTestcaseResult pending) {}

    public record TestcasePillarResult(BigDecimal pillarPercentage, List<PendingTestcaseResult> results) {
        /**
         * Canonical result for a challenge with no operational testcases — matches what
         * {@link #grade} would return for an empty testcase list, without invoking the grader.
         */
        public static TestcasePillarResult empty() {
            return new TestcasePillarResult(BigDecimal.ZERO, List.of());
        }
    }

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
