package com.eiu.capstone.backend.grading.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PillarScoreAggregatorTest {

    @Test
    void challengePercentage_bothApplicable_averagesThreePillars() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), true,
                BigDecimal.ZERO, true);
        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void challengePercentage_mmdNotApplicable_averagesClassAndTestcase() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(999), false,
                BigDecimal.valueOf(50), true);
        assertEquals(new BigDecimal("75.00"), result);
    }

    @Test
    void challengePercentage_testcaseNotApplicable_averagesClassAndMmd() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), true,
                BigDecimal.valueOf(999), false);
        assertEquals(new BigDecimal("75.00"), result);
    }

    @Test
    void challengePercentage_onlyClassApplicable_equalsClassPillar() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(999), false,
                BigDecimal.valueOf(999), false);
        assertEquals(new BigDecimal("80.00"), result);
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
