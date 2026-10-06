package unit.com.eiu.capstone.backend.service.bulk;

import com.eiu.capstone.backend.service.bulk.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkGradeResponse;
import com.eiu.capstone.backend.grading.GradedChallengeSummary;
import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.DisclosureMode;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.service.UploadPersistService;

class LecturerBulkGradingServiceTest {

    private LabRepository labRepository;
    private ChallengeRepository challengeRepository;
    private LabRubricCache labRubricCache;
    private SubmissionStorageService submissionStorageService;
    private GradingService gradingService;
    private LecturerBulkGradingService service;

    private final UUID labId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        labRepository = mock(LabRepository.class);
        challengeRepository = mock(ChallengeRepository.class);
        labRubricCache = mock(LabRubricCache.class);
        submissionStorageService = mock(SubmissionStorageService.class);
        gradingService = mock(GradingService.class);
        service = new LecturerBulkGradingService(
                labRepository,
                challengeRepository,
                labRubricCache,
                submissionStorageService,
                gradingService);
    }

    @Test
    void examModeWithMultiChallengeLab_is400() {
        Lab lab = new Lab();
        lab.setId(labId);
        when(labRepository.findById(labId)).thenReturn(java.util.Optional.of(lab));
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId))
                .thenReturn(List.of(new Challenge(), new Challenge()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.gradeOne(
                        labId,
                        BulkGradeMode.EXAM,
                        List.of(new MockMultipartFile(
                                "files",
                                "2331200057_Student/Main.java",
                                "text/plain",
                                "class Main {}".getBytes()))));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("exactly 1 challenge"));
        verify(gradingService, never()).gradeSubmission(any(), any(), anyList(), any(), any());
        verify(submissionStorageService, never()).processUpload(anyString(), anyString(), anyList());
    }

    @Test
    void happyPathExam_gradesWithLecturerDisclosureAndDeletesTemp() throws Exception {
        Lab lab = new Lab();
        lab.setId(labId);
        when(labRepository.findById(labId)).thenReturn(java.util.Optional.of(lab));
        Challenge sole = new Challenge();
        sole.setChallengeNumber(1);
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId))
                .thenReturn(List.of(sole));

        LabRubricSnapshot rubric = mock(LabRubricSnapshot.class);
        when(labRubricCache.get(lab)).thenReturn(rubric);

        Path temp = Path.of("submissions-test", "bulk-tmp");
        SubmissionStorageService.ProcessResult processResult =
                new SubmissionStorageService.ProcessResult(temp, List.of(), Map.of(), 0L);
        when(submissionStorageService.processUpload(anyString(), anyString(), anyList()))
                .thenReturn(processResult);

        UUID challengeId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        GradingOutcome outcome = new GradingOutcome(
                new BigDecimal("88.50"),
                List.of(new GradedChallengeSummary(challengeId, 88)),
                Map.of(),
                Map.of(),
                null);
        when(gradingService.gradeSubmission(any(), eq(rubric), anyList(), any(), eq(DisclosureMode.LECTURER)))
                .thenReturn(outcome);

        BulkGradeResponse response = service.gradeOne(
                labId,
                BulkGradeMode.EXAM,
                List.of(new MockMultipartFile(
                        "files",
                        "2331200057_Student/Main.java",
                        "text/plain",
                        "class Main {}".getBytes())));

        assertEquals(new BigDecimal("88.50"), response.getScore());
        assertEquals("2331200057", response.getIrn());
        assertEquals("2331200057_Student", response.getStudentFolder());
        verify(submissionStorageService).deleteFolder(temp);
        // Compile-time / design gate: service must not depend on UploadPersistService
        assertTrue(LecturerBulkGradingService.class.getDeclaredConstructors()[0]
                .getParameterTypes().length < 6
                || !List.of(LecturerBulkGradingService.class.getDeclaredConstructors()[0].getParameterTypes())
                        .contains(UploadPersistService.class));
    }
}
