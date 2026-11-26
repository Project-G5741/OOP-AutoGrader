package com.eiu.capstone.backend.DTO;

import java.util.Map;

public record OopBlitzBankStatsView(
        int proceduralTemplateCount,
        int estimatedProceduralVariants,
        int customTotal,
        int customActive,
        Map<String, Integer> proceduralByKind,
        Map<String, Integer> customByDifficulty) {
}
