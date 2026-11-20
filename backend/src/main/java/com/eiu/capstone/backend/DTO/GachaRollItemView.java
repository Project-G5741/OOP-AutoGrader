package com.eiu.capstone.backend.DTO;

public record GachaRollItemView(
        String type,
        String mascotId,
        String displayName,
        String rarity,
        int pointsRefund,
        int crystalAmount,
        boolean wasPity) {
}