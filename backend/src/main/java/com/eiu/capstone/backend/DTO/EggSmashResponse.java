package com.eiu.capstone.backend.DTO;

import java.util.List;

public record EggSmashResponse(
        List<EggSmashCellView> cells,
        List<Integer> picks,
        int pointsEarned,
        int crystalsEarned,
        int points,
        int crystals) {
}
