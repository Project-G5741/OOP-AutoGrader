package unit.com.eiu.capstone.backend.grading.pipeline;

import com.eiu.capstone.backend.grading.pipeline.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

class TestcaseGraderTest {

    @Test
    void emptyTestcaseRowsScoreZeroPercent() {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of());

        ChallengeGradingContext context = ChallengeGradingContext.of(rubric, null, null, List.of());
        TestcaseGrader grader = new TestcaseGrader(null, null, null, null);

        TestcaseGrader.TestcasePillarResult result = grader.grade(context);

        assertEquals(0, result.pillarPercentage().compareTo(BigDecimal.ZERO));
        assertTrue(result.results().isEmpty());
    }

    @Test
    void mixedCompile_errorsWhenTargetClassFailed() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "new student",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                false,
                new InvocationRubric(
                        invocationId,
                        InvocationKind.CONSTRUCTOR,
                        UUID.randomUUID(),
                        null,
                        "Student",
                        null,
                        List.of(),
                        "[]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of(new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        invocationId,
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of(testcase));
        ChallengeGradingContext context = ChallengeGradingContext.of(
                rubric,
                null,
                null,
                List.of(),
                Set.of("Student"),
                Map.of("Student", "ERROR: line 1: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("ERROR: line 1"));
    }

    @Test
    void mixedCompile_errorsWhenParamTypeFailed() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "deposit",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                false,
                new InvocationRubric(
                        invocationId,
                        InvocationKind.METHOD,
                        null,
                        UUID.randomUUID(),
                        "BankAccount",
                        "setOwner",
                        List.of("Student"),
                        "[null]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of(new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        invocationId,
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of(testcase));
        ChallengeGradingContext context = ChallengeGradingContext.of(
                rubric,
                null,
                null,
                List.of(),
                Set.of("Student"),
                Map.of("Student", "ERROR: line 1: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("Compilation Error on Student")
                || result.feedback().contains("ERROR: line 1"));
    }
}
