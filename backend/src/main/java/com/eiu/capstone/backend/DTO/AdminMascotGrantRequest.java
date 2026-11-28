package com.eiu.capstone.backend.DTO;

import java.util.List;

public record AdminMascotGrantRequest(
        List<String> mascotIds,
        String mascotId,
        Integer pricePaid,
        Boolean equip) {
}
