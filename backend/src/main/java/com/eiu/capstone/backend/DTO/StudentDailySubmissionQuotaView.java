package com.eiu.capstone.backend.DTO;

public record StudentDailySubmissionQuotaView(
        int usedToday,
        int dailyLimit,
        int remaining,
        boolean canSubmit) {
}
