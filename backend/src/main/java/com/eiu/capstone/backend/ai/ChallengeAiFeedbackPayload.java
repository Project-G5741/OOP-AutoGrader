package com.eiu.capstone.backend.ai;

import java.util.List;

public record ChallengeAiFeedbackPayload(
        String challengeSummary,
        List<ClassGuidanceItem> classes,
        String language
) {}
