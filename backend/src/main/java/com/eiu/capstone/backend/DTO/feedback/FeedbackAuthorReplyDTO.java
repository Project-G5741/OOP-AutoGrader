package com.eiu.capstone.backend.DTO.feedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackAuthorReplyDTO(
        UUID id,
        String itemType,
        String lecturerResponse,
        OffsetDateTime responseAt) {
}
