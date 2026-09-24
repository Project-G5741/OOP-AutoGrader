package com.eiu.capstone.backend.grading.testcase;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.InvocationKind;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class TestcaseDisplayFormatter {

    private final JsonValueCoercer jsonValueCoercer;

    public TestcaseDisplayFormatter(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public String formatInput(TestcaseRubric testcase) {
        return formatInput(testcase, firstInvocation(testcase));
    }

    public String formatInput(TestcaseRubric testcase, InvocationRubric invocation) {
        if (invocation == null) {
            return "";
        }
        return formatInvocationInput(invocation);
    }

    private static InvocationRubric firstInvocation(TestcaseRubric testcase) {
        if (testcase == null) {
            return null;
        }
        if (testcase.invocations() != null && !testcase.invocations().isEmpty()) {
            return testcase.invocations().get(0);
        }
        return testcase.invocation();
    }

    public String formatExpected(AssertionRubric assertion) {
        return formatExpected(assertion, null);
    }

    public String formatExpected(AssertionRubric assertion, InvocationRubric invocation) {
        if (assertion == null) {
            return "";
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> formatReturnExpected(assertion, invocation);
            case FIELD_STATE -> formatFieldStateExpected(assertion);
            case STDOUT -> String.valueOf(jsonValueCoercer.coerceExpectedValue(
                    assertion.expectedValueJson(), AssertionEvaluator.STRING_TYPE));
            case EXCEPTION -> jsonValueCoercer.parseExceptionType(assertion.expectedValueJson());
        };
    }

    private String formatFieldStateActual(String actualValueJson) {
        Object parsed = parseActual(actualValueJson);
        String sameInstance = AssertionEvaluator.readSameInstanceName(parsed);
        if (sameInstance != null) {
            return "$" + sameInstance;
        }
        return TestcaseLiteralFormatter.format(parsed);
    }

    private String formatFieldStateExpected(AssertionRubric assertion) {
        try {
            JsonNode node = jsonValueCoercer.parseTree(assertion.expectedValueJson());
            if (node != null && node.isObject() && node.has(AssertionEvaluator.INSTANCE_REF_KEY)) {
                return assertion.fieldName() + " = $" + node.get(AssertionEvaluator.INSTANCE_REF_KEY).asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return assertion.fieldName() + " = "
                + TestcaseLiteralFormatter.format(jsonValueCoercer.coerceExpectedValue(
                        assertion.expectedValueJson(), assertion.fieldDataType()));
    }

    private String formatReturnExpected(AssertionRubric assertion, InvocationRubric invocation) {
        try {
            JsonNode node = jsonValueCoercer.parseTree(assertion.expectedValueJson());
            if (node != null && node.isObject() && node.has(AssertionEvaluator.OBJECT_CHECK_KEY)) {
                String kind = node.get(AssertionEvaluator.OBJECT_CHECK_KEY).asText();
                if (AssertionEvaluator.OBJECT_CHECK_FIELDS.equals(kind)) {
                    JsonNode fields = node.get("fields");
                    return fields == null ? "fields" : fields.toString();
                }
                if (AssertionEvaluator.OBJECT_CHECK_EQUALS.equals(kind)) {
                    JsonNode instance = node.get(AssertionEvaluator.INSTANCE_REF_KEY);
                    return instance != null ? "equals(" + instance.asText() + ")" : "equals()";
                }
            }
        } catch (Exception ignored) {
            // fall through to scalar formatting
        }
        return TestcaseLiteralFormatter.format(
                jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null));
    }

    public String formatActual(AssertionRubric assertion,
                               AssertionEvaluation evaluation,
                               InvocationOutcome invocationOutcome,
                               ComparisonOutcome comparisonOutcome) {
        if (evaluation == null) {
            return "";
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> formatReturnActual(evaluation);
            case FIELD_STATE -> assertion.fieldName() + " = " + formatFieldStateActual(evaluation.actualValueJson());
            case STDOUT -> {
                String stdout = evaluation.actualValueJson() != null
                        ? stripQuotes(evaluation.actualValueJson())
                        : "";
                if (invocationOutcome != null && invocationOutcome.stdoutTruncated()) {
                    stdout = stdout + " [truncated]";
                }
                yield stdout;
            }
            case EXCEPTION -> formatExceptionActual(evaluation, invocationOutcome);
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
            String constructed = "new " + invocation.className() + "(" + args + ")";
            if (hasInstanceName(invocation)) {
                return invocation.instanceName() + " = " + constructed;
            }
            return constructed;
        }
        if (hasInstanceName(invocation)) {
            return invocation.instanceName() + "." + invocation.methodName() + "(" + args + ")";
        }
        if (invocation.hasReceiver()) {
            String receiverSetup = "new " + invocation.receiverClassName()
                    + "(" + formatArgs(invocation.receiverParamsJson()) + ")";
            return receiverSetup + "\n" + invocation.className() + "." + invocation.methodName() + "(" + args + ")";
        }
        return invocation.className() + "." + invocation.methodName() + "(" + args + ")";
    }

    private static boolean hasInstanceName(InvocationRubric invocation) {
        return invocation.instanceName() != null && !invocation.instanceName().isBlank();
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
            JsonNode param = array.get(i);
            if (param.isObject() && param.has(AssertionEvaluator.INSTANCE_REF_KEY)) {
                builder.append('$').append(param.get(AssertionEvaluator.INSTANCE_REF_KEY).asText());
            } else {
                builder.append(TestcaseLiteralFormatter.format(
                        jsonValueCoercer.coerceFromNode(param, null)));
            }
        }
        return builder.toString();
    }

    private String formatExceptionActual(AssertionEvaluation evaluation,
                                         InvocationOutcome invocationOutcome) {
        if (evaluation.status() == com.eiu.capstone.backend.model.TestcaseResultStatus.PASSED) {
            return stripQuotes(evaluation.actualValueJson());
        }
        if (evaluation.feedback() != null && !evaluation.feedback().isBlank()) {
            return evaluation.feedback();
        }
        if (invocationOutcome != null && invocationOutcome.kind() == InvocationOutcomeKind.THREW) {
            return "Exception mismatch";
        }
        return "No exception thrown";
    }

    private String formatReturnActual(AssertionEvaluation evaluation) {
        String raw = evaluation.actualValueJson();
        if (raw == null || raw.isBlank()) {
            return TestcaseLiteralFormatter.format(null);
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonNode node = jsonValueCoercer.parseTree(trimmed);
                if (node.isObject() || node.isArray()) {
                    return node.toString();
                }
            } catch (Exception ignored) {
                // fall through to scalar formatting
            }
        }
        return TestcaseLiteralFormatter.format(parseActual(raw));
    }

    private Object parseActual(String actualValueJson) {
        if (actualValueJson == null || actualValueJson.isBlank()) {
            return null;
        }
        return jsonValueCoercer.coerceExpectedValue(actualValueJson, null);
    }

    private String stripQuotes(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        if (json.startsWith("\"") && json.endsWith("\"")) {
            Object parsed = jsonValueCoercer.coerceExpectedValue(json, AssertionEvaluator.STRING_TYPE);
            return parsed != null ? String.valueOf(parsed) : "";
        }
        return json;
    }
}
