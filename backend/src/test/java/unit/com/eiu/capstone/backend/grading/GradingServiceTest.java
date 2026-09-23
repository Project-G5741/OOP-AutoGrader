package unit.com.eiu.capstone.backend.grading;

import com.eiu.capstone.backend.grading.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.pipeline.GradingPipeline;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

class GradingServiceTest {

    @Test
    void challengePercentageUsesThreeEqualPillars() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                true,
                BigDecimal.ZERO,
                true);

        assertEquals(new BigDecimal("66.66"), result);
    }

    @Test
    void testcaseStatusMapsToFrontendPassFailError() {
        assertEquals("PASS", LabResultAssembler.toFrontendResult(TestcaseResultStatus.PASSED));
        assertEquals("FAIL", LabResultAssembler.toFrontendResult(TestcaseResultStatus.FAILED));
        assertEquals("ERROR", LabResultAssembler.toFrontendResult(TestcaseResultStatus.ERROR));
    }

    @Test
    void gradeSubmission_withOperationalTestRows_doesNotOpenWorkerSession() {
        LabResultAssembler assembler = mock(LabResultAssembler.class);
        when(assembler.assemble(any(), any(), any(), any(), any())).thenReturn(Map.of());

        var gradingExecutor = Executors.newSingleThreadExecutor();
        try {
            GradingService service = new GradingService(
                    mock(ChallengeRepository.class),
                    mock(FieldRepository.class),
                    mock(MethodRepository.class),
                    mock(ConstructorRepository.class),
                    mock(MmdComparisonService.class),
                    gradingExecutor,
                    mock(ClassRelationRepository.class),
                    mock(GradingPipeline.class),
                    mock(TestcaseRepository.class),
                    mock(TestcaseAssertionRepository.class),
                    assembler,
                    new ParsedSubmissionSnapshotBuilder(),
                    false);

            LabSubmission submission = new LabSubmission();
            submission.setId(UUID.randomUUID());

            assertDoesNotThrow(() ->
                    service.gradeSubmission(submission, rubricWithUnitAndComposition(), List.of(), Map.of()));
        } finally {
            gradingExecutor.shutdownNow();
        }
    }

    private static LabRubricSnapshot rubricWithUnitAndComposition() {
        UUID challengeId = UUID.randomUUID();
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                null,
                null,
                null,
                null,
                "\"1\"",
                ComparisonMode.EXACT,
                0);
        ClassRubric classRubric = new ClassRubric(
                UUID.randomUUID(),
                "Animal",
                "PUBLIC",
                "CLASS",
                false,
                List.of(),
                List.of(),
                List.of());
        ChallengeRubric challenge = new ChallengeRubric(
                challengeId,
                1,
                "Animals",
                List.of(classRubric),
                List.of(),
                List.of(
                        new TestcaseRubric(
                                UUID.randomUUID(),
                                "unit",
                                TestcaseType.UNIT,
                                null,
                                1,
                                0,
                                false,
                                null,
                                List.of(),
                                List.of(assertion)),
                        new TestcaseRubric(
                                UUID.randomUUID(),
                                "composition",
                                TestcaseType.COMPOSITION,
                                null,
                                1,
                                1,
                                false,
                                null,
                                List.of(),
                                List.of(assertion))),
                true);
        return new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge));
    }
}
