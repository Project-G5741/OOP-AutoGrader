package com.eiu.capstone.backend.DTO;

import java.util.List;

public record GachaRollResponse(
        List<GachaRollItemView> pulls,
        int gachaPity,
        int points,
        int gachaTickets,
        int crystals,
        List<String> ownedMascotIds) {
}