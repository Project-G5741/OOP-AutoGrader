package com.eiu.capstone.backend.grading.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Aggregates weighted member accuracies into pillar and challenge percentages.
 *
 * <p>Challenge score = arithmetic mean of three pillar percentages (class, mmd, testcase).
 * Lab score = average across all rubric challenges; missing challenges count as 0.
 */
public final class PillarScoreAggregator {

    private static final int SCALE = 2;

    private PillarScoreAggregator() {}

    /**
     * Weighted pillar percentage: sum(weight * accuracy) / sum(weight) * 100.
     * Returns 0 when there are no weighted members.
     */
    public static BigDecimal pillarPercentage(List<WeightedAccuracy> members) {
        if (members == null || members.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double weightSum = 0;
        double earned = 0;
        for (WeightedAccuracy member : members) {
            int w = Math.max(1, member.weight());
            weightSum += w;
            earned += w * clamp(member.accuracy());
        }
        if (weightSum <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(earned / weightSum * 100.0)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Mean of three pillar percentages (equal weight per pillar).
     */
    public static BigDecimal challengePercentage(BigDecimal classPct,
                                                BigDecimal mmdPct,
                                                BigDecimal testcasePct) {
        BigDecimal sum = safe(classPct).add(safe(mmdPct)).add(safe(testcasePct));
        return sum.divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Lab-level average across challenge percentages; empty list yields 0.
     */
    public static BigDecimal labPercentage(List<BigDecimal> challengePercentages) {
        if (challengePercentages == null || challengePercentages.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal pct : challengePercentages) {
            sum = sum.add(safe(pct));
        }
        return sum.divide(BigDecimal.valueOf(challengePercentages.size()), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static double clamp(double accuracy) {
        if (accuracy < 0) {
            return 0;
        }
        if (accuracy > 1) {
            return 1;
        }
        return accuracy;
    }

    public record WeightedAccuracy(int weight, double accuracy) {}
}
