package support.com.eiu.capstone.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.eiu.capstone.backend.DTO.SubmissionUploadResponse;
import com.eiu.capstone.backend.controller.SubmissionController;
import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.grading.GradedChallengeSummary;
import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.plagiarism.PlagiarismSignals;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.MmdPersistenceHook;
import com.eiu.capstone.backend.service.StudentHistoryService;
import com.eiu.capstone.backend.service.StudentStreakService;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.SubmissionCompileErrorStore;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore;
import com.eiu.capstone.backend.service.SubmissionPackageNormalizationStore;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.service.UploadPersistService;

@ExtendWith(MockitoExtension.class)
class AdminSubmissionDiscardTest {

    @Mock private JwtAuthHelper jwtAuthHelper;
    @Mock private SubmissionStorageService submissionStorageService;
    @Mock private LabRepository labRepository;
    @Mock private GradingService gradingService;
    @Mock private LabRubricCache labRubricCache;
    @Mock private MmdPersistenceHook mmdPersistenceHook;
    @Mock private SubmissionCompileErrorStore compileErrorStore;
    @Mock private SubmissionPackageNormalizationStore packageNormalizationStore;
    @Mock private SubmissionMmdMetaStore submissionMmdMetaStore;
    @Mock private StudentHistoryService studentHistoryService;
    @Mock private LabStatisticsCache labStatisticsCache;
    @Mock private LecturerOverviewCache lecturerOverviewCache;
    @Mock private PlagiarismService plagiarismService;
    @Mock private StudentTermAccessService studentTermAccessService;
    @Mock private UploadPersistService uploadPersistService;
    @Mock private StudentStreakService studentStreakService;

    private SubmissionController controller;
    private ExecutorService inline;

    @BeforeEach
    void setUp() {
        inline = mock(ExecutorService.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(inline).execute(any());
        controller = new SubmissionController(
                jwtAuthHelper,
                submissionStorageService,
                labRepository,
                gradingService,
                labRubricCache,
                mmdPersistenceHook,
                compileErrorStore,
                packageNormalizationStore,
                submissionMmdMetaStore,
                studentHistoryService,
                labStatisticsCache,
                lecturerOverviewCache,
                plagiarismService,
                studentTermAccessService,
                uploadPersistService,
                studentStreakService,
                inline,
                inline,
                false,
                null);
    }

    @Test
    void adminUploadReturnsGradeAndSkipsSavedResults() throws Exception {
        UUID labId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setEmail("admin@eiu.edu.vn");
        user.setTeacherCode("T100");
        Lab lab = new Lab();
        Path folder = Files.createTempDirectory("admin-discard");
        when(studentTermAccessService.requireUploadAccess("admin@eiu.edu.vn", labId))
                .thenReturn(new StudentTermAccessService.UploadAccess(user, lab));
        when(submissionStorageService.processUpload(any(), any(), any()))
                .thenReturn(new SubmissionStorageService.ProcessResult(folder, List.of(), Map.of(), 0L));
        when(labRubricCache.get(lab)).thenReturn(null);
        when(gradingService.gradeSubmission(any(), any(), any(), any()))
                .thenReturn(new GradingOutcome(
                        new BigDecimal("80.00"),
                        List.of(new GradedChallengeSummary(challengeId, 80)),
                        Map.of(),
                        Map.of(),
                        null));

        JwtUserPrincipal principal = new JwtUserPrincipal("admin@eiu.edu.vn", "T100", List.of("ADMIN"));
        ResponseEntity<SubmissionUploadResponse> response = controller.upload(
                principal,
                labId,
                1,
                List.of(new MockMultipartFile("files", "A.java", "text/plain", "class A {}".getBytes())));

        assertEquals(new BigDecimal("80.00"), response.getBody().getScore());
        assertEquals(80, response.getBody().getChallengeResult().get(challengeId));
        verify(uploadPersistService, never()).persist(any(), any());
        verify(studentStreakService, never()).recordSubmission(any());
        verify(plagiarismService, never()).inspectUpload(any(), any(PlagiarismSignals.class));
        verify(labStatisticsCache, never()).invalidate(any());
        verify(lecturerOverviewCache, never()).invalidate();
        verify(mmdPersistenceHook, never()).onUploadComplete(any(), any(), any());
        verify(submissionStorageService).deleteFolder(folder);
    }
}
