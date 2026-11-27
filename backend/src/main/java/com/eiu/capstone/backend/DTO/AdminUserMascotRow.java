package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.UUID;

public record AdminUserMascotRow(
        UUID userId,
        String studentCode,
        String teacherCode,
        String fullName,
        String email,
        boolean active,
        boolean inCurrentTerm,
        boolean iceQueenEligible,
        String equippedMascot,
        int points,
        int crystals,
        int gachaTickets,
        int gachaPityCount,
        int eggSmashPity,
        List<OwnedPetView> owned) {

    public record OwnedPetView(
            String mascotId,
            String displayName,
            int catalogPrice,
            int pricePaid,
            String purchasedAt,
            boolean equipped) {
    }
}
