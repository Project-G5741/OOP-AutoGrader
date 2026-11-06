package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluation;
import com.eiu.capstone.backend.grading.testcase.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

class TestcaseDisplayFormatterTest {

    private final TestcaseDisplayFormatter formatter = new TestcaseDisplayFormatter(new JsonValueCoercer());

    @Test
    void compositionInputJoinsAllSteps() {
        UUID constructId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();
        InvocationRubric construct = new InvocationRubric(
                constructId,
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                "Car",
                null,
                List.of("int", "String"),
                "[6767, \"SixSeven\"]",
                null,
                null,
                List.of(),
                null,
                "car",
                null,
                null,
                null);
        InvocationRubric accelerate = new InvocationRubric(
                callId,
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Car",
                "accelerate",
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null,
                "car",
                null,
                null,
                null);
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "drive",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(),
                List.of(construct, accelerate),
                null);

        String input = formatter.formatInput(testcase);

        assertEquals(
                "Car car = new Car(6767, \"SixSeven\")\ncar.accelerate()",
                input);
    }

    @Test
    void callAsMethodUsesDispatchTypeInInput() {
        UUID constructId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();
        InvocationRubric construct = new InvocationRubric(
                constructId,
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                "Circle",
                null,
                List.of("double"),
                "[3.14]",
                null,
                null,
                List.of(),
                null,
                "c1",
                null,
                null,
                null);
        InvocationRubric draw = new InvocationRubric(
                drawId,
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Circle",
                "draw",
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null,
                "c1",
                null,
                "Shape",
                null);
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "polymorphic draw",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(),
                List.of(construct, draw),
                null);

        String input = formatter.formatInput(testcase);

        assertEquals(
                "Circle c1 = new Circle(3.14)\n((Shape) c1).draw()",
                input);
    }

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
