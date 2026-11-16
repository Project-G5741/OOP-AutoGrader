package com.eiu.capstone.backend.ai;

public enum AiLearningArea {
    FIELD_MODELING,
    METHODS,
    CONSTRUCTORS;

    public static AiLearningArea require(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("learningArea is required");
        }
        try {
            return AiLearningArea.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported learningArea: " + raw);
        }
    }
}
