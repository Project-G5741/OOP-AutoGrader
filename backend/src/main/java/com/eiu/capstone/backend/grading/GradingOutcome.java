package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.util.List;

public record GradingOutcome(BigDecimal overallScore, List<GradedChallengeSummary> gradedChallenges) {}
