package com.eiu.capstone.backend.DTO.rubric;

import java.util.UUID;

public record RelationStructureDTO(
        UUID id,
        UUID sourceClassId,
        UUID targetClassId,
        Integer relationTypeId) {}
