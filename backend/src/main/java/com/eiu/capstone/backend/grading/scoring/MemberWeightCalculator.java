package com.eiu.capstone.backend.grading.scoring;

/**
 * Assigns unit weights to rubric members for proportional scoring within a pillar.
 * Each class shell, field, method, and constructor counts as weight 1 unless overridden.
 */
public final class MemberWeightCalculator {

    private MemberWeightCalculator() {}

    public static int defaultMemberWeight() {
        return 1;
    }

    public static int testcaseWeight(int configuredWeight) {
        return configuredWeight > 0 ? configuredWeight : 1;
    }
}
