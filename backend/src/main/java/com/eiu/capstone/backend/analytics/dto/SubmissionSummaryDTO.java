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
        boolean plagiarismFlagged,
        String plagiarismRole,
        BigDecimal mmdScore,
        BigDecimal declarationScore,
        BigDecimal operationalScore) {

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
                bestSubmission, submissionId, hasSubmission, false, null, null, null, null);
    }

    public SubmissionSummaryDTO(UUID studentId,
                                String studentName,
                                String studentCode,
                                BigDecimal score,
                                int attempt,
                                String submittedAt,
                                boolean bestSubmission,
                                UUID submissionId,
                                boolean hasSubmission,
                                boolean plagiarismFlagged) {
        this(studentId, studentName, studentCode, score, attempt, submittedAt,
                bestSubmission, submissionId, hasSubmission, plagiarismFlagged, null, null, null, null);
    }
}
