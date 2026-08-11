package com.eiu.capstone.backend.grading.testcase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
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
            case RETURN_VALUE -> TestcaseLiteralFormatter.format(
                    jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null));
            case FIELD_STATE -> assertion.fieldName() + " = "
                    + TestcaseLiteralFormatter.format(jsonValueCoercer.coerceExpectedValue(
                            assertion.expectedValueJson(), assertion.fieldDataType()));
            case STDOUT -> String.valueOf(jsonValueCoercer.coerceExpectedValue(
                    assertion.expectedValueJson(), AssertionEvaluator.STRING_TYPE));
            case EXCEPTION -> jsonValueCoercer.parseExceptionType(assertion.expectedValueJson());
            case COMPARISON_RESULT -> TestcaseLiteralFormatter.format(
                    jsonValueCoercer.coerceExpectedValue(assertion.expectedValueJson(), null));
        };
    }

    public String formatActual(AssertionRubric assertion,
                               AssertionEvaluation evaluation,
                               InvocationOutcome invocationOutcome,
                               ComparisonOutcome comparisonOutcome) {
        if (evaluation == null) {
            return "";
        }
        return switch (assertion.kind()) {
            case RETURN_VALUE -> TestcaseLiteralFormatter.format(parseActual(evaluation.actualValueJson()));
            case FIELD_STATE -> assertion.fieldName() + " = "
                    + TestcaseLiteralFormatter.format(parseActual(evaluation.actualValueJson()));
            case STDOUT -> evaluation.actualValueJson() != null
                    ? stripQuotes(evaluation.actualValueJson())
                    : "";
            case EXCEPTION -> formatExceptionActual(evaluation, invocationOutcome);
            case COMPARISON_RESULT -> TestcaseLiteralFormatter.format(
                    comparisonOutcome != null ? comparisonOutcome.comparisonResult()
                            : parseActual(evaluation.actualValueJson()));
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
        if (invocation.hasReceiver()) {
            String receiverSetup = "new " + invocation.receiverClassName()
                    + "(" + formatArgs(invocation.receiverParamsJson()) + ")";
            return receiverSetup + "\n" + invocation.className() + "." + invocation.methodName() + "(" + args + ")";
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
            builder.append(TestcaseLiteralFormatter.format(
                    jsonValueCoercer.coerceFromNode(array.get(i), null)));
        }
        return builder.toString();
    }

    private String formatExceptionActual(AssertionEvaluation evaluation,
                                         InvocationOutcome invocationOutcome) {
        if (evaluation.status() == com.eiu.capstone.backend.model.TestcaseResultStatus.PASSED) {
            return stripQuotes(evaluation.actualValueJson());
        }
        if (invocationOutcome != null && invocationOutcome.kind() == InvocationOutcomeKind.NORMAL) {
            if (invocationOutcome.returnValue() != null) {
                return "no exception thrown\nreturned "
                        + TestcaseLiteralFormatter.format(invocationOutcome.returnValue());
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
