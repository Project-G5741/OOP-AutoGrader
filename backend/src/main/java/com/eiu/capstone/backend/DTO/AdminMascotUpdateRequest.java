package com.eiu.capstone.backend.DTO;

public record AdminMascotUpdateRequest(
        String targetMascotId,
        Integer pricePaid,
        Boolean equip) {
}
