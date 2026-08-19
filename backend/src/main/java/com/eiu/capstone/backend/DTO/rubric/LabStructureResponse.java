package com.eiu.capstone.backend.DTO.rubric;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LabStructureResponse(
        UUID id,
        String name,
        UUID termId,
        LocalDate deadlineDate,
        List<ChallengeStructureDTO> challenges) {}
