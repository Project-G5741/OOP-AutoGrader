package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentSubmissionHistoryItemDTO(
        UUID id,
        StudentLabRefDTO lab,
        int attemptNumber,
        BigDecimal score,
        String submittedAt,
        String status,
        List<StudentChallengeResultDTO> challengeResults) {
}
