package com.eiu.capstone.backend.grading.rubric;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record LabRubricSnapshot(UUID labId, Map<Integer, ChallengeRubric> byChallengeNumber) {

    public Optional<ChallengeRubric> challenge(int challengeNumber) {
        return Optional.ofNullable(byChallengeNumber.get(challengeNumber));
    }

    public Optional<ChallengeRubric> challengeById(UUID challengeId) {
        if (challengeId == null || byChallengeNumber == null) {
            return Optional.empty();
        }
        for (ChallengeRubric challenge : byChallengeNumber.values()) {
            if (challenge != null && challengeId.equals(challenge.challengeId())) {
                return Optional.of(challenge);
            }
        }
        return Optional.empty();
    }
}
