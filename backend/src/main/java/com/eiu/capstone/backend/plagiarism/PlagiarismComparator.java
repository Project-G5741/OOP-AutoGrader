package com.eiu.capstone.backend.plagiarism;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Three independent plagiarism checks:
 * git history (ordered hashes, 100%), metadata (100%), file-byte hashes (&gt;90% Jaccard).
 */
public final class PlagiarismComparator {

    public static final BigDecimal HASH_FLAG_THRESHOLD = new BigDecimal("0.90");

    private PlagiarismComparator() {}

    public static PlagiarismComparison compare(PlagiarismSignals left, PlagiarismSignals right) {
        PlagiarismSignals a = left == null ? PlagiarismSignals.empty() : left;
        PlagiarismSignals b = right == null ? PlagiarismSignals.empty() : right;
        boolean gitMatch = gitHistoriesMatch(a.gitCommitHashes(), b.gitCommitHashes());
        boolean metadataMatch = metadataMatches(a.metadataCanonical(), b.metadataCanonical());
        BigDecimal hashSimilarity = hashJaccard(a.fileHashes(), b.fileHashes());
        boolean hashFlag = hashSimilarity.compareTo(HASH_FLAG_THRESHOLD) > 0;
        return new PlagiarismComparison(gitMatch, metadataMatch, hashSimilarity, gitMatch || metadataMatch || hashFlag);
    }

    static boolean gitHistoriesMatch(List<String> left, List<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!normalizeHash(left.get(i)).equals(normalizeHash(right.get(i)))) {
                return false;
            }
        }
        return true;
    }

    static boolean metadataMatches(String left, String right) {
        String a = canonicalize(left);
        String b = canonicalize(right);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.equals(b);
    }

    static BigDecimal hashJaccard(List<String> left, List<String> right) {
        Set<String> a = normalizeHashSet(left);
        Set<String> b = normalizeHashSet(right);
        if (a.isEmpty() || b.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return BigDecimal.valueOf((double) intersection.size() / union.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static String metadataCanonical(GitHistory history) {
        if (history == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append(safe(history.userName()).toLowerCase(Locale.ROOT)).append('\n');
        out.append(safe(history.userEmail()).toLowerCase(Locale.ROOT)).append('\n');
        if (history.commits() != null) {
            for (GitCommitRecord commit : history.commits()) {
                out.append(safe(commit.authorName()).toLowerCase(Locale.ROOT)).append('\t');
                out.append(safe(commit.authorEmail()).toLowerCase(Locale.ROOT)).append('\t');
                out.append(commit.timestampEpochSeconds()).append('\n');
            }
        }
        String canonical = out.toString().trim();
        if (canonical.replace("\n", "").isBlank()) {
            return "";
        }
        return canonical;
    }

    private static Set<String> normalizeHashSet(List<String> hashes) {
        Set<String> set = new HashSet<>();
        if (hashes == null) {
            return set;
        }
        for (String hash : hashes) {
            String normalized = normalizeHash(hash);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    private static String normalizeHash(String hash) {
        return hash == null ? "" : hash.trim().toLowerCase(Locale.ROOT);
    }

    private static String canonicalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
