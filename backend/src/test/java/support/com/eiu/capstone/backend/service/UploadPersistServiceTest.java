package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter.UploadWriteResult;
import com.eiu.capstone.backend.grading.GradingResultStore;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.utility.TimeUtil;

@ExtendWith(MockitoExtension.class)
class UploadPersistServiceTest {

    @Mock
    private GradingResultJdbcWriter jdbcWriter;
    @Mock
    private GradingResultStore gradingResultStore;
    @Mock
    private ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;

    private UploadPersistService persistService;

    @BeforeEach
    void setUp() {
        persistService = new UploadPersistService(
                jdbcWriter,
                gradingResultStore,
                parsedSubmissionSnapshotStore);
    }

    @Test
    void persist_writesOneSqlThenSnapshotThenSchedulesDetails() {
        UUID submissionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        setId(user, userId);
        Lab lab = new Lab();
        setId(lab, labId);

        LabSubmission submission = new LabSubmission();
        submission.setId(submissionId);
        submission.setUser(user);
        submission.setLab(lab);
        submission.setScore(BigDecimal.ZERO);

        GradingService.GradingComputationResult computed = new GradingService.GradingComputationResult();
        computed.challengeResults = List.of();
        computed.snapshotsByChallengeId = Map.of();
        GradingOutcome outcome = new GradingOutcome(
                new BigDecimal("81.25"), List.of(), Map.of(), Map.of(), computed);

        OffsetDateTime submittedAt = TimeUtil.nowInVietnam();
        when(jdbcWriter.persistUpload(eq(submissionId), eq(userId), eq(labId),
                eq(new BigDecimal("81.25")), any(), eq(List.of())))
                .thenAnswer(invocation -> {
                    submission.setSubmittedAt(invocation.getArgument(4));
                    return new UploadWriteResult(2, submittedAt);
                });

        UploadPersistService.PersistResult result = persistService.persist(submission, outcome);

        assertEquals(new BigDecimal("81.25"), result.submission().getScore());
        assertEquals(2, result.submission().getAttemptNumber());
        assertEquals(2, result.progress().getAttemptsCount());
        InOrder order = inOrder(jdbcWriter, parsedSubmissionSnapshotStore, gradingResultStore);
        order.verify(jdbcWriter).persistUpload(
                eq(submissionId), eq(userId), eq(labId),
                eq(new BigDecimal("81.25")), any(), eq(List.of()));
        order.verify(parsedSubmissionSnapshotStore).save(eq(submissionId), eq(Map.of()));
        order.verify(gradingResultStore).scheduleDetailPersist(submissionId, computed);
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
