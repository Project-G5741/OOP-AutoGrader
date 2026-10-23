package com.eiu.capstone.backend.DTO.feedback;

import java.util.UUID;

public record FeedbackAttachmentMetaDTO(
        UUID id,
        String originalFilename,
        String storedContentType,
        int byteSize) {
}
