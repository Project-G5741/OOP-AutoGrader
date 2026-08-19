package com.eiu.capstone.backend.grading.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Aggregates weighted member accuracies into pillar and challenge percentages.
 *
 * <p>Challenge score = weighted mean of the pillars applicable to that challenge. The class
 * (Declaration Test) pillar is always applicable; the mmd and testcase pillars are each
 * conditionally applicable (mmd when the challenge requires an MMD diagram, testcase when the
 * challenge has at least one operational testcase). When only class is applicable, the challenge
 * score equals the class pillar percentage. Default pillar weights are 1 (equal mean).
 *
 * <p>Lab score = weighted mean across all rubric challenges; missing challenges count as 0.
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
     * Mean of the applicable pillar percentages. Class (Declaration Test) is always applicable;
     * mmd and testcase are included only when their respective flags are true. When both flags
     * are false, the result equals {@code classPct} alone.
     */
    public static BigDecimal challengePercentage(BigDecimal classPct,
                                                BigDecimal mmdPct,
                                                boolean mmdApplicable,
                                                BigDecimal testcasePct,
                                                boolean testcaseApplicable) {
        return challengePercentage(classPct, 1, mmdPct, mmdApplicable, 1, testcasePct, testcaseApplicable, 1);
    }

    public static BigDecimal challengePercentage(BigDecimal classPct,
                                                int classWeight,
                                                BigDecimal mmdPct,
                                                boolean mmdApplicable,
                                                int mmdWeight,
                                                BigDecimal testcasePct,
                                                boolean testcaseApplicable,
                                                int testcaseWeight) {
        double weightSum = Math.max(1, classWeight);
        double earned = safe(classPct).doubleValue() * Math.max(1, classWeight);
        if (mmdApplicable) {
            int w = Math.max(1, mmdWeight);
            weightSum += w;
            earned += safe(mmdPct).doubleValue() * w;
        }
        if (testcaseApplicable) {
            int w = Math.max(1, testcaseWeight);
            weightSum += w;
            earned += safe(testcasePct).doubleValue() * w;
        }
        return BigDecimal.valueOf(earned / weightSum).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Lab-level average across equally weighted challenge percentages; empty list yields 0.
     */
    public static BigDecimal labPercentage(List<BigDecimal> challengePercentages) {
        if (challengePercentages == null || challengePercentages.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return weightedLabPercentage(challengePercentages.stream()
                .map(pct -> new WeightedPercentage(1, pct))
                .toList());
    }

    /**
     * Lab-level weighted mean of challenge percentages; empty list yields 0.
     */
    public static BigDecimal weightedLabPercentage(List<WeightedPercentage> parts) {
        if (parts == null || parts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double weightSum = 0;
        double earned = 0;
        for (WeightedPercentage part : parts) {
            int w = Math.max(1, part.weight());
            weightSum += w;
            earned += w * safe(part.percentage()).doubleValue();
        }
        if (weightSum <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(earned / weightSum).setScale(SCALE, RoundingMode.HALF_UP);
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

    public record WeightedPercentage(int weight, BigDecimal percentage) {}
}
