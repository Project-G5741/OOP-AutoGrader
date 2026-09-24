package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluation;
import com.eiu.capstone.backend.grading.testcase.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

class TestcaseDisplayFormatterTest {

    private final TestcaseDisplayFormatter formatter = new TestcaseDisplayFormatter(new JsonValueCoercer());

    @Test
    void returnValueActualMatchesExpectedLiteralStyle() {
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                null,
                null,
                null,
                null,
                "\"\"",
                ComparisonMode.EXACT,
                0);

        assertEquals("\"\"", formatter.formatExpected(assertion));
        assertEquals("\"\"", formatter.formatActual(
                assertion,
                evaluation("\"\"", TestcaseResultStatus.PASSED),
                null,
                null));

        assertEquals("null", formatter.formatExpected(
                new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        null,
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
        assertEquals("null", formatter.formatActual(
                assertion,
                evaluation("null", TestcaseResultStatus.FAILED),
                null,
                null));
    }

    private static AssertionEvaluation evaluation(String actualJson, TestcaseResultStatus status) {
        return new AssertionEvaluation(UUID.randomUUID(), status, actualJson, "");
    }
}
