package com.eiu.capstone.backend.grading.testcase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

@Component
public class AssertionEvaluator {

    static final String STRING_TYPE = "String";

    private final JsonValueCoercer jsonValueCoercer;

    public AssertionEvaluator(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public AssertionEvaluation evaluate(AssertionRubric assertion,
                                        InvocationOutcome invocationOutcome,
                                        ComparisonOutcome comparisonOutcome) {
        if (assertion.kind() != com.eiu.capstone.backend.model.AssertionKind.COMPARISON_RESULT
                && invocationOutcome == null) {
            return skipped(assertion);
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> evaluateReturnValue(assertion, invocationOutcome);
            case FIELD_STATE -> evaluateFieldState(assertion, invocationOutcome);
            case STDOUT -> evaluateStdout(assertion, invocationOutcome);
            case EXCEPTION -> evaluateException(assertion, invocationOutcome);
            case COMPARISON_RESULT -> evaluateComparison(assertion, comparisonOutcome);
        };
    }

    private AssertionEvaluation evaluateReturnValue(AssertionRubric assertion, InvocationOutcome outcome) {
        Optional<AssertionEvaluation> precondition = invocationPrecondition(assertion, outcome);
        if (precondition.isPresent()) {
            return precondition.get();
        }
        if (outcome.kind() == InvocationOutcomeKind.THREW) {
            return failure(assertion, outcome.exceptionSimpleName(),
                    "Unexpected exception: " + outcome.exceptionSimpleName());
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null);
        boolean passed = ValueComparator.matches(outcome.returnValue(), expected, assertion.comparisonMode());
        return passed
                ? success(assertion, outcome.returnValue(), "Return value matches")
                : failure(assertion, outcome.returnValue(), "Return value mismatch");
    }

    private AssertionEvaluation evaluateFieldState(AssertionRubric assertion, InvocationOutcome outcome) {
        Optional<AssertionEvaluation> precondition = invocationPrecondition(assertion, outcome);
        if (precondition.isPresent()) {
            return precondition.get();
        }
        Object actual = outcome.fieldSnapshots() != null
                ? outcome.fieldSnapshots().get(assertion.fieldName())
                : null;
        if (actual == null && (outcome.fieldSnapshots() == null
                || !outcome.fieldSnapshots().containsKey(assertion.fieldName()))) {
            return failure(assertion, null, "Could not read field: " + assertion.fieldName());
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(
                assertion.expectedValueJson(), assertion.fieldDataType());
        boolean passed = ValueComparator.matches(actual, expected, assertion.comparisonMode());
        return passed
                ? success(assertion, actual, assertion.fieldName() + " matches")
                : failure(assertion, actual, assertion.fieldName() + " mismatch");
    }

    private AssertionEvaluation evaluateStdout(AssertionRubric assertion, InvocationOutcome outcome) {
        Optional<AssertionEvaluation> precondition = invocationPrecondition(assertion, outcome);
        if (precondition.isPresent()) {
            return precondition.get();
        }
        String actual = outcome.stdout() != null ? outcome.stdout() : "";
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), STRING_TYPE);
        boolean passed = ValueComparator.matches(actual, expected, assertion.comparisonMode());
        return passed
                ? success(assertion, actual, "Stdout matches")
                : failure(assertion, actual, "Stdout mismatch");
    }

    private AssertionEvaluation evaluateException(AssertionRubric assertion, InvocationOutcome outcome) {
        Optional<AssertionEvaluation> precondition = invocationPrecondition(assertion, outcome);
        if (precondition.isPresent()) {
            return precondition.get();
        }
        String expectedType = jsonValueCoercer.parseExceptionType(assertion.expectedValueJson());
        if (outcome.kind() != InvocationOutcomeKind.THREW) {
            String secondary = outcome.returnValue() != null
                    ? "returned " + TestcaseLiteralFormatter.format(outcome.returnValue())
                    : "no exception thrown";
            return failure(assertion, null, secondary);
        }
        String actualType = outcome.exceptionSimpleName();
        boolean passed = matchesExceptionType(actualType, outcome.exceptionSuperclassSimpleNames(), expectedType);
        return passed
                ? success(assertion, actualType, "Exception matches")
                : failure(assertion, actualType, "Expected " + expectedType + " but got " + actualType);
    }

    private AssertionEvaluation evaluateComparison(AssertionRubric assertion, ComparisonOutcome outcome) {
        if (outcome == null) {
            return failure(assertion, null, "Comparison not available for this assertion");
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome.errorMessage());
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null);
        boolean passed = ValueComparator.matches(outcome.comparisonResult(), expected, assertion.comparisonMode());
        return passed
                ? success(assertion, outcome.comparisonResult(), "Comparison matches")
                : failure(assertion, outcome.comparisonResult(), "Comparison mismatch");
    }

    private Optional<AssertionEvaluation> invocationPrecondition(AssertionRubric assertion,
                                                                 InvocationOutcome outcome) {
        if (outcome == null) {
            return Optional.of(skipped(assertion));
        }
        if (outcome.kind() == InvocationOutcomeKind.TIMED_OUT) {
            return Optional.of(failure(assertion, null, "Invocation timed out"));
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return Optional.of(failure(assertion, null, outcome.errorMessage()));
        }
        return Optional.empty();
    }

    static boolean matchesExceptionType(String simpleName, List<String> superNames, String expectedSimpleName) {
        if (expectedSimpleName == null) {
            return false;
        }
        if (expectedSimpleName.equals(simpleName)) {
            return true;
        }
        return superNames != null && superNames.contains(expectedSimpleName);
    }

    private AssertionEvaluation skipped(AssertionRubric assertion) {
        return new AssertionEvaluation(
                assertion.id(),
                TestcaseResultStatus.SKIPPED,
                null,
                "Step did not run");
    }

    private AssertionEvaluation success(AssertionRubric assertion, Object actual, String feedback) {
        return new AssertionEvaluation(
                assertion.id(),
                TestcaseResultStatus.PASSED,
                jsonValueCoercer.toJson(actual),
                feedback);
    }

    private AssertionEvaluation failure(AssertionRubric assertion, Object actual, String feedback) {
        return new AssertionEvaluation(
                assertion.id(),
                TestcaseResultStatus.FAILED,
                actual == null ? null : jsonValueCoercer.toJson(actual),
                feedback);
    }
}
