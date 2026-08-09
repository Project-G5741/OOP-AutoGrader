package com.eiu.capstone.backend.grading.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Per-attribute partial credit for declaration checks.
 * accuracy = matchingAttributes / totalAttributesChecked (0..1).
 */
public final class PartialCreditEvaluator {

    private PartialCreditEvaluator() {}

    public static double accuracy(List<Boolean> attributeMatches) {
        if (attributeMatches == null || attributeMatches.isEmpty()) {
            return 0;
        }
        int total = attributeMatches.size();
        int matches = 0;
        for (Boolean match : attributeMatches) {
            if (Boolean.TRUE.equals(match)) {
                matches++;
            }
        }
        return (double) matches / total;
    }

    public static double binaryAccuracy(boolean correct) {
        return correct ? 1.0 : 0.0;
    }

    public static List<Boolean> matches(String expected, String actual) {
        return List.of(Objects.equals(normalize(expected), normalize(actual)));
    }

    public static List<Boolean> matches(boolean expected, boolean actual) {
        return List.of(expected == actual);
    }

    public static List<Boolean> matchesList(List<String> expected, List<String> actual) {
        List<Boolean> results = new ArrayList<>();
        int size = Math.max(expected == null ? 0 : expected.size(), actual == null ? 0 : actual.size());
        if (size == 0) {
            return List.of(true);
        }
        for (int i = 0; i < size; i++) {
            String e = expected != null && i < expected.size() ? expected.get(i) : null;
            String a = actual != null && i < actual.size() ? actual.get(i) : null;
            results.add(Objects.equals(normalize(e), normalize(a)));
        }
        return results;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
