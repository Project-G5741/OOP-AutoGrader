package com.eiu.capstone.backend.DTO;

public record EggSmashRateUpdateRequest(
        Double baseCrystalHitRate,
        Double turn6CrystalHitRate,
        Double turn7CrystalHitRate,
        Double turn8CrystalHitRate,
        Double turn9CrystalHitRate,
        Double eggRate300,
        Double eggRate200,
        Double eggRate100) {
}
