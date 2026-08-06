package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;

public record SubmissionSummaryDTO(
        String studentName,
        String studentCode,
        BigDecimal score,
        int attempt,
        String submittedAt,
        boolean bestSubmission) {
}
