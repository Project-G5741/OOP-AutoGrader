package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzQuestionView(
        int index,
        String kind,
        String difficulty,
        int timeLimitSec,
        long remainingMs,
        String prompt,
        List<String> codeLines,
        List<String> options) {
}
