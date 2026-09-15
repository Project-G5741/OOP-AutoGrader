package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.ReferenceSourceDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseDryRunRequest;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader.PendingAssertionResult;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader.PendingTestcaseResult;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubricAssembler;
import com.eiu.capstone.backend.grading.testcase.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.grading.testcase.TestcaseResultMapper;
import com.eiu.capstone.backend.grading.testcase.RemoteWorkerSessionClient;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

@ExtendWith(MockitoExtension.class)
class TestcaseDryRunServiceTest {

    @Mock private TestcaseRubricService testcaseRubricService;
    @Mock private TestcaseRubricAssembler testcaseRubricAssembler;
    @Mock private JavaCompilerService javaCompilerService;

    private final TestcaseDisplayFormatter displayFormatter =
            new TestcaseDisplayFormatter(new JsonValueCoercer());
    private final PrimaryAssertionSelector primaryAssertionSelector = new PrimaryAssertionSelector();

    private UUID labId;
    private UUID challengeId;

    @BeforeEach
    void setUp() {
        labId = UUID.randomUUID();
        challengeId = UUID.randomUUID();
    }

    @Test
    void dryRun_missingTestcase_throwsUnprocessable() {
        TestcaseDryRunService service = newService(mock(TestcaseGrader.class));
        TestcaseDryRunRequest request = new TestcaseDryRunRequest(
                List.of(new ReferenceSourceDTO("Car", "public class Car {}")),
                null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dryRun(labId, challengeId, request));
        assertEquals(422, ex.getStatusCode().value());
    }

    @Test
    void dryRun_mixedCompile_returnsPreview() {
        TestcaseStructureDTO testcase = mock(TestcaseStructureDTO.class);
        TestcaseDryRunRequest request = new TestcaseDryRunRequest(
                List.of(
                        new ReferenceSourceDTO("Car", "public class Car {}"),
                        new ReferenceSourceDTO("Broken", "public class Broken {")),
                testcase);
        TestcaseRubric rubric = minimalRubric();

        when(testcaseRubricService.loadForChallenge(labId, challengeId))
                .thenReturn(new ChallengeTestcasesResponse(labId, challengeId, List.of()));
        when(testcaseRubricAssembler.assemble(challengeId, testcase)).thenReturn(rubric);

        JavaCompilerService realCompiler = new JavaCompilerService();
        realCompiler.initCompiler();
        TestcaseGrader graderSpy = mock(TestcaseGrader.class);
        PendingTestcaseResult pending = new PendingTestcaseResult(
                rubric.id(),
                TestcaseResultStatus.PASSED,
                "ok",
                "input",
                "expected",
                "actual",
                List.of(new PendingAssertionResult(
                        rubric.assertions().get(0).id(),
                        TestcaseResultStatus.PASSED,
                        null,
                        "matched")));
        when(graderSpy.gradeSingle(any(TestcaseRubric.class), any(ChallengeGradingContext.class)))
                .thenReturn(pending);

        TestcaseDryRunService wired = new TestcaseDryRunService(
                testcaseRubricService,
                testcaseRubricAssembler,
                realCompiler,
                graderSpy,
                new TestcaseResultMapper(displayFormatter, primaryAssertionSelector),
                new Semaphore(1),
                new WorkerSessionFactory(false,
                        new WorkerProcessClient("java", "missing-worker.jar"),
                        org.mockito.Mockito.mock(RemoteWorkerSessionClient.class)),
                5);

        TestcaseResultDTO result = wired.dryRun(labId, challengeId, request);
        assertEquals("PASS", result.getResult());
        org.mockito.ArgumentCaptor<ChallengeGradingContext> contextCaptor =
                org.mockito.ArgumentCaptor.forClass(ChallengeGradingContext.class);
        verify(graderSpy).gradeSingle(any(TestcaseRubric.class), contextCaptor.capture());
        assertTrue(contextCaptor.getValue().failedClassNames().contains("Broken"));
        assertFalse(contextCaptor.getValue().failedClassNames().contains("Car"));
    }

    @Test
    void dryRun_validCompile_returnsPreviewDto() {
        TestcaseStructureDTO testcase = mock(TestcaseStructureDTO.class);
        TestcaseDryRunRequest request = new TestcaseDryRunRequest(
                List.of(new ReferenceSourceDTO("Car", "public class Car {}")),
                testcase);
        TestcaseRubric rubric = minimalRubric();

        when(testcaseRubricService.loadForChallenge(labId, challengeId))
                .thenReturn(new ChallengeTestcasesResponse(labId, challengeId, List.of()));
        when(testcaseRubricAssembler.assemble(challengeId, testcase)).thenReturn(rubric);
        when(javaCompilerService.compileSources(any(), any())).thenReturn(CompileOutcome.skipped());

        TestcaseGrader graderSpy = mock(TestcaseGrader.class);
        TestcaseDryRunService wired = newService(graderSpy);

        PendingTestcaseResult pending = new PendingTestcaseResult(
                rubric.id(),
                TestcaseResultStatus.PASSED,
                "ok",
                "input",
                "expected",
                "actual",
                List.of(new PendingAssertionResult(
                        rubric.assertions().get(0).id(),
                        TestcaseResultStatus.PASSED,
                        null,
                        "matched")));
        when(graderSpy.gradeSingle(any(TestcaseRubric.class), any(ChallengeGradingContext.class)))
                .thenReturn(pending);

        TestcaseResultDTO result = wired.dryRun(labId, challengeId, request);

        verify(testcaseRubricService).loadForChallenge(labId, challengeId);
        assertEquals("preview", result.getTestcaseName());
        assertEquals("PASS", result.getResult());
        assertEquals("expected", result.getExpectedOutput());
        assertEquals("actual", result.getActualOutput());
    }

    private TestcaseDryRunService newService(TestcaseGrader grader) {
        TestcaseResultMapper mapper = new TestcaseResultMapper(displayFormatter, primaryAssertionSelector);
        return new TestcaseDryRunService(
                testcaseRubricService,
                testcaseRubricAssembler,
                javaCompilerService,
                grader,
                mapper,
                new Semaphore(1),
                new WorkerSessionFactory(false,
                        new WorkerProcessClient("java", "missing-worker.jar"),
                        org.mockito.Mockito.mock(RemoteWorkerSessionClient.class)),
                5);
    }

    private TestcaseRubric minimalRubric() {
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        InvocationRubric invocation = new InvocationRubric(
                invocationId,
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                "Car",
                null,
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null);
        AssertionRubric assertion = new AssertionRubric(
                assertionId,
                AssertionKind.RETURN_VALUE,
                invocationId,
                null,
                null,
                null,
                "null",
                ComparisonMode.EXACT,
                0);
        return new TestcaseRubric(
                UUID.randomUUID(),
                "preview",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                false,
                invocation,
                List.of(),
                List.of(assertion));
    }
}
