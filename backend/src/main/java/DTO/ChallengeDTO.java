package com.eiu.capstone.backend.DTO;

import java.util.UUID;

/**
 * Matches the frontend's {@code { id, challengeNumber, name, score, hasMmd, hasClass }} shape
 * used in the challenges sidebar. {@code score} is null when the student hasn't submitted
 * anything for this challenge yet — the frontend renders "Not submitted" in that case.
 */
public record ChallengeDTO(
        UUID id,
        int challengeNumber,
        String name,
        Integer score,
        int weight,
        int classWeight,
        int mmdWeight,
        int testcaseWeight,
        boolean hasMmd,
        boolean hasClass) {

    public ChallengeDTO(UUID id, int challengeNumber, String name, Integer score) {
        this(id, challengeNumber, name, score, 1, 1, 1, 1, true, true);
    }

    /** Legacy full ctor (pre-{@code hasClass}); Declaration defaults on. */
    public ChallengeDTO(UUID id,
                        int challengeNumber,
                        String name,
                        Integer score,
                        int weight,
                        int classWeight,
                        int mmdWeight,
                        int testcaseWeight,
                        boolean hasMmd) {
        this(id, challengeNumber, name, score, weight, classWeight, mmdWeight, testcaseWeight, hasMmd, true);
    }
}
