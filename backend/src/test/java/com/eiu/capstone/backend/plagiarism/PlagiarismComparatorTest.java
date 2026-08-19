package com.eiu.capstone.backend.plagiarism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlagiarismComparatorTest {

    @Test
    void gitMatch_requiresSameHashesInSameOrder() {
        assertTrue(PlagiarismComparator.gitHistoriesMatch(
                List.of("aaa", "bbb", "ccc"),
                List.of("AAA", "bbb", "ccc")));
        assertFalse(PlagiarismComparator.gitHistoriesMatch(
                List.of("aaa", "bbb"),
                List.of("bbb", "aaa")));
        assertFalse(PlagiarismComparator.gitHistoriesMatch(List.of(), List.of("aaa")));
    }

    @Test
    void metadataMatch_requiresIdenticalCanonicalText() {
        String meta = "alice\nalice@eiu.edu.vn\nalice\talice@eiu.edu.vn\t1700000000";
        assertTrue(PlagiarismComparator.metadataMatches(meta, meta));
        assertFalse(PlagiarismComparator.metadataMatches(meta, meta + "\nextra"));
        assertFalse(PlagiarismComparator.metadataMatches("", ""));
    }

    @Test
    void hashJaccard_flagsAboveNinetyPercent() {
        List<String> left = List.of("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9", "h10");
        List<String> almost = List.of("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9", "other");
        List<String> same = List.of("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9", "h10");

        assertEquals(new BigDecimal("0.82"), PlagiarismComparator.hashJaccard(left, almost));
        assertFalse(PlagiarismComparator.compare(
                new PlagiarismSignals(List.of(), "", left),
                new PlagiarismSignals(List.of(), "", almost)).flagged());

        PlagiarismComparison identical = PlagiarismComparator.compare(
                new PlagiarismSignals(List.of(), "", same),
                new PlagiarismSignals(List.of(), "", left));
        assertEquals(new BigDecimal("1.00"), identical.hashSimilarity());
        assertTrue(identical.flagged());
    }

    @Test
    void compare_anyMethodCanFlag() {
        PlagiarismComparison gitOnly = PlagiarismComparator.compare(
                new PlagiarismSignals(List.of("c1", "c2"), "", List.of("a")),
                new PlagiarismSignals(List.of("c1", "c2"), "other-meta", List.of("b")));
        assertTrue(gitOnly.gitMatch());
        assertFalse(gitOnly.metadataMatch());
        assertTrue(gitOnly.flagged());
    }
}
