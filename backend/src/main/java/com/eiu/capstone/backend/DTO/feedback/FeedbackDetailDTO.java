package com.eiu.capstone.backend.DTO.feedback;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FeedbackDetailDTO(
        UUID id,
        String itemType,
        String message,
        String authorIrn,
        String authorName,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt,
        String lecturerResponse,
        OffsetDateTime responseAt,
        List<FeedbackAttachmentMetaDTO> attachments) {
}
