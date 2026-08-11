package com.eiu.capstone.backend.grading.testcase;

import java.lang.reflect.Field;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class AssertionEvaluator {

    private final JsonValueCoercer jsonValueCoercer;

    public AssertionEvaluator(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public AssertionEvaluation evaluate(AssertionRubric assertion,
                                        InvocationOutcome invocationOutcome,
                                        ComparisonOutcome comparisonOutcome) {
        return switch (assertion.kind()) {
            case RETURN_VALUE -> evaluateReturnValue(assertion, invocationOutcome);
            case FIELD_STATE -> evaluateFieldState(assertion, invocationOutcome);
            case STDOUT -> evaluateStdout(assertion, invocationOutcome);
            case EXCEPTION -> evaluateException(assertion, invocationOutcome);
            case COMPARISON_RESULT -> evaluateComparison(assertion, comparisonOutcome);
        };
    }

    private AssertionEvaluation evaluateReturnValue(AssertionRubric assertion, InvocationOutcome outcome) {
        if (outcome.kind() == InvocationOutcomeKind.TIMED_OUT) {
            return failure(assertion, null, "Invocation timed out");
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome.errorMessage());
        }
        if (outcome.kind() == InvocationOutcomeKind.THREW) {
            return failure(assertion, outcome.caughtException().getClass().getSimpleName(),
                    "Unexpected exception: " + outcome.caughtException().getClass().getSimpleName());
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null);
        boolean passed = ValueComparator.matches(outcome.returnValue(), expected, assertion.comparisonMode());
        return passed
                ? success(assertion, outcome.returnValue(), "Return value matches")
                : failure(assertion, outcome.returnValue(), "Return value mismatch");
    }

    private AssertionEvaluation evaluateFieldState(AssertionRubric assertion, InvocationOutcome outcome) {
        if (outcome.kind() == InvocationOutcomeKind.TIMED_OUT) {
            return failure(assertion, null, "Invocation timed out");
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome.errorMessage());
        }
        Object target = outcome.instance() != null ? outcome.instance() : outcome.returnValue();
        if (target == null) {
            return failure(assertion, null, "No instance available for field check");
        }
        try {
            Object actual = readField(target, assertion.fieldName());
            Object expected = jsonValueCoercer.coerceExpectedValue(
                    assertion.expectedValueJson(), assertion.fieldDataType());
            boolean passed = ValueComparator.matches(actual, expected, assertion.comparisonMode());
            return passed
                    ? success(assertion, actual, assertion.fieldName() + " matches")
                    : failure(assertion, actual, assertion.fieldName() + " mismatch");
        } catch (ReflectiveOperationException e) {
            return failure(assertion, null, "Could not read field: " + assertion.fieldName());
        }
    }

    private AssertionEvaluation evaluateStdout(AssertionRubric assertion, InvocationOutcome outcome) {
        if (outcome.kind() == InvocationOutcomeKind.TIMED_OUT) {
            return failure(assertion, null, "Invocation timed out");
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome.errorMessage());
        }
        String actual = outcome.stdout() != null ? outcome.stdout() : "";
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), "String");
        boolean passed = ValueComparator.matches(actual, expected, assertion.comparisonMode());
        return passed
                ? success(assertion, actual, "Stdout matches")
                : failure(assertion, actual, "Stdout mismatch");
    }

    private AssertionEvaluation evaluateException(AssertionRubric assertion, InvocationOutcome outcome) {
        if (outcome.kind() == InvocationOutcomeKind.TIMED_OUT) {
            return failure(assertion, null, "Invocation timed out");
        }
        if (outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome.errorMessage());
        }
        String expectedType = expectedExceptionType(assertion.expectedValueJson());
        if (outcome.kind() != InvocationOutcomeKind.THREW) {
            String secondary = outcome.returnValue() != null
                    ? "returned " + formatLiteral(outcome.returnValue())
                    : "no exception thrown";
            return failure(assertion, null, secondary);
        }
        String actualType = outcome.caughtException().getClass().getSimpleName();
        boolean passed = actualType.equals(expectedType);
        return passed
                ? success(assertion, actualType, "Exception matches")
                : failure(assertion, actualType, "Expected " + expectedType + " but got " + actualType);
    }

    private AssertionEvaluation evaluateComparison(AssertionRubric assertion, ComparisonOutcome outcome) {
        if (outcome == null || outcome.kind() == InvocationOutcomeKind.ERROR) {
            return failure(assertion, null, outcome != null ? outcome.errorMessage() : "Comparison failed");
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null);
        boolean passed = ValueComparator.matches(outcome.comparisonResult(), expected, assertion.comparisonMode());
        return passed
                ? success(assertion, outcome.comparisonResult(), "Comparison matches")
                : failure(assertion, outcome.comparisonResult(), "Comparison mismatch");
    }

    private Object readField(Object target, String fieldName) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private String expectedExceptionType(String expectedValueJson) {
        JsonNode node = jsonValueCoercer.parseTree(expectedValueJson);
        if (node.isObject() && node.has("type")) {
            return node.get("type").asText();
        }
        return node.asText();
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

    private String formatLiteral(Object value) {
        if (value instanceof String text) {
            return "\"" + text + "\"";
        }
        return String.valueOf(value);
    }
}
