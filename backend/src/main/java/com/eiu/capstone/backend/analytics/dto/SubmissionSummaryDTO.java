package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmissionSummaryDTO(
        UUID studentId,
        String studentName,
        String studentCode,
        BigDecimal score,
        int attempt,
        String submittedAt,
        boolean bestSubmission,
        UUID submissionId,
        boolean hasSubmission,
        boolean plagiarismFlagged) {

    public SubmissionSummaryDTO(UUID studentId,
                                String studentName,
                                String studentCode,
                                BigDecimal score,
                                int attempt,
                                String submittedAt,
                                boolean bestSubmission,
                                UUID submissionId,
                                boolean hasSubmission) {
        this(studentId, studentName, studentCode, score, attempt, submittedAt,
                bestSubmission, submissionId, hasSubmission, false);
    }
}
