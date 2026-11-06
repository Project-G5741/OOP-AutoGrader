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
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.service.DisclosureMode;
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
    void hiddenTestcaseIncludesIoFieldsForLecturer() {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Hidden case", true);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.FAILED);
        submissionResult.setInputDisplay("input");
        submissionResult.setExpectedDisplay("expected");
        submissionResult.setActualDisplay("actual");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult),
                DisclosureMode.LECTURER).get(0);

        assertEquals(true, dto.getHidden());
        assertEquals("FAIL", dto.getResult());
        assertEquals("", dto.getInput());
        assertEquals("expected", dto.getExpectedOutput());
        assertEquals("actual", dto.getActualOutput());
    }

    @Test
    void visibleTestcaseMapsDisplayColumns() {
        UUID invocationId = UUID.randomUUID();
        InvocationRubric invocation = new InvocationRubric(
                invocationId,
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Account",
                "deposit",
                List.of(),
                "[50]",
                null,
                null,
                List.of(),
                null,
                "account",
                null,
                null,
                null);
        TestcaseRubric rubric = visibleRubricWithInvocation(UUID.randomUUID(), "Visible case", false, invocation);
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
        assertNull(dto.getOopPrincipleTag());
    }

    @Test
    void ae6WrongTagOmittedOnExampleDto() throws Exception {
        UUID invocationId = UUID.randomUUID();
        InvocationRubric invocation = new InvocationRubric(
                invocationId,
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                "Person",
                null,
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null);
        TestcaseRubric rubric = visibleRubricWithInvocation(
                UUID.randomUUID(), "Mis-tagged", false, OopPrincipleTag.Polymorphism, invocation);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.FAILED);
        submissionResult.setInputDisplay("new Person()");
        submissionResult.setExpectedDisplay("ok");
        submissionResult.setActualDisplay("bad");

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertNull(dto.getOopPrincipleTag());
        assertEquals("new Person()", dto.getInput());
        String json = new ObjectMapper().writeValueAsString(dto);
        assertFalse(json.contains("oop_principle_tag"), json);
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
    void ae8LegacyOneStepOmitsStudentTag() {
        TestcaseRubric rubric = visibleRubric(UUID.randomUUID(), "Legacy", false);
        SubmissionTestcaseResult submissionResult = resultFor(rubric.id(), TestcaseResultStatus.PASSED);

        TestcaseResultDTO dto = mapper.mapChallengeTestcases(
                List.of(rubric),
                Map.of(rubric.id(), submissionResult)).get(0);

        assertNull(dto.getOopPrincipleTag());
        assertEquals("PASS", dto.getResult());
    }

    private static TestcaseRubric visibleRubric(UUID id, String name, boolean hidden) {
        return visibleRubric(id, name, hidden, OopPrincipleTag.Unit);
    }

    private static TestcaseRubric visibleRubric(UUID id, String name, boolean hidden, OopPrincipleTag tag) {
        return visibleRubricWithInvocation(id, name, hidden, tag, null);
    }

    private static TestcaseRubric visibleRubricWithInvocation(UUID id,
                                                              String name,
                                                              boolean hidden,
                                                              InvocationRubric invocation) {
        return visibleRubricWithInvocation(id, name, hidden, OopPrincipleTag.Unit, invocation);
    }

    private static TestcaseRubric visibleRubricWithInvocation(UUID id,
                                                              String name,
                                                              boolean hidden,
                                                              OopPrincipleTag tag,
                                                              InvocationRubric invocation) {
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                invocation != null ? invocation.id() : null,
                null,
                null,
                null,
                "\"150\"",
                ComparisonMode.EXACT,
                0);
        List<InvocationRubric> invocations = invocation != null ? List.of(invocation) : List.of();
        return new TestcaseRubric(
                id,
                name,
                TestcaseType.UNIT,
                null,
                1,
                0,
                hidden,
                invocation,
                List.of(),
                List.of(assertion),
                invocations,
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
