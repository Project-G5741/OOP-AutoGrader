package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

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
    void nullInvocationOutcomeReturnsSkippedWithoutNpe() {
        AssertionRubric assertion = assertion(AssertionKind.RETURN_VALUE, "42");

        AssertionEvaluation result = evaluator.evaluate(assertion, null, null);

        assertEquals(TestcaseResultStatus.SKIPPED, result.status());
        assertTrue(result.feedback() == null || !result.feedback().isBlank());
    }

    @Test
    void exceptionAssertionMatchesSubclass() {
        AssertionRubric assertion = assertion(AssertionKind.EXCEPTION, "\"IllegalArgumentException\"");
        InvocationOutcome outcome = InvocationOutcome.threw(
                "",
                false,
                "NumberFormatException",
                List.of("IllegalArgumentException", "RuntimeException", "Exception", "Object"));

        AssertionEvaluation result = evaluator.evaluate(assertion, outcome, null);

        assertEquals(TestcaseResultStatus.PASSED, result.status());
    }

    @Test
    void fieldStateUsesSerializedSnapshot() {
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.FIELD_STATE,
                null,
                null,
                "speed",
                "int",
                "5",
                ComparisonMode.EXACT,
                0);
        InvocationOutcome outcome = InvocationOutcome.normal(
                null, "", false, java.util.Map.of("speed", 5));

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
