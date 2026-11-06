package com.eiu.capstone.backend.service;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Persisted compile diagnostics for one challenge. {@code catastrophic} is I/O or
 * setup failure and gates the whole challenge. Mixed javac uses {@code byClassName}.
 */
public record ChallengeCompileErrors(
        String catastrophic,
        Map<String, String> byClassName) {

    public ChallengeCompileErrors {
        if (catastrophic != null && catastrophic.isBlank()) {
            catastrophic = null;
        }
        byClassName = byClassName == null ? Map.of() : Map.copyOf(byClassName);
    }

    public static ChallengeCompileErrors none() {
        return new ChallengeCompileErrors(null, Map.of());
    }

    public static ChallengeCompileErrors catastrophic(String message) {
        if (message == null || message.isBlank()) {
            return none();
        }
        return new ChallengeCompileErrors(message, Map.of());
    }

    public static ChallengeCompileErrors perClass(Map<String, String> byClassName) {
        if (byClassName == null || byClassName.isEmpty()) {
            return none();
        }
        return new ChallengeCompileErrors(null, byClassName);
    }

    public static ChallengeCompileErrors fromChallengeResult(SubmissionStorageService.ChallengeResult folder) {
        if (folder == null) {
            return none();
        }
        if (folder.compileError != null && !folder.compileError.isBlank()) {
            return catastrophic(folder.compileError);
        }
        return perClass(folder.compileErrorsByClassName);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return catastrophic == null && byClassName.isEmpty();
    }

    public String messageForClass(String... names) {
        if (catastrophic != null) {
            return catastrophic;
        }
        for (String name : names) {
            if (name != null && byClassName.containsKey(name)) {
                return byClassName.get(name);
            }
        }
        return null;
    }
}
