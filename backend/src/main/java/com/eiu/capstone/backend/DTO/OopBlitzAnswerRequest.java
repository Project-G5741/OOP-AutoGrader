package com.eiu.capstone.backend.DTO;

import java.util.UUID;

public record OopBlitzAnswerRequest(
        UUID sessionId,
        int questionIndex,
        Integer selectedIndex) {
}
