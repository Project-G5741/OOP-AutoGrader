package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.UUID;

public record AdminMascotBulkGrantRequest(
        List<UUID> userIds,
        List<String> mascotIds,
        Integer pricePaid,
        Boolean equip,
        Integer crystalsDelta,
        Integer ticketsDelta,
        Integer pointsDelta) {

    public AdminMascotBulkGrantRequest(
            List<UUID> userIds,
            List<String> mascotIds,
            Integer pricePaid,
            Boolean equip) {
        this(userIds, mascotIds, pricePaid, equip, 0, 0, 0);
    }
}
