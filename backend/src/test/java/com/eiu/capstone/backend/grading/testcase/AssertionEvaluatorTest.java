package com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

class AssertionEvaluatorTest {

    private final AssertionEvaluator evaluator = new AssertionEvaluator(new JsonValueCoercer());

    @Test
    void nullInvocationOutcomeReturnsFailedWithoutNpe() {
        AssertionRubric assertion = assertion(AssertionKind.RETURN_VALUE, "42");

        AssertionEvaluation result = evaluator.evaluate(assertion, null, null);

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertTrue(result.feedback().contains("Invocation not available"));
    }

    @Test
    void exceptionAssertionMatchesSubclass() {
        AssertionRubric assertion = assertion(AssertionKind.EXCEPTION, "\"IllegalArgumentException\"");
        InvocationOutcome outcome = InvocationOutcome.threw(
                null,
                new NumberFormatException("bad"),
                "");

        AssertionEvaluation result = evaluator.evaluate(assertion, outcome, null);

        assertEquals(TestcaseResultStatus.PASSED, result.status());
    }

    @Test
    void comparisonAssertionHandlesNullOutcome() {
        AssertionRubric assertion = assertion(AssertionKind.COMPARISON_RESULT, "true");

        AssertionEvaluation result = evaluator.evaluate(assertion, null, null);

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertTrue(result.feedback().contains("Comparison not available"));
    }

    private AssertionRubric assertion(AssertionKind kind, String expectedJson) {
        return new AssertionRubric(
                UUID.randomUUID(),
                kind,
                null,
                null,
                null,
                null,
                expectedJson,
                ComparisonMode.EXACT,
                0);
    }
}
