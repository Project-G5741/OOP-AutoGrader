package com.eiu.capstone.backend.grading.testcase;

import java.util.List;
import java.util.Map;

public record InvocationOutcome(
        InvocationOutcomeKind kind,
        Object returnValue,
        String stdout,
        boolean stdoutTruncated,
        Map<String, Object> fieldSnapshots,
        String exceptionSimpleName,
        List<String> exceptionSuperclassSimpleNames,
        String errorMessage) {

    public static InvocationOutcome normal(Object returnValue,
                                           String stdout,
                                           boolean stdoutTruncated,
                                           Map<String, Object> fieldSnapshots) {
        return new InvocationOutcome(
                InvocationOutcomeKind.NORMAL,
                returnValue,
                stdout,
                stdoutTruncated,
                fieldSnapshots == null ? Map.of() : fieldSnapshots,
                null,
                List.of(),
                null);
    }

    public static InvocationOutcome threw(String stdout,
                                          boolean stdoutTruncated,
                                          String exceptionSimpleName,
                                          List<String> exceptionSuperclassSimpleNames) {
        return new InvocationOutcome(
                InvocationOutcomeKind.THREW,
                null,
                stdout,
                stdoutTruncated,
                Map.of(),
                exceptionSimpleName,
                exceptionSuperclassSimpleNames == null ? List.of() : exceptionSuperclassSimpleNames,
                null);
    }

    public static InvocationOutcome timedOut(String stdout) {
        return new InvocationOutcome(InvocationOutcomeKind.TIMED_OUT, null, stdout, false, Map.of(),
                null, List.of(), "Invocation timed out");
    }

    public static InvocationOutcome error(String message) {
        return new InvocationOutcome(InvocationOutcomeKind.ERROR, null, "", false, Map.of(),
                null, List.of(), message);
    }
}
