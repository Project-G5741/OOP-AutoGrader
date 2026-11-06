package com.eiu.capstone.backend.DTO.rubric;

/**
 * One challenge outcome from lecturer solution-folder import (derive only; no persist).
 */
public record SolutionImportChallengeResult(
        int challengeNumber,
        String status,
        String message,
        ChallengeStructureDTO challenge) {

    public static SolutionImportChallengeResult applied(int challengeNumber, ChallengeStructureDTO challenge) {
        return new SolutionImportChallengeResult(challengeNumber, "applied", null, challenge);
    }

    public static SolutionImportChallengeResult skipped(int challengeNumber, String message) {
        return new SolutionImportChallengeResult(challengeNumber, "skipped", message, null);
    }
}
