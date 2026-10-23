package com.eiu.capstone.backend.analytics.dto;

import java.time.Instant;
import java.util.List;

public record WeeklyRankingBoardResponse(
        Instant weekStart,
        Instant weekEnd,
        List<WeeklyRankingRow> rows) {
}
