package com.eiu.capstone.backend.DTO;

public record EggSmashConfigView(
        double baseCrystalHitRate,
        double turn6CrystalHitRate,
        double turn7CrystalHitRate,
        double turn8CrystalHitRate,
        double turn9CrystalHitRate,
        double eggRate300,
        double eggRate200,
        double eggRate100) {
}
