package com.eiu.capstone.backend.DTO;

public record GachaConfigView(
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
}
