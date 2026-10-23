package com.eiu.capstone.backend.grading.testcase;

public record ComparisonOutcome(
        InvocationOutcomeKind kind,
        Object comparisonResult,
        String errorMessage) {

    public static ComparisonOutcome normal(Object comparisonResult) {
        return new ComparisonOutcome(InvocationOutcomeKind.NORMAL, comparisonResult, null);
    }

    public static ComparisonOutcome error(String message) {
        return new ComparisonOutcome(InvocationOutcomeKind.ERROR, null, message);
    }
}
