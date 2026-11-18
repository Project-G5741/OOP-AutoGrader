package com.eiu.capstone.backend.DTO;

import java.util.List;

public record GachaSettleRequest(
        int rollCount,
        int pity,
        int pointsCashback,
        int crystalsGranted,
        List<String> mascotIds) {
}