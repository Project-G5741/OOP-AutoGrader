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
        assertTrue(result.feedback().contains("Missing ; on line 1"));
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
        assertTrue(result.feedback().contains("Missing ; on line 1"));
    }

    @Test
    void mixedCompile_independentTargetStillInvokes() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "new good",
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
                        "Good",
                        null,
                        List.of(),
                        "[]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of());
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
                Set.of("Bad"),
                Map.of("Bad", "ERROR: line 1: ';' expected"));

        com.eiu.capstone.backend.grading.testcase.InvocationRunner runner =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.InvocationRunner.class);
        org.mockito.Mockito.when(runner.invokeSingle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.eiu.capstone.backend.grading.testcase.InvocationOutcome.normal(null, null, ""));
        com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter formatter =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter.class);
        org.mockito.Mockito.when(formatter.formatInput(org.mockito.ArgumentMatchers.any())).thenReturn("in");
        com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector selector =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector.class);
        org.mockito.Mockito.when(selector.select(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        TestcaseGrader.PendingTestcaseResult result =
                new TestcaseGrader(runner, null, selector, formatter).gradeSingle(testcase, context);

        org.mockito.Mockito.verify(runner).invokeSingle(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertTrue(result.feedback() == null || !result.feedback().startsWith("Compilation error:"));
    }
}
