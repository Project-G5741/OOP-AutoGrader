package com.eiu.capstone.backend.DTO;

import java.util.UUID;

public record OopBlitzStartResponse(
        UUID sessionId,
        String mode,
        boolean resumed,
        int totalQuestions,
        int currentIndex,
        int scoreSoFar,
        int correctCount,
        int combo,
        OopBlitzQuestionView question,
        OopBlitzStatusView status) {
}
