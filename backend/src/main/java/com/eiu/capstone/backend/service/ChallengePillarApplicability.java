package com.eiu.capstone.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ensures a challenge keeps at least one applicable grading pillar.
 */
public final class ChallengePillarApplicability {

    private ChallengePillarApplicability() {}

    public static void requireAtLeastOne(boolean hasClass,
                                         boolean hasMmd,
                                         int operationalTestcaseCount,
                                         String challengeLabel) {
        if (hasClass || hasMmd || operationalTestcaseCount > 0) {
            return;
        }
        String name = challengeLabel != null && !challengeLabel.isBlank()
                ? challengeLabel.trim()
                : "Challenge";
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "\"" + name + "\" must keep at least one scoring pillar "
                        + "(Requires Declaration Test, Requires MMD diagram, or operational testcases).");
    }
}
