package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter.UploadWriteResult;
import com.eiu.capstone.backend.grading.GradingResultStore;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
public class UploadPersistService {

    public record PersistResult(LabSubmission submission, StudentLabProgress progress) {}

    private final GradingResultJdbcWriter jdbcWriter;
    private final GradingResultStore gradingResultStore;
    private final ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;

    public UploadPersistService(GradingResultJdbcWriter jdbcWriter,
                                GradingResultStore gradingResultStore,
                                ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore) {
        this.jdbcWriter = jdbcWriter;
        this.gradingResultStore = gradingResultStore;
        this.parsedSubmissionSnapshotStore = parsedSubmissionSnapshotStore;
    }

    public PersistResult persist(LabSubmission submission, GradingOutcome outcome) {
        if (submission == null || submission.getId() == null) {
            throw new IllegalArgumentException("submission id is required before persist");
        }
        if (submission.getUser() == null || submission.getUser().getId() == null
                || submission.getLab() == null || submission.getLab().getId() == null) {
            throw new IllegalArgumentException("submission user and lab are required");
        }
        if (outcome == null || outcome.computed() == null) {
            throw new IllegalArgumentException("grading outcome is required");
        }
        BigDecimal score = outcome.overallScore() != null ? outcome.overallScore() : BigDecimal.ZERO;
        submission.setScore(score);
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(TimeUtil.nowInVietnam());
        }
        List<SubmissionChallengeResult> challengeRows = outcome.computed().challengeResults;
        UploadWriteResult written = jdbcWriter.persistUpload(
                submission.getId(),
                submission.getUser().getId(),
                submission.getLab().getId(),
                score,
                submission.getSubmittedAt(),
                challengeRows);
        submission.setAttemptNumber(written.attemptNumber());
        parsedSubmissionSnapshotStore.save(submission.getId(), outcome.computed().snapshotsByChallengeId);
        StudentLabProgress progress = new StudentLabProgress();
        progress.setUser(submission.getUser());
        progress.setLab(submission.getLab());
        progress.setAttemptsCount(written.attemptNumber());
        progress.setLastSubmittedAt(written.lastSubmittedAt());
        progress.setHighestScore(score);
        progress.setBestSubmissionId(submission.getId());
        gradingResultStore.scheduleDetailPersist(submission.getId(), outcome.computed());
        return new PersistResult(submission, progress);
    }
}
