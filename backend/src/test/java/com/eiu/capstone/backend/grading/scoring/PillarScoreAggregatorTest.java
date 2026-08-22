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

    @Test
    void challengePercentage_classAndMmdWeights() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100), 3,
                BigDecimal.valueOf(40), true, 1,
                BigDecimal.ZERO, false, 1);
        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    void weightedLabPercentage_prefersHeavierChallenge() {
        BigDecimal result = PillarScoreAggregator.weightedLabPercentage(List.of(
                new PillarScoreAggregator.WeightedPercentage(3, BigDecimal.valueOf(100)),
                new PillarScoreAggregator.WeightedPercentage(1, BigDecimal.ZERO)));
        assertEquals(new BigDecimal("75.00"), result);
    }

    @Test
    void challengePercentage_threePillarsUnevenWeights() {
        // (100*2 + 50*1 + 0*1) / 4 = 62.50
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(100), 2,
                BigDecimal.valueOf(50), true, 1,
                BigDecimal.ZERO, true, 1);
        assertEquals(new BigDecimal("62.50"), result);
    }

    @Test
    void challengePercentage_heavyMmdPullsScoreTowardMmd() {
        // (0*1 + 100*4) / 5 = 80.00
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.ZERO, 1,
                BigDecimal.valueOf(100), true, 4,
                BigDecimal.ZERO, false, 1);
        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    void challengePercentage_zeroWeightsNormalizeToOne() {
        // (80*1 + 20*1) / 2 = 50.00
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.valueOf(80), 0,
                BigDecimal.valueOf(20), true, 0,
                BigDecimal.ZERO, false, 0);
        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void challengePercentage_heavyTestcasePullsScoreTowardTestcase() {
        // (0*1 + 100*4) / 5 = 80.00
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                BigDecimal.ZERO, 1,
                BigDecimal.ZERO, false, 1,
                BigDecimal.valueOf(100), true, 4);
        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    void pillarPercentage_heavierClassShellDominatesMembers() {
        // (1.0*4 + 0.0*1) / 5 * 100 = 80.00
        BigDecimal result = PillarScoreAggregator.pillarPercentage(List.of(
                new PillarScoreAggregator.WeightedAccuracy(4, 1.0),
                new PillarScoreAggregator.WeightedAccuracy(1, 0.0)));
        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    void weightedLabPercentage_threeChallengesDifferentWeights() {
        // (100*1 + 50*2 + 0*3) / 6 = 33.33
        BigDecimal result = PillarScoreAggregator.weightedLabPercentage(List.of(
                new PillarScoreAggregator.WeightedPercentage(1, BigDecimal.valueOf(100)),
                new PillarScoreAggregator.WeightedPercentage(2, BigDecimal.valueOf(50)),
                new PillarScoreAggregator.WeightedPercentage(3, BigDecimal.ZERO)));
        assertEquals(new BigDecimal("33.33"), result);
    }

    @Test
    void weightedLabPercentage_equalScoresStayEqualRegardlessOfWeight() {
        BigDecimal equal = PillarScoreAggregator.weightedLabPercentage(List.of(
                new PillarScoreAggregator.WeightedPercentage(1, BigDecimal.valueOf(70)),
                new PillarScoreAggregator.WeightedPercentage(1, BigDecimal.valueOf(70))));
        BigDecimal uneven = PillarScoreAggregator.weightedLabPercentage(List.of(
                new PillarScoreAggregator.WeightedPercentage(5, BigDecimal.valueOf(70)),
                new PillarScoreAggregator.WeightedPercentage(1, BigDecimal.valueOf(70))));
        assertEquals(new BigDecimal("70.00"), equal);
        assertEquals(new BigDecimal("70.00"), uneven);
    }
}
