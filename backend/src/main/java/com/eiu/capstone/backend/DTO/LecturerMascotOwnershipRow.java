package com.eiu.capstone.backend.DTO;

import java.time.OffsetDateTime;
import java.util.List;

public record LecturerMascotOwnershipRow(
        String studentCode,
        String fullName,
        String equippedMascot,
        List<OwnedMascot> owned) {

    public record OwnedMascot(String mascotId, String displayName, int pricePaid, OffsetDateTime purchasedAt) {
    }
}