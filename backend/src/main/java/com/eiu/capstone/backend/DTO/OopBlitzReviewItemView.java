package com.eiu.capstone.backend.DTO;

public record OopBlitzReviewItemView(
        int index,
        String kind,
        String difficulty,
        String prompt,
        boolean correct,
        boolean timedOut,
        int pointsEarned,
        String chosenLabel,
        String correctLabel,
        String explanation) {
}
