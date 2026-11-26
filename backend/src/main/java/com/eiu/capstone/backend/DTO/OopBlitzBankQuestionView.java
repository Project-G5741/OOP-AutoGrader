package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.UUID;

public record OopBlitzBankQuestionView(
        UUID id,
        String kind,
        String difficulty,
        String prompt,
        List<String> codeLines,
        List<String> options,
        int correctIndex,
        String explanation,
        String topic,
        boolean active,
        String createdAt,
        String updatedAt) {
}
