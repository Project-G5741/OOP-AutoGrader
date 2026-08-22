package com.eiu.capstone.backend.DTO;

import java.util.UUID;

/**
 * Matches the frontend's `{ id, challengeNumber, name, score, hasMmd }` shape used in the
 * challenges sidebar. `score` is null when the student hasn't submitted anything for
 * this challenge yet — the frontend renders "Not submitted" in that case.
 */
public record ChallengeDTO(
        UUID id,
        int challengeNumber,
        String name,
        Integer score,
        int weight,
        int classWeight,
        int mmdWeight,
        boolean hasMmd) {

    public ChallengeDTO(UUID id, int challengeNumber, String name, Integer score) {
        this(id, challengeNumber, name, score, 1, 1, 1, true);
    }
}
