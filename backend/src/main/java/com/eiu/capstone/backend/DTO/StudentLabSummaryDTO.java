package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentLabSummaryDTO(
        UUID id,
        String name,
        BigDecimal bestScore,
        int attempts,
        String lastSubmittedAt) {
}
