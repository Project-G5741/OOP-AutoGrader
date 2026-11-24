package com.eiu.capstone.backend.DTO;

public record EggSmashCellView(
        int index,
        boolean picked,
        String type,
        int points,
        int crystals) {
}
