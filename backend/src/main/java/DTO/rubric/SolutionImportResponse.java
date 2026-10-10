package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record SolutionImportResponse(
        UUID labId,
        List<SolutionImportChallengeResult> challenges) {}
