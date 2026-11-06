package com.eiu.capstone.backend.grading;

import java.util.UUID;

public record GradedChallengeSummary(UUID challengeId, int scorePercent) {}
