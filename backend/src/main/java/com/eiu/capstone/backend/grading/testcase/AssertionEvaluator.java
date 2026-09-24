package com.eiu.capstone.backend.grading.testcase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class AssertionEvaluator {

    static final String STRING_TYPE = "String";
    static final String OBJECT_CHECK_KEY = "$objectCheck";
    static final String OBJECT_CHECK_TYPE = "TYPE";
    static final String OBJECT_CHECK_FIELDS = "FIELDS";
    static final String OBJECT_CHECK_EQUALS = "EQUALS";
    static final String INSTANCE_REF_KEY = "$instance";
    /** Worker field snapshot when the live field value is the same object as a named instance. */
    static final String SAME_INSTANCE_KEY = "$sameInstance";

    private final JsonValueCoercer jsonValueCoercer;

    public AssertionEvaluator(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public AssertionEvaluation evaluate(AssertionRubric assertion,
                                        InvocationOutcome invocationOutcome,
                                        ComparisonOutcome comparisonOutcome) {
        if (invocationOutcome == null) {
            return notExecuted(assertion);
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> evaluateReturnValue(assertion, invocationOutcome);
            case FIELD_STATE -> evaluateFieldState(assertion, invocationOutcome);
            case STDOUT -> evaluateStdout(assertion, invocationOutcome);
            case EXCEPTION -> evaluateException(assertion, invocationOutcome);
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
        if (isObjectCheck(assertion.expectedValueJson())) {
            return evaluateObjectCheck(assertion, outcome);
        }
        Object expected = jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null);
        boolean passed = ValueComparator.matches(outcome.returnValue(), expected, assertion.comparisonMode());
        return passed
                ? success(assertion, outcome.returnValue(), "Return value matches")
                : failure(assertion, outcome.returnValue(), "Return value mismatch");
    }

    private AssertionEvaluation evaluateObjectCheck(AssertionRubric assertion, InvocationOutcome outcome) {
        JsonNode node = jsonValueCoercer.parseTree(assertion.expectedValueJson());
        String kind = node.get(OBJECT_CHECK_KEY).asText();
        if (OBJECT_CHECK_TYPE.equals(kind)) {
            return failure(assertion, null, "Type-only object checks are not supported");
        }
        if (OBJECT_CHECK_FIELDS.equals(kind)) {
            return failure(assertion, null, "Object field maps are not supported; use field state");
        }
        if (OBJECT_CHECK_EQUALS.equals(kind)) {
            JsonNode instanceNode = node.get(INSTANCE_REF_KEY);
            String name = instanceNode != null && instanceNode.isTextual() ? instanceNode.asText() : null;
            Boolean matched = name != null && outcome.equalsNamed() != null
                    ? outcome.equalsNamed().get(name)
                    : null;
            if (Boolean.TRUE.equals(matched)) {
                return success(assertion, true, "equals() matches");
            }
            return failure(assertion, matched, "equals() mismatch");
        }
        return failure(assertion, null, "Unknown object check: " + kind);
    }

    private boolean isObjectCheck(String expectedJson) {
        if (expectedJson == null || expectedJson.isBlank()) {
            return false;
        }
        try {
            JsonNode node = jsonValueCoercer.parseTree(expectedJson);
            return node != null && node.isObject() && node.has(OBJECT_CHECK_KEY);
        } catch (Exception e) {
            return false;
        }
    }

    private AssertionEvaluation evaluateFieldState(AssertionRubric assertion, InvocationOutcome outcome) {
        Optional<AssertionEvaluation> precondition = invocationPrecondition(assertion, outcome);
        if (precondition.isPresent()) {
            return precondition.get();
        }
        Optional<AssertionEvaluation> namedInstance = evaluateFieldStateNamedInstance(assertion, outcome);
        if (namedInstance.isPresent()) {
            return namedInstance.get();
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

    private Optional<AssertionEvaluation> evaluateFieldStateNamedInstance(AssertionRubric assertion,
                                                                            InvocationOutcome outcome) {
        JsonNode expectedNode = jsonValueCoercer.parseTree(assertion.expectedValueJson());
        if (expectedNode == null || !expectedNode.isObject() || !expectedNode.has(INSTANCE_REF_KEY)) {
            return Optional.empty();
        }
        String expectedName = expectedNode.get(INSTANCE_REF_KEY).asText();
        Object actual = outcome.fieldSnapshots() != null
                ? outcome.fieldSnapshots().get(assertion.fieldName())
                : null;
        String actualName = readSameInstanceName(actual);
        if (actualName == null) {
            return Optional.of(failure(assertion, actual, assertion.fieldName() + " is not the named instance"));
        }
        boolean passed = expectedName != null && expectedName.equals(actualName);
        return Optional.of(passed
                ? success(assertion, actualName, assertion.fieldName() + " is " + expectedName)
                : failure(assertion, actualName, assertion.fieldName() + " mismatch"));
    }

    static String readSameInstanceName(Object snapshotValue) {
        if (snapshotValue instanceof Map<?, ?> map) {
            Object name = map.get(SAME_INSTANCE_KEY);
            if (name != null) {
                return String.valueOf(name);
            }
        }
        if (snapshotValue instanceof JsonNode node && node.isObject() && node.has(SAME_INSTANCE_KEY)) {
            return node.get(SAME_INSTANCE_KEY).asText();
        }
        if (snapshotValue instanceof String text && !text.isBlank()) {
            try {
                JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
                if (node.isObject() && node.has(SAME_INSTANCE_KEY)) {
                    return node.get(SAME_INSTANCE_KEY).asText();
                }
            } catch (Exception ignored) {
                // not JSON
            }
        }
        return null;
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
            return failure(assertion, null, "No exception thrown");
        }
        String actualType = outcome.exceptionSimpleName();
        boolean passed = matchesExceptionType(actualType, outcome.exceptionSuperclassSimpleNames(), expectedType);
        return passed
                ? success(assertion, actualType, "Exception matches")
                : failure(assertion, actualType, "Exception mismatch");
    }

    private Optional<AssertionEvaluation> invocationPrecondition(AssertionRubric assertion,
                                                                 InvocationOutcome outcome) {
        if (outcome == null) {
            return Optional.of(notExecuted(assertion));
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

    private AssertionEvaluation notExecuted(AssertionRubric assertion) {
        return new AssertionEvaluation(
                assertion.id(),
                TestcaseResultStatus.FAILED,
                null,
                "Not executed");
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
