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
        String errorMessage,
        String objectTypeSimpleName,
        Map<String, Object> objectFieldSnapshots,
        Map<String, Boolean> equalsNamed) {

    public InvocationOutcome {
        fieldSnapshots = fieldSnapshots == null ? Map.of() : fieldSnapshots;
        exceptionSuperclassSimpleNames = exceptionSuperclassSimpleNames == null
                ? List.of() : exceptionSuperclassSimpleNames;
        objectFieldSnapshots = objectFieldSnapshots == null ? Map.of() : objectFieldSnapshots;
        equalsNamed = equalsNamed == null ? Map.of() : equalsNamed;
    }

    public static InvocationOutcome normal(Object returnValue,
                                           String stdout,
                                           boolean stdoutTruncated,
                                           Map<String, Object> fieldSnapshots) {
        return normal(returnValue, stdout, stdoutTruncated, fieldSnapshots, null, Map.of(), Map.of());
    }

    public static InvocationOutcome normal(Object returnValue,
                                           String stdout,
                                           boolean stdoutTruncated,
                                           Map<String, Object> fieldSnapshots,
                                           String objectTypeSimpleName,
                                           Map<String, Object> objectFieldSnapshots,
                                           Map<String, Boolean> equalsNamed) {
        return new InvocationOutcome(
                InvocationOutcomeKind.NORMAL,
                returnValue,
                stdout,
                stdoutTruncated,
                fieldSnapshots,
                null,
                List.of(),
                null,
                objectTypeSimpleName,
                objectFieldSnapshots,
                equalsNamed);
    }

    public static InvocationOutcome threw(String stdout,
                                          boolean stdoutTruncated,
                                          String exceptionSimpleName,
                                          List<String> exceptionSuperclassSimpleNames) {
        return threw(stdout, stdoutTruncated, exceptionSimpleName, exceptionSuperclassSimpleNames, Map.of());
    }

    public static InvocationOutcome threw(String stdout,
                                          boolean stdoutTruncated,
                                          String exceptionSimpleName,
                                          List<String> exceptionSuperclassSimpleNames,
                                          Map<String, Object> fieldSnapshots) {
        return new InvocationOutcome(
                InvocationOutcomeKind.THREW,
                null,
                stdout,
                stdoutTruncated,
                fieldSnapshots,
                exceptionSimpleName,
                exceptionSuperclassSimpleNames,
                null,
                null,
                Map.of(),
                Map.of());
    }

    public static InvocationOutcome timedOut(String stdout) {
        return new InvocationOutcome(InvocationOutcomeKind.TIMED_OUT, null, stdout, false, Map.of(),
                null, List.of(), "Invocation timed out", null, Map.of(), Map.of());
    }

    public static InvocationOutcome error(String message) {
        return new InvocationOutcome(InvocationOutcomeKind.ERROR, null, "", false, Map.of(),
                null, List.of(), message, null, Map.of(), Map.of());
    }
}
