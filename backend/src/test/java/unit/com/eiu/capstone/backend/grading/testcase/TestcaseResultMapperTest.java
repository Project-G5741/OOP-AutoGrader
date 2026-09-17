package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        assertNull(dto.getOopPrincipleTag());
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
        assertEquals(OopPrincipleTag.Unit, dto.getOopPrincipleTag());
    }

    @Test
    void ae6WrongTagStillEmittedOnExampleDto() throws Exception {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Mis-tagged", false, OopPrincipleTag.Polymorphism);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.FAILED);
        submissionResult.setInputDisplay("new Person()");
        submissionResult.setExpectedDisplay("ok");
        submissionResult.setActualDisplay("bad");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertEquals(OopPrincipleTag.Polymorphism, dto.getOopPrincipleTag());
        assertEquals("new Person()", dto.getInput());
        String json = new ObjectMapper().writeValueAsString(dto);
        assertTrue(json.contains("\"oop_principle_tag\":\"Polymorphism\""), json);
    }

    @Test
    void ae7HiddenDtoOmitsTagInJson() throws Exception {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Hidden poly", true, OopPrincipleTag.Polymorphism);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.FAILED);
        submissionResult.setInputDisplay("secret");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertNull(dto.getOopPrincipleTag());
        String json = new ObjectMapper().writeValueAsString(dto);
        assertFalse(json.contains("oop_principle_tag"), json);
        assertFalse(json.contains("Polymorphism"), json);
    }

    @Test
    void ae8LegacyOneStepShowsUnitTag() {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Legacy", false);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.PASSED);

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertEquals(OopPrincipleTag.Unit, dto.getOopPrincipleTag());
        assertEquals("PASS", dto.getResult());
    }

    private static TestcaseRubric visibleRubric(UUID id, String name, boolean hidden) {
        return visibleRubric(id, name, hidden, OopPrincipleTag.Unit);
    }

    private static TestcaseRubric visibleRubric(UUID id, String name, boolean hidden, OopPrincipleTag tag) {
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
                List.of(assertion),
                List.of(),
                tag);
    }

    private static SubmissionTestcaseResult resultFor(UUID testcaseId, TestcaseResultStatus status) {
        Testcase testcase = new Testcase();
        SubmissionTestcaseResult result = new SubmissionTestcaseResult();
        result.setTestcase(testcase);
        result.setResult(status);
        return result;
    }
}
