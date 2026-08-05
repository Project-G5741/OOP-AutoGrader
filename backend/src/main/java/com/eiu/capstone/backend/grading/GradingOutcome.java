package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;

public record GradingOutcome(
        BigDecimal overallScore,
        List<GradedChallengeSummary> gradedChallenges,
        Map<UUID, ChallengeMmdMeta> mmdMetaByChallengeId) {}
