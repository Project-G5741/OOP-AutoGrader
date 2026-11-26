package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Orchestrates the 4 procedural Java OOP question engines plus optional lecturer/admin custom questions.
 * Every 8-question match emits 3 Easy (15s), 3 Medium (15s), and 2 Hard (20s) items covering all 4 kinds.
 */
public final class OopBlitzQuestionBank {

    public static final String KIND_BUG = "BUG";
    public static final String KIND_OUTPUT = "OUTPUT";
    public static final String KIND_UML = "UML";
    public static final String KIND_MODIFIER = "MODIFIER";

    public static final String DIFF_EASY = "EASY";
    public static final String DIFF_MEDIUM = "MEDIUM";
    public static final String DIFF_HARD = "HARD";

    private static final List<String> ALL_KINDS = List.of(KIND_BUG, KIND_OUTPUT, KIND_UML, KIND_MODIFIER);
    private static final int ESTIMATED_PROCEDURAL_VARIANTS = 15_000;

    public record StoredQuestion(
            String key,
            String kind,
            String difficulty,
            int timeLimitSec,
            String prompt,
            List<String> codeLines,
            List<String> options,
            int correctIndex,
            String explanation) {
    }

    public record StoredAnswer(
            int index,
            String kind,
            String difficulty,
            String prompt,
            Integer selectedIndex,
            boolean correct,
            boolean timedOut,
            long elapsedMs,
            int basePts,
            int comboBonus,
            int speedBonus,
            int pointsEarned,
            String chosenLabel,
            String correctLabel,
            String explanation) {
    }

    public record BankStats(
            int proceduralTemplateCount,
            int estimatedProceduralVariants,
            Map<String, Integer> proceduralByKind) {
    }

    record EngineEntry(String key, String kind, Function<RandomGenerator, StoredQuestion> builder) {
    }

    private record Candidate(
            String key,
            String kind,
            String difficulty,
            boolean custom,
            Function<RandomGenerator, StoredQuestion> supplier) {
    }

    private enum Tier {
        EASY(DIFF_EASY, 3),
        MEDIUM(DIFF_MEDIUM, 3),
        HARD(DIFF_HARD, 2);

        final String label;
        final int quota;

        Tier(String label, int quota) {
            this.label = label;
            this.quota = quota;
        }
    }

    private static final List<EngineEntry> EASY_PROCEDURAL = concat(
            OopBlitzBugEngine.easyEntries(),
            OopBlitzOutputEngine.easyEntries(),
            OopBlitzUmlEngine.easyEntries(),
            OopBlitzModifierEngine.easyEntries());

    private static final List<EngineEntry> MEDIUM_PROCEDURAL = concat(
            OopBlitzBugEngine.mediumEntries(),
            OopBlitzOutputEngine.mediumEntries(),
            OopBlitzUmlEngine.mediumEntries(),
            OopBlitzModifierEngine.mediumEntries());

    private static final List<EngineEntry> HARD_PROCEDURAL = concat(
            OopBlitzBugEngine.hardEntries(),
            OopBlitzOutputEngine.hardEntries(),
            OopBlitzUmlEngine.hardEntries(),
            OopBlitzModifierEngine.hardEntries());

    private OopBlitzQuestionBank() {
    }

    public static BankStats proceduralStats() {
        Map<String, Integer> byKind = new LinkedHashMap<>();
        for (String kind : ALL_KINDS) {
            byKind.put(kind, 0);
        }
        int total = 0;
        for (List<EngineEntry> tier : List.of(EASY_PROCEDURAL, MEDIUM_PROCEDURAL, HARD_PROCEDURAL)) {
            for (EngineEntry entry : tier) {
                total++;
                byKind.merge(entry.kind(), 1, Integer::sum);
            }
        }
        return new BankStats(total, ESTIMATED_PROCEDURAL_VARIANTS, Map.copyOf(byKind));
    }

    public static List<StoredQuestion> generateMatch(Set<String> avoidKeys, RandomGenerator rng) {
        return generateMatch(List.of(), avoidKeys, rng);
    }

    public static List<StoredQuestion> generateMatch(
            List<StoredQuestion> customPool,
            Set<String> avoidKeys,
            RandomGenerator rng) {
        Set<String> avoid = avoidKeys == null ? Set.of() : avoidKeys;
        List<StoredQuestion> custom = customPool == null ? List.of() : customPool;

        Map<Tier, List<Candidate>> customByTier = indexCustomByTier(custom);
        Map<Tier, List<Candidate>> procByTier = Map.of(
                Tier.EASY, toProceduralCandidates(Tier.EASY, EASY_PROCEDURAL),
                Tier.MEDIUM, toProceduralCandidates(Tier.MEDIUM, MEDIUM_PROCEDURAL),
                Tier.HARD, toProceduralCandidates(Tier.HARD, HARD_PROCEDURAL));

        // Plan exact kind targets across 8 slots so every match covers all 4 kinds
        List<String> targetKinds = new ArrayList<>(OopBlitzRules.QUESTIONS_PER_MATCH);
        targetKinds.addAll(ALL_KINDS);
        List<String> secondPass = new ArrayList<>(ALL_KINDS);
        OopBlitzDomains.shuffle(secondPass, rng);
        targetKinds.addAll(secondPass);
        OopBlitzDomains.shuffle(targetKinds, rng);

        Set<String> usedMatchKeys = new HashSet<>();
        Map<String, Integer> kindCounts = new HashMap<>();
        for (String k : ALL_KINDS) {
            kindCounts.put(k, 0);
        }

        List<StoredQuestion> match = new ArrayList<>(OopBlitzRules.QUESTIONS_PER_MATCH);
        int cursor = 0;

        for (Tier tier : Tier.values()) {
            boolean customUsedInTier = false;
            for (int slot = 0; slot < tier.quota; slot++) {
                String desiredKind = targetKinds.get(cursor++);
                int remainingSlots = OopBlitzRules.QUESTIONS_PER_MATCH - match.size();
                int remainingBeforeHard = Math.max(0, (Tier.EASY.quota + Tier.MEDIUM.quota) - match.size());
                List<String> missingKinds = ALL_KINDS.stream()
                        .filter(k -> kindCounts.getOrDefault(k, 0) == 0)
                        .toList();
                boolean forceMissingInPreHard = tier != Tier.HARD && !missingKinds.isEmpty() && remainingBeforeHard <= missingKinds.size();
                boolean strictKind = !missingKinds.isEmpty() && remainingSlots <= missingKinds.size();
                if (strictKind || forceMissingInPreHard) {
                    desiredKind = missingKinds.get(0);
                }

                Candidate picked = pickCandidateForSlot(
                        tier,
                        desiredKind,
                        strictKind,
                        procByTier.get(tier),
                        customByTier.getOrDefault(tier, List.of()),
                        customUsedInTier,
                        avoid,
                        usedMatchKeys,
                        kindCounts,
                        rng);

                usedMatchKeys.add(picked.key());
                kindCounts.merge(picked.kind(), 1, Integer::sum);
                if (picked.custom()) {
                    customUsedInTier = true;
                }
                match.add(picked.supplier().apply(rng));
            }
        }
        return List.copyOf(match);
    }

    private static Candidate pickCandidateForSlot(
            Tier tier,
            String desiredKind,
            boolean strictKind,
            List<Candidate> proceduralPool,
            List<Candidate> customPool,
            boolean customUsedInTier,
            Set<String> avoid,
            Set<String> usedInMatch,
            Map<String, Integer> kindCounts,
            RandomGenerator rng) {
        if (!customUsedInTier && !customPool.isEmpty()) {
            List<Candidate> unseenMatchingCustom = filterCandidates(customPool, desiredKind, avoid, usedInMatch, true);
            List<Candidate> unseenAnyCustom = strictKind
                    ? List.of()
                    : filterCandidates(customPool, null, avoid, usedInMatch, true);
            boolean hasDesiredInCustom = !unseenMatchingCustom.isEmpty();
            if ((hasDesiredInCustom || !unseenAnyCustom.isEmpty()) && rng.nextDouble() < 0.45) {
                List<Candidate> pool = hasDesiredInCustom ? unseenMatchingCustom : unseenAnyCustom;
                return pool.get(rng.nextInt(pool.size()));
            }
        }

        // 1. Fresh procedural with desiredKind
        List<Candidate> freshDesired = filterCandidates(proceduralPool, desiredKind, avoid, usedInMatch, true);
        if (!freshDesired.isEmpty()) {
            return freshDesired.get(rng.nextInt(freshDesired.size()));
        }

        // 2. If strictKind, allow already-seen procedural matching the missing kind before abandoning kind
        if (strictKind) {
            List<Candidate> strictSeen = filterCandidates(proceduralPool, desiredKind, avoid, usedInMatch, false);
            if (!strictSeen.isEmpty()) {
                return strictSeen.get(rng.nextInt(strictSeen.size()));
            }
        }

        // 3. Fresh procedural preferring missing / least-used kinds (beats repeating avoid keys)
        List<Candidate> freshAny = filterCandidates(proceduralPool, null, avoid, usedInMatch, true);
        if (!freshAny.isEmpty()) {
            freshAny.sort((a, b) -> Integer.compare(
                    kindCounts.getOrDefault(a.kind(), 0),
                    kindCounts.getOrDefault(b.kind(), 0)));
            int minCount = kindCounts.getOrDefault(freshAny.get(0).kind(), 0);
            List<Candidate> leastUsed = freshAny.stream()
                    .filter(c -> kindCounts.getOrDefault(c.kind(), 0) == minCount)
                    .toList();
            return leastUsed.get(rng.nextInt(leastUsed.size()));
        }

        // 4. Already-seen procedural matching desiredKind
        List<Candidate> anyDesired = filterCandidates(proceduralPool, desiredKind, avoid, usedInMatch, false);
        if (!anyDesired.isEmpty()) {
            return anyDesired.get(rng.nextInt(anyDesired.size()));
        }

        // 5. Fallback to any procedural not used in this match (or custom if exhausted)
        List<Candidate> fallback = new ArrayList<>(filterCandidates(proceduralPool, null, avoid, usedInMatch, false));
        if (fallback.isEmpty()) {
            fallback.addAll(filterCandidates(customPool, null, avoid, usedInMatch, false));
        }
        if (fallback.isEmpty()) {
            fallback.addAll(proceduralPool);
        }
        return fallback.get(rng.nextInt(fallback.size()));
    }

    private static List<Candidate> filterCandidates(
            List<Candidate> pool,
            String requiredKind,
            Set<String> avoid,
            Set<String> usedInMatch,
            boolean excludeAvoid) {
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : pool) {
            if (usedInMatch.contains(c.key())) {
                continue;
            }
            if (excludeAvoid && avoid.contains(c.key())) {
                continue;
            }
            if (requiredKind != null && !requiredKind.equals(c.kind())) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    private static List<Candidate> toProceduralCandidates(Tier tier, List<EngineEntry> entries) {
        List<Candidate> list = new ArrayList<>(entries.size());
        for (EngineEntry entry : entries) {
            list.add(new Candidate(entry.key(), entry.kind(), tier.label, false, entry.builder()));
        }
        return list;
    }

    private static Map<Tier, List<Candidate>> indexCustomByTier(List<StoredQuestion> customPool) {
        Map<Tier, List<Candidate>> map = new EnumMap<>(Tier.class);
        for (StoredQuestion q : customPool) {
            if (q == null || q.key() == null) {
                continue;
            }
            Tier tier = switch (String.valueOf(q.difficulty())) {
                case DIFF_HARD -> Tier.HARD;
                case DIFF_MEDIUM -> Tier.MEDIUM;
                default -> Tier.EASY;
            };
            map.computeIfAbsent(tier, ignored -> new ArrayList<>())
                    .add(new Candidate(q.key(), q.kind(), tier.label, true, rng -> prepareCustomForMatch(q, rng)));
        }
        return map;
    }

    private static StoredQuestion prepareCustomForMatch(StoredQuestion q, RandomGenerator rng) {
        if (KIND_BUG.equals(q.kind()) || q.options() == null || q.options().isEmpty()) {
            return q;
        }
        record Indexed(String label, boolean correct) {
        }
        List<Indexed> items = new ArrayList<>(q.options().size());
        for (int i = 0; i < q.options().size(); i++) {
            items.add(new Indexed(q.options().get(i), i == q.correctIndex()));
        }
        OopBlitzDomains.shuffle(items, rng);
        List<String> shuffledOptions = new ArrayList<>(items.size());
        int newCorrect = 0;
        for (int i = 0; i < items.size(); i++) {
            shuffledOptions.add(items.get(i).label());
            if (items.get(i).correct()) {
                newCorrect = i;
            }
        }
        return new StoredQuestion(
                q.key(),
                q.kind(),
                q.difficulty(),
                q.timeLimitSec(),
                q.prompt(),
                q.codeLines(),
                List.copyOf(shuffledOptions),
                newCorrect,
                q.explanation());
    }

    @SafeVarargs
    private static List<EngineEntry> concat(List<EngineEntry>... lists) {
        List<EngineEntry> all = new ArrayList<>();
        for (List<EngineEntry> list : lists) {
            all.addAll(list);
        }
        return Collections.unmodifiableList(all);
    }
}
