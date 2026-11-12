package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedPercentage;

/**
 * Lab-level MMD, Declaration, and Operational percentages for one attempt.
 * A null pillar means that pillar does not apply to the lab.
 */
public record LabPillarScores(
        BigDecimal mmdScore,
        BigDecimal declarationScore,
        BigDecimal operationalScore) {

    public boolean anyStored() {
        return mmdScore != null || declarationScore != null || operationalScore != null;
    }

    /**
     * Weighted mean of each applicable pillar across rubric challenges.
     * A challenge the student omitted counts as 0 when that pillar applies.
     */
    public static LabPillarScores rollup(LabRubricSnapshot rubric, Map<Integer, PillarScoreBreakdown> byNumber) {
        List<WeightedPercentage> mmd = new ArrayList<>();
        List<WeightedPercentage> declaration = new ArrayList<>();
        List<WeightedPercentage> operational = new ArrayList<>();
        if (rubric != null && rubric.byChallengeNumber() != null) {
            List<ChallengeRubric> challenges = rubric.byChallengeNumber().values().stream()
                    .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
                    .toList();
            for (ChallengeRubric challenge : challenges) {
                PillarScoreBreakdown scores = byNumber == null ? null : byNumber.get(challenge.challengeNumber());
                int weight = challenge.weight();
                if (challenge.hasMmd()) {
                    mmd.add(new WeightedPercentage(weight, pillarOrZero(scores == null ? null : scores.mmdPillar())));
                }
                if (challenge.hasClass()) {
                    declaration.add(new WeightedPercentage(weight, pillarOrZero(scores == null ? null : scores.classPillar())));
                }
                boolean operationalApplies = challenge.testcases() != null && !challenge.testcases().isEmpty();
                if (operationalApplies) {
                    operational.add(new WeightedPercentage(weight, pillarOrZero(scores == null ? null : scores.testcasePillar())));
                }
            }
        }
        return new LabPillarScores(meanOrNull(mmd), meanOrNull(declaration), meanOrNull(operational));
    }

    private static BigDecimal pillarOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal meanOrNull(List<WeightedPercentage> parts) {
        if (parts.isEmpty()) {
            return null;
        }
        return PillarScoreAggregator.weightedLabPercentage(parts);
    }
}
