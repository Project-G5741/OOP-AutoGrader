package com.eiu.capstone.backend.DTO.feedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackListItemDTO(
        UUID id,
        String itemType,
        String preview,
        String authorIrn,
        String authorName,
        String status,
        OffsetDateTime createdAt,
        int attachmentCount) {
}
