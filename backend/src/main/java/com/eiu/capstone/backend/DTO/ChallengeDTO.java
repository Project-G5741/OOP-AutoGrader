package com.eiu.capstone.backend.DTO;

import java.util.UUID;

/**
 * Matches the frontend's `{ id, name, score }` shape used in the challenges
 * sidebar. `score` is null when the student hasn't submitted anything for
 * this challenge yet — the frontend renders "Not submitted" in that case.
 */
public record ChallengeDTO(UUID id, String name, Integer score) {
}