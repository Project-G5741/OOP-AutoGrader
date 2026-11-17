package com.eiu.capstone.backend.ai;

public record ConceptualHintPayload(
        String summary,
        String category,
        String concept,
        String question,
        String explanation,
        String language) {}
