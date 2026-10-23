package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.List;
import java.util.UUID;

public record ChallengeTestcasesResponse(
        UUID labId,
        UUID challengeId,
        List<TestcaseStructureDTO> testcases) {}
