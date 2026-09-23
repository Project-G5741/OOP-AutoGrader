package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

class AssertionEvaluatorTest {

    private final AssertionEvaluator evaluator = new AssertionEvaluator(new JsonValueCoercer());

    @Test
    void nullInvocationOutcomeFailsAsNotExecutedWithoutNpe() {
        AssertionRubric assertion = assertion(AssertionKind.RETURN_VALUE, "42");

        AssertionEvaluation result = evaluator.evaluate(assertion, null, null);

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertTrue(result.feedback().toLowerCase().contains("not executed"), result.feedback());
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
    void objectCheckTypePassesWhenWorkerReportsMatchingType() {
        AssertionRubric assertion = assertion(
                AssertionKind.RETURN_VALUE, "{\"$objectCheck\":\"TYPE\"}");
        InvocationOutcome outcome = InvocationOutcome.normal(
                null, "", false, Map.of(), "Money", Map.of(), Map.of());

        AssertionEvaluation result = evaluator.evaluate(assertion, outcome, null);

        assertEquals(TestcaseResultStatus.PASSED, result.status());
    }

    @Test
    void objectCheckFieldsPassesWhenWorkerReportsMatchingLiterals() {
        AssertionRubric assertion = assertion(
                AssertionKind.RETURN_VALUE,
                "{\"$objectCheck\":\"FIELDS\",\"fields\":{\"amount\":10}}");
        InvocationOutcome outcome = InvocationOutcome.normal(
                null, "", false, Map.of(), "Money", Map.of("amount", 10), Map.of());

        AssertionEvaluation result = evaluator.evaluate(assertion, outcome, null);

        assertEquals(TestcaseResultStatus.PASSED, result.status());
    }

    @Test
    void objectCheckEqualsPassesWhenWorkerReportsNamedMatch() {
        AssertionRubric assertion = assertion(
                AssertionKind.RETURN_VALUE,
                "{\"$objectCheck\":\"EQUALS\",\"$instance\":\"other\"}");
        InvocationOutcome outcome = InvocationOutcome.normal(
                null, "", false, Map.of(), "Coin", Map.of(), Map.of("other", true));

        AssertionEvaluation result = evaluator.evaluate(assertion, outcome, null);

        assertEquals(TestcaseResultStatus.PASSED, result.status());
    }

    @Test
    void ae7ExceptionStdoutAndFieldStateEvaluateIndependently() {
        InvocationOutcome outcome = InvocationOutcome.threw(
                "printed",
                false,
                "IllegalArgumentException",
                List.of("RuntimeException"),
                Map.of("age", 30));

        AssertionEvaluation exception = evaluator.evaluate(
                assertion(AssertionKind.EXCEPTION, "\"IllegalArgumentException\""), outcome, null);
        AssertionEvaluation stdout = evaluator.evaluate(
                assertion(AssertionKind.STDOUT, "\"printed\""), outcome, null);
        AssertionEvaluation field = evaluator.evaluate(
                new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.FIELD_STATE,
                        null,
                        null,
                        "age",
                        "int",
                        "30",
                        ComparisonMode.EXACT,
                        2),
                outcome,
                null);

        assertEquals(TestcaseResultStatus.PASSED, exception.status());
        assertEquals(TestcaseResultStatus.PASSED, stdout.status());
        assertEquals(TestcaseResultStatus.PASSED, field.status());
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
