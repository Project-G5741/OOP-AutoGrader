package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;

/**
 * One student prune per transaction — keeps the {@code maxPerStudent} newest submissions
 * globally plus the highest-scoring attempt for each lab.
 */
@Service
public class SubmissionRetentionTx {

    private final int maxPerStudent;
    private final LabSubmissionRepository labSubmissionRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
    private final SubmissionDeletionService submissionDeletionService;
    private final SubmissionSidecarCleanup submissionSidecarCleanup;

    SubmissionRetentionTx(
            @Value("${app.submission.retention.max-per-student:${app.submission.retention.max-per-lab:10}}") int maxPerStudent,
            LabSubmissionRepository labSubmissionRepository,
            StudentLabProgressRepository studentLabProgressRepository,
            SubmissionDeletionService submissionDeletionService,
            SubmissionSidecarCleanup submissionSidecarCleanup) {
        this.maxPerStudent = Math.max(1, maxPerStudent);
        this.labSubmissionRepository = labSubmissionRepository;
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.submissionDeletionService = submissionDeletionService;
        this.submissionSidecarCleanup = submissionSidecarCleanup;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enforceForStudent(UUID userId) {
        List<LabSubmission> submissions =
                labSubmissionRepository.findByUser_IdWithLabOrderBySubmittedAtDesc(userId);
        List<UUID> toDelete = SubmissionRetentionKeepers.idsToPrune(submissions, maxPerStudent);
        if (!toDelete.isEmpty()) {
            submissionDeletionService.deleteSubmissionsFully(toDelete);
            for (UUID submissionId : toDelete) {
                submissionSidecarCleanup.deleteForSubmission(submissionId);
            }
        }
        Set<UUID> deleted = Set.copyOf(toDelete);
        List<LabSubmission> remaining = new ArrayList<>();
        for (LabSubmission submission : submissions) {
            if (submission.getId() != null && !deleted.contains(submission.getId())) {
                remaining.add(submission);
            }
        }
        List<UUID> toStrip = SubmissionRetentionKeepers.idsToStripDetails(remaining);
        if (!toStrip.isEmpty()) {
            submissionDeletionService.stripDetailRows(toStrip);
            for (UUID submissionId : toStrip) {
                submissionSidecarCleanup.deleteForSubmission(submissionId);
            }
        }
        if (toDelete.isEmpty() && toStrip.isEmpty()) {
            return 0;
        }
        reconcileAllProgressForStudent(userId);
        return toDelete.size() + toStrip.size();
    }

    private void reconcileAllProgressForStudent(UUID userId) {
        Set<UUID> labIds = new HashSet<>();
        for (LabSubmission submission : labSubmissionRepository.findByUser_IdWithLabOrderBySubmittedAtDesc(userId)) {
            if (submission.getLab() != null && submission.getLab().getId() != null) {
                labIds.add(submission.getLab().getId());
            }
        }
        for (StudentLabProgress progress :
                studentLabProgressRepository.findByUser_IdWithLabOrderByLastSubmittedAtDesc(userId)) {
            if (progress.getLab() != null && progress.getLab().getId() != null) {
                labIds.add(progress.getLab().getId());
            }
        }
        for (UUID labId : labIds) {
            reconcileProgress(userId, labId);
        }
    }

    private void reconcileProgress(UUID userId, UUID labId) {
        List<LabSubmission> remaining =
                labSubmissionRepository.findByUser_IdAndLab_IdWithLabOrderByAttemptNumberDesc(userId, labId);
        Optional<StudentLabProgress> progressOpt =
                studentLabProgressRepository.findByUser_IdAndLab_Id(userId, labId);
        if (remaining.isEmpty()) {
            progressOpt.ifPresent(studentLabProgressRepository::delete);
            return;
        }
        StudentLabProgress progress = progressOpt.orElseGet(() -> {
            StudentLabProgress created = new StudentLabProgress();
            created.setUser(remaining.get(0).getUser());
            created.setLab(remaining.get(0).getLab());
            return created;
        });
        progress.setAttemptsCount(remaining.size());
        progress.setFirstSubmittedAt(remaining.stream()
                .map(LabSubmission::getSubmittedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null));
        progress.setLastSubmittedAt(remaining.stream()
                .map(LabSubmission::getSubmittedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        LabSubmission best = remaining.stream()
                .max(Comparator
                        .comparing(LabSubmission::getScore, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(LabSubmission::getSubmittedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(remaining.get(0));
        BigDecimal highest = remaining.stream()
                .map(LabSubmission::getScore)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        progress.setHighestScore(highest);
        progress.setBestSubmissionId(best.getId());
        studentLabProgressRepository.save(progress);
    }
}
