package com.eiu.capstone.backend.grading.rubric;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record LabRubricSnapshot(UUID labId, Map<Integer, ChallengeRubric> byChallengeNumber) {

    public Optional<ChallengeRubric> challenge(int challengeNumber) {
        return Optional.ofNullable(byChallengeNumber.get(challengeNumber));
    }
}
