package com.eiu.capstone.backend.DTO.rubric;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * PUT /structure body: same shape as {@link LabStructureResponse} plus optional
 * import-replaced challenge ids for in-TX OT wipe + replace-aware rebuild.
 */
public record LabStructureSaveRequest(
        UUID id,
        String name,
        UUID termId,
        LocalDate deadlineDate,
        boolean studentVisible,
        LocalDate releaseDate,
        List<ChallengeStructureDTO> challenges,
        List<UUID> replacedChallengeIds) {

    public LabStructureResponse toStructure() {
        return new LabStructureResponse(id, name, termId, deadlineDate, studentVisible, releaseDate, challenges);
    }

    public List<UUID> replacedChallengeIdsOrEmpty() {
        return replacedChallengeIds != null ? replacedChallengeIds : List.of();
    }
}
