package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.UUID;

public record InstanceStructureDTO(
        UUID id,
        String label,
        UUID constructorId,
        String params) {}
