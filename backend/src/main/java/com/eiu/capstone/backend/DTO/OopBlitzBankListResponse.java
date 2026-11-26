package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzBankListResponse(
        OopBlitzBankStatsView stats,
        List<OopBlitzBankQuestionView> questions) {
}
