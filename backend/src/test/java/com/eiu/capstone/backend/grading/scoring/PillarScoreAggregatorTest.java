package com.eiu.capstone.backend.grading.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PillarScoreAggregatorTest {

    @Test
    void challengePercentage_averagesThreePillars() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                BigDecimal.ZERO);
        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void pillarPercentage_weightedMemberAccuracies() {
        BigDecimal result = PillarScoreAggregator.pillarPercentage(List.of(
                new PillarScoreAggregator.WeightedAccuracy(1, 1.0),
                new PillarScoreAggregator.WeightedAccuracy(1, 0.5)));
        assertEquals(new BigDecimal("75.00"), result);
    }

    @Test
    void labPercentage_missingChallengeCountsAsZeroInList() {
        BigDecimal result = PillarScoreAggregator.labPercentage(List.of(
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                BigDecimal.valueOf(60),
                BigDecimal.ZERO,
                BigDecimal.valueOf(80)));
        assertEquals(new BigDecimal("48.00"), result);
    }

    @Test
    void pillarPercentage_emptyMembersReturnsZero() {
        assertEquals(0, PillarScoreAggregator.pillarPercentage(List.of()).compareTo(BigDecimal.ZERO));
    }
}
