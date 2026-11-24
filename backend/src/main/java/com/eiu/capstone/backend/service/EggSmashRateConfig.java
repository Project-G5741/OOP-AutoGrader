package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.EggSmashConfigView;

/**
 * Singleton row {@code egg_smash_rate_config.id = 1}. Managed by Admin via {@code EggSmashRateConfigService}.
 */
public record EggSmashRateConfig(
        double baseCrystalHitRate,
        double turn6CrystalHitRate,
        double turn7CrystalHitRate,
        double turn8CrystalHitRate,
        double turn9CrystalHitRate,
        double eggRate300,
        double eggRate200,
        double eggRate100) {

    public static EggSmashRateConfig defaults() {
        return new EggSmashRateConfig(
                0.0,
                0.0,
                0.0,
                1.0 / 3.0,
                1.0,
                0.05,
                0.15,
                0.30);
    }

    public double crystalHitRateForTurn(int turn) {
        if (turn < StudentEggSmashService.FIRST_ELIGIBLE_TURN) {
            return baseCrystalHitRate;
        }
        if (turn == StudentEggSmashService.FIRST_ELIGIBLE_TURN) {
            return turn6CrystalHitRate;
        }
        if (turn == StudentEggSmashService.TURN_7_RAMP) {
            return turn7CrystalHitRate;
        }
        if (turn == StudentEggSmashService.SOFT_PITY_TURN) {
            return turn8CrystalHitRate;
        }
        return turn9CrystalHitRate;
    }

    public EggSmashConfigView toView() {
        return new EggSmashConfigView(
                baseCrystalHitRate,
                turn6CrystalHitRate,
                turn7CrystalHitRate,
                turn8CrystalHitRate,
                turn9CrystalHitRate,
                eggRate300,
                eggRate200,
                eggRate100);
    }
}
