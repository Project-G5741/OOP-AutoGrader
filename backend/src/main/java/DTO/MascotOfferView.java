package com.eiu.capstone.backend.DTO;

public record MascotOfferView(
        String id,
        String name,
        int price,
        boolean owned,
        boolean equipped,
        boolean starter) {
}
