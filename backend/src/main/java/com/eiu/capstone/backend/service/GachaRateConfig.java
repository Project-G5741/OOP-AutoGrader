package com.eiu.capstone.backend.service;

/**
 * Singleton row {@code gacha_rate_config.id = 1}. Edit via operator SQL; students only read.
 */
public record GachaRateConfig(
        int costSinglePoints,
        int costMultiPoints,
        int hardPityLimit,
        int softPityStart,
        double directLegendaryRate,
        double crystalBaseRate,
        double randomMascotRate,
        double cashbackRate300,
        double cashbackRate150,
        double cashbackRate60,
        double cashbackRate20) {

    /** Point-paid rolls below soft pity: 1 crystal per 10,000 rolls. */
    public static final double POINT_CRYSTAL_RATE_BELOW_SOFT = 1.0 / 10_000;

    /** Point soft pity ramp caps below 100% so roll 100 is never a forced crystal. */
    public static final double POINT_CRYSTAL_RATE_SOFT_CAP = 0.99;

    public double softPityRateStep() {
        if (hardPityLimit <= softPityStart) {
            return 1.0;
        }
        return (1.0 - crystalBaseRate) / (hardPityLimit - softPityStart);
    }

    public double pointsSoftPityRateStep() {
        if (hardPityLimit <= softPityStart) {
            return POINT_CRYSTAL_RATE_SOFT_CAP;
        }
        return (POINT_CRYSTAL_RATE_SOFT_CAP - POINT_CRYSTAL_RATE_BELOW_SOFT)
                / (hardPityLimit - softPityStart);
    }

    public double crystalRateForPointRoll(int rollNumber) {
        if (rollNumber < softPityStart()) {
            return POINT_CRYSTAL_RATE_BELOW_SOFT;
        }
        return Math.min(
                POINT_CRYSTAL_RATE_SOFT_CAP,
                POINT_CRYSTAL_RATE_BELOW_SOFT
                        + (rollNumber - softPityStart()) * pointsSoftPityRateStep());
    }

    public int pointCost(int rollCount) {
        return rollCount == 1 ? costSinglePoints : costMultiPoints;
    }
}