package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.repository.LabSubmissionRepository;

@Service
public class SubmissionRetentionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRetentionService.class);

    private final boolean enabled;
    private final int maxPerStudent;
    private final int nightlyBatchStudents;
    private final LabSubmissionRepository labSubmissionRepository;
    private final SubmissionRetentionTx submissionRetentionTx;

    public SubmissionRetentionService(
            @Value("${app.submission.retention.enabled:true}") boolean enabled,
            @Value("${app.submission.retention.max-per-student:${app.submission.retention.max-per-lab:10}}") int maxPerStudent,
            @Value("${app.submission.retention.nightly-batch-pairs:200}") int nightlyBatchStudents,
            LabSubmissionRepository labSubmissionRepository,
            SubmissionRetentionTx submissionRetentionTx) {
        this.enabled = enabled;
        this.maxPerStudent = Math.max(1, maxPerStudent);
        this.nightlyBatchStudents = Math.max(1, nightlyBatchStudents);
        this.labSubmissionRepository = labSubmissionRepository;
        this.submissionRetentionTx = submissionRetentionTx;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return number of submissions deleted
     */
    public int enforceForStudent(UUID userId) {
        if (!enabled || userId == null) {
            return 0;
        }
        return submissionRetentionTx.enforceForStudent(userId);
    }

    /**
     * @return number of students processed in this batch
     */
    public int enforceAllOverCap() {
        if (!enabled) {
            return 0;
        }
        List<UUID> students = labSubmissionRepository.findStudentsOverRetentionCap(
                maxPerStudent, nightlyBatchStudents);
        int studentsProcessed = 0;
        int submissionsDeleted = 0;
        for (UUID userId : students) {
            try {
                int deleted = submissionRetentionTx.enforceForStudent(userId);
                studentsProcessed++;
                submissionsDeleted += deleted;
            } catch (RuntimeException ex) {
                log.warn("Submission retention skipped user={}: {}", userId, ex.getMessage());
            }
        }
        if (submissionsDeleted > 0) {
            log.info("Submission retention batch: {} student(s), {} submission row(s) deleted",
                    studentsProcessed, submissionsDeleted);
        }
        return studentsProcessed;
    }

    /**
     * @return total number of students processed across all batches
     */
    public int pruneAllExistingOverCap() {
        if (!enabled) {
            return 0;
        }
        int totalStudents = 0;
        int batch;
        while ((batch = enforceAllOverCap()) > 0) {
            totalStudents += batch;
        }
        return totalStudents;
    }
}
