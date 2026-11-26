package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzQuestionUpsertRequest(
        String kind,
        String difficulty,
        String prompt,
        List<String> codeLines,
        List<String> options,
        Integer correctIndex,
        String explanation,
        String topic,
        Boolean active) {
}
