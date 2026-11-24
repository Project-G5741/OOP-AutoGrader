package com.eiu.capstone.backend.DTO;

public record GachaRateUpdateRequest(
        Integer costSinglePoints,
        Integer costMultiPoints,
        Integer hardPityLimit,
        Integer softPityStart,
        Double directLegendaryRate,
        Double crystalBaseRate,
        Double pointCrystalRate,
        Double randomMascotRate,
        Double cashbackRate300,
        Double cashbackRate150,
        Double cashbackRate60,
        Double cashbackRate20) {
}
