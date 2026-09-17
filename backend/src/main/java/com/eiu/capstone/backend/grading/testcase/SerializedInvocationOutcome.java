package com.eiu.capstone.backend.grading.testcase;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Untrusted worker facts. Scoring keys such as {@code passed} are ignored if present.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SerializedInvocationOutcome(
        String kind,
        String returnValueJson,
        String stdout,
        boolean stdoutTruncated,
        Map<String, String> fieldSnapshotsJson,
        String exceptionSimpleName,
        List<String> exceptionSuperclassSimpleNames,
        String comparisonResultJson,
        String errorMessage,
        List<SerializedInvocationOutcome> steps) {

    public static final String KIND_ERROR = "ERROR";
    public static final String KIND_NORMAL = "NORMAL";
    public static final String KIND_THREW = "THREW";
    public static final String KIND_TIMED_OUT = "TIMED_OUT";

    public SerializedInvocationOutcome(
            String kind,
            String returnValueJson,
            String stdout,
            boolean stdoutTruncated,
            Map<String, String> fieldSnapshotsJson,
            String exceptionSimpleName,
            List<String> exceptionSuperclassSimpleNames,
            String comparisonResultJson,
            String errorMessage) {
        this(
                kind,
                returnValueJson,
                stdout,
                stdoutTruncated,
                fieldSnapshotsJson,
                exceptionSimpleName,
                exceptionSuperclassSimpleNames,
                comparisonResultJson,
                errorMessage,
                null);
    }

    public static SerializedInvocationOutcome error(String message) {
        return new SerializedInvocationOutcome(
                KIND_ERROR, null, "", false, Map.of(), null, List.of(), null, message);
    }

    public static SerializedInvocationOutcome timedOut(String stdout, boolean truncated) {
        return new SerializedInvocationOutcome(
                KIND_TIMED_OUT, null, stdout, truncated, Map.of(), null, List.of(), null, "Invocation timed out");
    }
}
