package com.eiu.capstone.backend.service;

/**
 * Next {@code lab_submission.attempt_number} for a student+lab.
 * Each successful upload is a new attempt; the client path number is not trusted.
 */
public final class SubmissionAttemptNumbers {

    private SubmissionAttemptNumbers() {}

    public static int next(Integer maxExisting) {
        if (maxExisting == null || maxExisting < 1) {
            return 1;
        }
        return maxExisting + 1;
    }
}
