package com.eiu.capstone.backend.DTO;

import java.util.UUID;

public record OopBlitzStatusView(
        int rewardedRunsUsed,
        int rewardedRunsAllowed,
        int rewardedRunsMax,
        int pointsEarnedToday,
        int dailyPointsCap,
        int submitCountToday,
        Integer todayBestScore,
        UUID activeSessionId,
        String activeSessionMode,
        Integer activeQuestionIndex,
        int totalQuestions,
        int currentPoints) {
}
