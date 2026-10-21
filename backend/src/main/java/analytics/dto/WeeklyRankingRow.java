package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WeeklyRankingRow(
        int rank,
        UUID userId,
        String fullName,
        String irn,
        BigDecimal total,
        int giftPoints) {
}
