package com.eiu.capstone.backend.DTO.feedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackCreatedDTO(
        UUID id,
        String itemType,
        String status,
        OffsetDateTime createdAt) {
}
