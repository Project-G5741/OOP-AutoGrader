package com.eiu.capstone.backend.DTO;

public record AdminGachaResourceUpdateRequest(
        Integer points,
        Integer crystals,
        Integer gachaTickets,
        Integer gachaPityCount,
        Integer eggSmashPity) {
}
