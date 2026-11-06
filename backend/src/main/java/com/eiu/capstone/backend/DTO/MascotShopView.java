package com.eiu.capstone.backend.DTO;

import java.util.List;

public record MascotShopView(
        int points,
        String equipped,
        List<MascotOfferView> mascots) {
}
