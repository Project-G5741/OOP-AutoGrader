package com.eiu.capstone.backend.DTO.feedback;

public record ConvertedAttachment(
        String originalFilename,
        String storedContentType,
        byte[] content) {
}
