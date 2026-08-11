package com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

class TestcaseResultMapperTest {

    private final TestcaseResultMapper mapper = new TestcaseResultMapper(
            new TestcaseDisplayFormatter(new JsonValueCoercer()),
            new PrimaryAssertionSelector());

    @Test
    void hiddenTestcaseOmitsIoFields() {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Hidden case", true);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.FAILED);
        submissionResult.setInputDisplay("input");
        submissionResult.setExpectedDisplay("expected");
        submissionResult.setActualDisplay("actual");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertEquals(true, dto.getHidden());
        assertEquals("FAIL", dto.getResult());
        assertNull(dto.getInput());
        assertNull(dto.getExpectedOutput());
        assertNull(dto.getActualOutput());
        assertNull(dto.getAssertions());
        assertNull(dto.getFeedback());
    }

    @Test
    void visibleTestcaseMapsDisplayColumns() {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Visible case", false);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.PASSED);
        submissionResult.setInputDisplay("account.deposit(50)");
        submissionResult.setExpectedDisplay("150");
        submissionResult.setActualDisplay("150");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertEquals(false, dto.getHidden());
        assertEquals("PASS", dto.getResult());
        assertEquals("account.deposit(50)", dto.getInput());
        assertEquals("150", dto.getExpectedOutput());
        assertEquals("150", dto.getActualOutput());
    }

    private static TestcaseRubric visibleRubric(UUID id, String name, boolean hidden) {
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                null,
                null,
                null,
                null,
                "\"150\"",
                ComparisonMode.EXACT,
                0);
        return new TestcaseRubric(
                id,
                name,
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                hidden,
                null,
                List.of(),
                List.of(assertion));
    }

    private static SubmissionTestcaseResult resultFor(UUID testcaseId, TestcaseResultStatus status) {
        Testcase testcase = new Testcase();
        SubmissionTestcaseResult result = new SubmissionTestcaseResult();
        result.setTestcase(testcase);
        result.setResult(status);
        return result;
    }
}
