package com.eiu.capstone.backend.service;

/**
 * Singleton row {@code gacha_rate_config.id = 1}. Managed by Admin via {@code GachaRateConfigService}; students only read.
 */
public record GachaRateConfig(
        int costSinglePoints,
        int costMultiPoints,
        int hardPityLimit,
        int softPityStart,
        double directLegendaryRate,
        double crystalBaseRate,
        double pointCrystalRate,
        double randomMascotRate,
        double cashbackRate300,
        double cashbackRate150,
        double cashbackRate60,
        double cashbackRate20) {

    /** Default point-paid rolls below soft pity: 1 crystal per 10,000 rolls (0.01%). */
    public static final double POINT_CRYSTAL_RATE_BELOW_SOFT = 1.0 / 10_000;

    /** Point soft pity ramp caps below 100% by default so roll 100 is not a forced crystal unless configured to 100%. */
    public static final double POINT_CRYSTAL_RATE_SOFT_CAP = 0.99;

    public GachaRateConfig(
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
        this(
                costSinglePoints,
                costMultiPoints,
                hardPityLimit,
                softPityStart,
                directLegendaryRate,
                crystalBaseRate,
                POINT_CRYSTAL_RATE_BELOW_SOFT,
                randomMascotRate,
                cashbackRate300,
                cashbackRate150,
                cashbackRate60,
                cashbackRate20);
    }

    public static GachaRateConfig defaults() {
        return new GachaRateConfig(
                100,
                900,
                100,
                90,
                1.0 / 6000.0,
                0.0015,
                POINT_CRYSTAL_RATE_BELOW_SOFT,
                0.003,
                0.025,
                0.06,
                0.18,
                0.42);
    }

    public double softPityRateStep() {
        if (hardPityLimit <= softPityStart) {
            return 1.0;
        }
        return Math.max(0.0, (1.0 - crystalBaseRate) / (hardPityLimit - softPityStart));
    }

    public double pointsSoftPityRateStep() {
        double cap = Math.max(pointCrystalRate, POINT_CRYSTAL_RATE_SOFT_CAP);
        if (hardPityLimit <= softPityStart) {
            return cap;
        }
        return Math.max(0.0, (cap - pointCrystalRate) / (hardPityLimit - softPityStart));
    }

    public double crystalRateForPointRoll(int rollNumber) {
        if (rollNumber < softPityStart()) {
            return pointCrystalRate;
        }
        double cap = Math.max(pointCrystalRate, POINT_CRYSTAL_RATE_SOFT_CAP);
        return Math.min(
                cap,
                pointCrystalRate
                        + (rollNumber - softPityStart()) * pointsSoftPityRateStep());
    }

    public int pointCost(int rollCount) {
        return rollCount == 1 ? costSinglePoints : costMultiPoints;
    }

    public com.eiu.capstone.backend.DTO.GachaConfigView toView() {
        return new com.eiu.capstone.backend.DTO.GachaConfigView(
                costSinglePoints,
                costMultiPoints,
                hardPityLimit,
                softPityStart,
                directLegendaryRate,
                crystalBaseRate,
                pointCrystalRate,
                randomMascotRate,
                cashbackRate300,
                cashbackRate150,
                cashbackRate60,
                cashbackRate20);
    }
}
