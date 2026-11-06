package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record ConstructorStructureDTO(
        UUID id,
        String name,
        Integer scopeId,
        boolean isDefault,
        List<ParameterStructureDTO> parameters) {}
