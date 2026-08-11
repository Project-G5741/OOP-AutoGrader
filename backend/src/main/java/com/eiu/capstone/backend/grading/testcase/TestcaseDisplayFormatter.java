package com.eiu.capstone.backend.grading.testcase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseType;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class TestcaseDisplayFormatter {

    private final JsonValueCoercer jsonValueCoercer;

    public TestcaseDisplayFormatter(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public String formatInput(TestcaseRubric testcase) {
        if (testcase.testcaseType() == TestcaseType.COMPARISON) {
            return testcase.instances().stream()
                    .sorted(java.util.Comparator.comparing(InstanceRubric::label))
                    .map(this::formatInstanceInput)
                    .collect(Collectors.joining("\n"));
        }
        InvocationRubric invocation = testcase.invocation();
        if (invocation == null) {
            return "";
        }
        return formatInvocationInput(invocation);
    }

    public String formatExpected(AssertionRubric assertion) {
        if (assertion == null) {
            return "";
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> formatLiteral(jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null));
            case FIELD_STATE -> assertion.fieldName() + " = "
                    + formatLiteral(jsonValueCoercer.coerceExpectedValue(
                            assertion.expectedValueJson(), assertion.fieldDataType()));
            case STDOUT -> String.valueOf(jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), "String"));
            case EXCEPTION -> expectedExceptionType(assertion.expectedValueJson());
            case COMPARISON_RESULT -> formatLiteral(jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null));
        };
    }

    public String formatActual(AssertionRubric assertion,
                               AssertionEvaluation evaluation,
                               InvocationOutcome invocationOutcome,
                               ComparisonOutcome comparisonOutcome) {
        if (evaluation == null) {
            return "";
        }
        if (evaluation.status() == com.eiu.capstone.backend.model.TestcaseResultStatus.ERROR) {
            return evaluation.feedback();
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> formatLiteral(parseActual(evaluation.actualValueJson()));
            case FIELD_STATE -> assertion.fieldName() + " = " + formatLiteral(parseActual(evaluation.actualValueJson()));
            case STDOUT -> evaluation.actualValueJson() != null
                    ? stripQuotes(evaluation.actualValueJson())
                    : "";
            case EXCEPTION -> formatExceptionActual(assertion, evaluation, invocationOutcome);
            case COMPARISON_RESULT -> formatLiteral(
                    comparisonOutcome != null ? comparisonOutcome.comparisonResult() : parseActual(evaluation.actualValueJson()));
        };
    }

    public String formatExpandedAssertion(AssertionRubric assertion, String actualValueJson) {
        return formatActual(assertion,
                new AssertionEvaluation(assertion.id(),
                        com.eiu.capstone.backend.model.TestcaseResultStatus.FAILED,
                        actualValueJson,
                        ""),
                null,
                null);
    }

    private String formatInvocationInput(InvocationRubric invocation) {
        String args = formatArgs(invocation.paramsJson());
        if (invocation.kind() == InvocationKind.CONSTRUCTOR) {
            return "new " + invocation.className() + "(" + args + ")";
        }
        return invocation.className() + "." + invocation.methodName() + "(" + args + ")";
    }

    private String formatInstanceInput(InstanceRubric instance) {
        return instance.label() + " = new " + instance.className() + "(" + formatArgs(instance.paramsJson()) + ")";
    }

    private String formatArgs(String paramsJson) {
        JsonNode array = jsonValueCoercer.parseTree(paramsJson);
        if (!array.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(formatLiteral(jsonValueCoercer.coerceExpectedValue(array.get(i).toString(), null)));
        }
        return builder.toString();
    }

    private String formatExceptionActual(AssertionRubric assertion,
                                         AssertionEvaluation evaluation,
                                         InvocationOutcome invocationOutcome) {
        if (evaluation.status() == com.eiu.capstone.backend.model.TestcaseResultStatus.PASSED) {
            return stripQuotes(evaluation.actualValueJson());
        }
        if (invocationOutcome != null && invocationOutcome.kind() == InvocationOutcomeKind.NORMAL) {
            if (invocationOutcome.returnValue() != null) {
                return "no exception thrown\nreturned " + formatLiteral(invocationOutcome.returnValue());
            }
            return "no exception thrown";
        }
        return evaluation.feedback() != null ? evaluation.feedback() : "no exception thrown";
    }

    private Object parseActual(String actualValueJson) {
        if (actualValueJson == null || actualValueJson.isBlank()) {
            return null;
        }
        return jsonValueCoercer.coerceExpectedValue(actualValueJson, null);
    }

    private String expectedExceptionType(String expectedValueJson) {
        JsonNode node = jsonValueCoercer.parseTree(expectedValueJson);
        if (node.isObject() && node.has("type")) {
            return node.get("type").asText();
        }
        return node.asText();
    }

    private String formatLiteral(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + text + "\"";
        }
        if (value.getClass().isArray()) {
            return arrayToString(value);
        }
        return String.valueOf(value);
    }

    private String arrayToString(Object array) {
        if (array instanceof int[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof long[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof double[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof float[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof boolean[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof byte[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof short[] values) {
            return java.util.Arrays.toString(values);
        }
        if (array instanceof char[] values) {
            return java.util.Arrays.toString(values);
        }
        return java.util.Arrays.toString((Object[]) array);
    }

    private String stripQuotes(String json) {
        if (json == null) {
            return "";
        }
        if (json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1);
        }
        return json;
    }
}
