package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LabAttemptHistoryItemDTO(
        int attemptNumber,
        BigDecimal score,
        String submittedAt,
        UUID submissionId,
        boolean bestSubmission) {
}
