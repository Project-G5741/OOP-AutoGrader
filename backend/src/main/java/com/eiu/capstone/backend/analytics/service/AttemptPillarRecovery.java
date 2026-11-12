package com.eiu.capstone.backend.analytics.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eiu.capstone.backend.grading.LabPillarScores;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.MmdOutcomesCodec;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedPercentage;

/**
 * Rebuilds pillar percentages for attempts graded before those columns were stored.
 * Uses the same weights as {@link PillarScoreAggregator}. When the Declaration shell
 * cannot be read from member flags, the stored challenge score is inverted and the
 * lowest two-decimal value that reproduces it is kept (scores never round up).
 */
public final class AttemptPillarRecovery {

    private AttemptPillarRecovery() {}

    public record ChallengeSource(UUID challengeId, BigDecimal score, String mmdOutcomesJson) {}

    public record Sources(
            List<ChallengeSource> challenges,
            Map<UUID, Boolean> fieldCorrect,
            Map<UUID, Boolean> methodCorrect,
            Map<UUID, Boolean> constructorCorrect,
            Map<UUID, String> testcaseStatus) {

        public Sources {
            challenges = challenges == null ? List.of() : List.copyOf(challenges);
            fieldCorrect = fieldCorrect == null ? Map.of() : Map.copyOf(fieldCorrect);
            methodCorrect = methodCorrect == null ? Map.of() : Map.copyOf(methodCorrect);
            constructorCorrect = constructorCorrect == null ? Map.of() : Map.copyOf(constructorCorrect);
            testcaseStatus = testcaseStatus == null ? Map.of() : Map.copyOf(testcaseStatus);
        }
    }

    public record Recovered(LabPillarScores lab, Map<UUID, LabPillarScores> byChallenge) {}

    public static Recovered recover(LabRubricSnapshot rubric, Sources sources) {
        if (rubric == null || rubric.byChallengeNumber() == null || sources == null
                || sources.challenges().isEmpty()) {
            return new Recovered(new LabPillarScores(null, null, null), Map.of());
        }
        Map<UUID, ChallengeSource> byId = new HashMap<>();
        for (ChallengeSource source : sources.challenges()) {
            if (source != null && source.challengeId() != null) {
                byId.put(source.challengeId(), source);
            }
        }
        List<WeightedPercentage> mmdParts = new ArrayList<>();
        List<WeightedPercentage> declarationParts = new ArrayList<>();
        List<WeightedPercentage> operationalParts = new ArrayList<>();
        boolean mmdUnknown = false;
        boolean declarationUnknown = false;
        boolean operationalUnknown = false;
        Map<UUID, LabPillarScores> byChallenge = new HashMap<>();

        List<ChallengeRubric> challenges = rubric.byChallengeNumber().values().stream()
                .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
                .toList();
        for (ChallengeRubric challenge : challenges) {
            ChallengeSource source = byId.get(challenge.challengeId());
            Pillar mmd = mmdPillar(challenge, source, sources);
            Pillar operational = operationalPillar(challenge, source, sources);
            Pillar declaration = declarationPillar(challenge, source, sources, mmd, operational);
            byChallenge.put(challenge.challengeId(), new LabPillarScores(
                    mmd.applicable() ? mmd.score() : null,
                    declaration.applicable() ? declaration.score() : null,
                    operational.applicable() ? operational.score() : null));
            int weight = challenge.weight();
            mmdUnknown |= accumulate(mmdParts, weight, mmd);
            declarationUnknown |= accumulate(declarationParts, weight, declaration);
            operationalUnknown |= accumulate(operationalParts, weight, operational);
        }
        return new Recovered(new LabPillarScores(
                finish(mmdParts, mmdUnknown),
                finish(declarationParts, declarationUnknown),
                finish(operationalParts, operationalUnknown)), byChallenge);
    }

    private static boolean accumulate(List<WeightedPercentage> parts, int weight, Pillar pillar) {
        if (!pillar.applicable()) {
            return false;
        }
        if (pillar.score() == null) {
            return true;
        }
        parts.add(new WeightedPercentage(weight, pillar.score()));
        return false;
    }

    private static BigDecimal finish(List<WeightedPercentage> parts, boolean unknown) {
        if (unknown || parts.isEmpty()) {
            return null;
        }
        return PillarScoreAggregator.weightedLabPercentage(parts);
    }

    private static Pillar mmdPillar(ChallengeRubric challenge, ChallengeSource source, Sources sources) {
        if (!challenge.hasMmd()) {
            return Pillar.na();
        }
        if (source == null) {
            return Pillar.of(BigDecimal.ZERO);
        }
        MmdGradingOutcome outcome = MmdOutcomesCodec.fromJson(source.mmdOutcomesJson());
        if (outcome == null) {
            return Pillar.unknown();
        }
        List<WeightedAccuracy> weighted = new ArrayList<>();
        for (ClassRubric expectedClass : safeClasses(challenge)) {
            double classAccuracy = PartialCreditEvaluator.accuracy(List.of(
                    outcome.isClassPresent(expectedClass.id()),
                    outcome.isClassCorrect(expectedClass.id())));
            weighted.add(new WeightedAccuracy(
                    MemberWeightCalculator.configuredWeight(expectedClass.weight()), classAccuracy));
            for (var field : expectedClass.fields()) {
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(outcome.isFieldCorrect(field.id()))));
            }
            for (var method : expectedClass.methods()) {
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(outcome.isMethodCorrect(method.id()))));
            }
            for (var constructor : expectedClass.constructors()) {
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(outcome.isConstructorCorrect(constructor.id()))));
            }
        }
        if (challenge.relations() != null) {
            for (var relation : challenge.relations()) {
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(outcome.isRelationCorrect(relation.id()))));
            }
        }
        return Pillar.of(PillarScoreAggregator.pillarPercentage(weighted));
    }

    private static Pillar operationalPillar(ChallengeRubric challenge, ChallengeSource source, Sources sources) {
        List<TestcaseRubric> testcases = challenge.testcases() == null ? List.of() : challenge.testcases();
        if (testcases.isEmpty()) {
            return Pillar.na();
        }
        if (source == null) {
            return Pillar.of(BigDecimal.ZERO);
        }
        boolean anyRow = false;
        for (TestcaseRubric testcase : testcases) {
            if (sources.testcaseStatus().containsKey(testcase.id())) {
                anyRow = true;
                break;
            }
        }
        if (!anyRow) {
            return Pillar.unknown();
        }
        List<WeightedAccuracy> weighted = new ArrayList<>();
        for (TestcaseRubric testcase : testcases) {
            String status = sources.testcaseStatus().get(testcase.id());
            double accuracy = status != null && "PASSED".equalsIgnoreCase(status.trim()) ? 1.0 : 0.0;
            weighted.add(new WeightedAccuracy(MemberWeightCalculator.testcaseWeight(testcase.weight()), accuracy));
        }
        return Pillar.of(PillarScoreAggregator.pillarPercentage(weighted));
    }

    private static Pillar declarationPillar(ChallengeRubric challenge,
                                            ChallengeSource source,
                                            Sources sources,
                                            Pillar mmd,
                                            Pillar operational) {
        if (!challenge.hasClass()) {
            return Pillar.na();
        }
        if (source == null) {
            return Pillar.of(BigDecimal.ZERO);
        }
        BigDecimal fromMembers = declarationFromMembers(challenge, sources);
        if (fromMembers != null) {
            return Pillar.of(fromMembers);
        }
        if (mmd.applicable() && mmd.score() == null) {
            return Pillar.unknown();
        }
        if (operational.applicable() && operational.score() == null) {
            return Pillar.unknown();
        }
        BigDecimal solved = solveDeclaration(
                source.score(),
                challenge.classWeight(),
                mmd.applicable() ? mmd.score() : null,
                mmd.applicable(),
                challenge.mmdWeight(),
                operational.applicable() ? operational.score() : null,
                operational.applicable(),
                challenge.testcaseWeight());
        return solved == null ? Pillar.unknown() : Pillar.of(solved);
    }

    /**
     * Exact when every class shell is proven by at least one correct member.
     * Returns null when a shell-only class or an all-fail class leaves the shell unknown.
     */
    private static BigDecimal declarationFromMembers(ChallengeRubric challenge, Sources sources) {
        List<ClassRubric> classes = safeClasses(challenge);
        if (classes.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        List<WeightedAccuracy> weighted = new ArrayList<>();
        for (ClassRubric expectedClass : classes) {
            List<Boolean> members = new ArrayList<>();
            if (expectedClass.fields() != null) {
                for (var field : expectedClass.fields()) {
                    if (!sources.fieldCorrect().containsKey(field.id())) {
                        return null;
                    }
                    members.add(Boolean.TRUE.equals(sources.fieldCorrect().get(field.id())));
                }
            }
            if (expectedClass.methods() != null) {
                for (var method : expectedClass.methods()) {
                    if (!sources.methodCorrect().containsKey(method.id())) {
                        return null;
                    }
                    members.add(Boolean.TRUE.equals(sources.methodCorrect().get(method.id())));
                }
            }
            if (expectedClass.constructors() != null) {
                for (var constructor : expectedClass.constructors()) {
                    if (!sources.constructorCorrect().containsKey(constructor.id())) {
                        return null;
                    }
                    members.add(Boolean.TRUE.equals(sources.constructorCorrect().get(constructor.id())));
                }
            }
            if (members.isEmpty() || members.stream().noneMatch(Boolean::booleanValue)) {
                return null;
            }
            weighted.add(new WeightedAccuracy(
                    MemberWeightCalculator.configuredWeight(expectedClass.weight()), 1.0));
            for (Boolean correct : members) {
                weighted.add(new WeightedAccuracy(
                        MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(Boolean.TRUE.equals(correct))));
            }
        }
        return PillarScoreAggregator.pillarPercentage(weighted);
    }

    static BigDecimal solveDeclaration(BigDecimal challengeScore,
                                       int classWeight,
                                       BigDecimal mmd,
                                       boolean mmdApplicable,
                                       int mmdWeight,
                                       BigDecimal operational,
                                       boolean operationalApplicable,
                                       int operationalWeight) {
        if (challengeScore == null) {
            return null;
        }
        if (!mmdApplicable && !operationalApplicable) {
            return challengeScore.setScale(2, java.math.RoundingMode.DOWN);
        }
        int classW = Math.max(1, classWeight);
        int mmdW = Math.max(1, mmdWeight);
        int operationalW = Math.max(1, operationalWeight);
        BigDecimal stored = challengeScore.setScale(2, java.math.RoundingMode.DOWN);
        BigDecimal lowest = null;
        for (int cents = 0; cents <= 10000; cents++) {
            BigDecimal candidate = BigDecimal.valueOf(cents, 2);
            BigDecimal recomputed = PillarScoreAggregator.challengePercentage(
                    candidate, true, classW,
                    mmd == null ? BigDecimal.ZERO : mmd, mmdApplicable, mmdW,
                    operational == null ? BigDecimal.ZERO : operational, operationalApplicable, operationalW);
            if (recomputed.compareTo(stored) == 0) {
                lowest = candidate;
                break;
            }
        }
        return lowest;
    }

    private static List<ClassRubric> safeClasses(ChallengeRubric challenge) {
        return challenge.classes() == null ? List.of() : challenge.classes();
    }

    private record Pillar(boolean applicable, BigDecimal score) {
        static Pillar na() {
            return new Pillar(false, null);
        }

        static Pillar unknown() {
            return new Pillar(true, null);
        }

        static Pillar of(BigDecimal score) {
            return new Pillar(true, score);
        }
    }
}
