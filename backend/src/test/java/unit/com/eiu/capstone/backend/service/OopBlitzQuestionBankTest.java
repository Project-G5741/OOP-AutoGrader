package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.OopBlitzQuestionBank;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.BankStats;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

class OopBlitzQuestionBankTest {

    @Test
    void proceduralStats_exposesAtLeast60TemplatesAnd15000VariantsAcrossAllFourKinds() {
        BankStats stats = OopBlitzQuestionBank.proceduralStats();

        assertTrue(stats.proceduralTemplateCount() >= 60,
                "Expected >= 60 procedural templates, got " + stats.proceduralTemplateCount());
        assertTrue(stats.estimatedProceduralVariants() >= 15_000);
        assertTrue(stats.proceduralByKind().get(OopBlitzQuestionBank.KIND_BUG) >= 24);
        assertTrue(stats.proceduralByKind().get(OopBlitzQuestionBank.KIND_OUTPUT) >= 22);
        assertTrue(stats.proceduralByKind().get(OopBlitzQuestionBank.KIND_UML) >= 9);
        assertTrue(stats.proceduralByKind().get(OopBlitzQuestionBank.KIND_MODIFIER) >= 7);
    }

    @Test
    void generateMatch_buildsBalancedEightQuestionsAcrossAllFourKindsAndValidBugLines() {
        for (long seed = 1; seed <= 40; seed++) {
            List<StoredQuestion> match = OopBlitzQuestionBank.generateMatch(Set.of(), new Random(seed));

            assertEquals(8, match.size());
            for (int i = 0; i < 3; i++) {
                assertEquals(OopBlitzQuestionBank.DIFF_EASY, match.get(i).difficulty());
                assertEquals(15, match.get(i).timeLimitSec());
            }
            for (int i = 3; i < 6; i++) {
                assertEquals(OopBlitzQuestionBank.DIFF_MEDIUM, match.get(i).difficulty());
                assertEquals(15, match.get(i).timeLimitSec());
            }
            for (int i = 6; i < 8; i++) {
                assertEquals(OopBlitzQuestionBank.DIFF_HARD, match.get(i).difficulty());
                assertEquals(20, match.get(i).timeLimitSec());
            }

            Set<String> seenKeys = new HashSet<>();
            Set<String> seenKinds = new HashSet<>();
            for (StoredQuestion q : match) {
                assertTrue(seenKeys.add(q.key()), "Duplicate key in match seed=" + seed + ": " + q.key());
                seenKinds.add(q.kind());
                assertFalse(q.prompt().isBlank());
                assertFalse(q.codeLines().isEmpty());
                assertFalse(q.explanation().isBlank());

                if (OopBlitzQuestionBank.KIND_BUG.equals(q.kind())) {
                    assertTrue(q.options().isEmpty());
                    assertTrue(q.correctIndex() >= 0 && q.correctIndex() < q.codeLines().size(),
                            "Out-of-range bug line on " + q.key());
                    assertTrue(
                            q.explanation().startsWith("Line " + (q.correctIndex() + 1) + ":"),
                            "BUG explanation must match 1-based line number on " + q.key() + ": " + q.explanation());
                } else {
                    assertEquals(4, q.options().size(), "Choice question must have 4 options: " + q.key());
                    assertEquals(4, new HashSet<>(q.options()).size(), "Duplicate option on " + q.key());
                    assertTrue(q.correctIndex() >= 0 && q.correctIndex() < 4);
                }
            }

            assertEquals(
                    Set.of(
                            OopBlitzQuestionBank.KIND_BUG,
                            OopBlitzQuestionBank.KIND_OUTPUT,
                            OopBlitzQuestionBank.KIND_UML,
                            OopBlitzQuestionBank.KIND_MODIFIER),
                    seenKinds,
                    "Every 8-question match must cover all 4 kinds (seed=" + seed + ")");
        }
    }

    @Test
    void generateMatch_prefersUnseenKeysWhenAvoidSetProvided() {
        List<StoredQuestion> first = OopBlitzQuestionBank.generateMatch(Set.of(), new Random(1L));
        Set<String> seen = new HashSet<>();
        first.forEach(q -> seen.add(q.key()));

        List<StoredQuestion> second = OopBlitzQuestionBank.generateMatch(seen, new Random(2L));
        long overlap = second.stream().filter(q -> seen.contains(q.key())).count();
        assertEquals(0, overlap, "Pool is large enough to avoid repeating any template key on run 2");
    }

    @Test
    void generateMatch_producesHighUniqueVariantVolumeAcross400Matches() {
        Set<String> uniqueSignatures = new HashSet<>();
        for (long seed = 1000; seed < 1400; seed++) {
            List<StoredQuestion> match = OopBlitzQuestionBank.generateMatch(Set.of(), new Random(seed));
            for (StoredQuestion q : match) {
                String sig = q.key()
                        + "|" + q.prompt()
                        + "|" + String.join("\n", q.codeLines())
                        + "|" + String.join("||", q.options());
                uniqueSignatures.add(sig);
            }
        }
        assertTrue(
                uniqueSignatures.size() >= 1_200,
                "Expected >= 1,200 distinct rendered variants across 400 matches, got " + uniqueSignatures.size());
    }

    @Test
    void generateMatch_blendsCustomQuestionsShufflesChoicesAndRespectsAvoidKeys() {
        StoredQuestion customChoice = new StoredQuestion(
                "custom:midterm-1",
                OopBlitzQuestionBank.KIND_OUTPUT,
                OopBlitzQuestionBank.DIFF_MEDIUM,
                15,
                "Midterm custom output question",
                List.of("System.out.println(42);"),
                List.of("42", "24", "0", "Compile error"),
                0,
                "Prints 42.");

        boolean appeared = false;
        Set<Integer> observedCorrectIndexes = new HashSet<>();
        for (long seed = 1; seed <= 50; seed++) {
            List<StoredQuestion> match = OopBlitzQuestionBank.generateMatch(
                    List.of(customChoice), Set.of(), new Random(seed));
            for (StoredQuestion q : match) {
                if ("custom:midterm-1".equals(q.key())) {
                    appeared = true;
                    assertEquals("42", q.options().get(q.correctIndex()));
                    observedCorrectIndexes.add(q.correctIndex());
                }
            }
        }
        assertTrue(appeared, "Active custom question should blend into student matches");
        assertTrue(observedCorrectIndexes.size() >= 2, "Custom question choices should be shuffled across matches");

        // When in avoidKeys, custom question should be skipped in favor of unseen procedural questions
        for (long seed = 1; seed <= 20; seed++) {
            List<StoredQuestion> match = OopBlitzQuestionBank.generateMatch(
                    List.of(customChoice), Set.of("custom:midterm-1"), new Random(seed));
            assertTrue(match.stream().noneMatch(q -> "custom:midterm-1".equals(q.key())));
        }
    }
}
