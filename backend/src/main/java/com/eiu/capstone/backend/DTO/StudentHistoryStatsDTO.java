package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;

public record StudentHistoryStatsDTO(
        int labsAttempted,
        int totalSubmissions,
        BigDecimal averageScore,
        BigDecimal bestScore) {
}
