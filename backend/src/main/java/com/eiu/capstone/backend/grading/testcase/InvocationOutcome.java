package com.eiu.capstone.backend.grading.testcase;

public record InvocationOutcome(
        InvocationOutcomeKind kind,
        Object instance,
        Object returnValue,
        String stdout,
        Throwable caughtException,
        String errorMessage) {

    public static InvocationOutcome normal(Object instance, Object returnValue, String stdout) {
        return new InvocationOutcome(InvocationOutcomeKind.NORMAL, instance, returnValue, stdout, null, null);
    }

    public static InvocationOutcome threw(Object instance, Throwable exception, String stdout) {
        return new InvocationOutcome(InvocationOutcomeKind.THREW, instance, null, stdout, exception, null);
    }

    public static InvocationOutcome timedOut(String stdout) {
        return new InvocationOutcome(InvocationOutcomeKind.TIMED_OUT, null, null, stdout, null,
                "Invocation timed out");
    }

    public static InvocationOutcome error(String message) {
        return new InvocationOutcome(InvocationOutcomeKind.ERROR, null, null, "", null, message);
    }
}
