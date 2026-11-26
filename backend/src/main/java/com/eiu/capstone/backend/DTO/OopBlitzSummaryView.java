package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzSummaryView(
        String mode,
        int totalQuestions,
        int correctCount,
        int maxCombo,
        int speedBonusTotal,
        int rawPoints,
        int creditedPoints,
        boolean cappedByDailyLimit,
        int newWalletPoints,
        List<OopBlitzReviewItemView> review) {
}
