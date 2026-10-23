package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record MethodStructureDTO(
        UUID id,
        String name,
        String returnType,
        Integer scopeId,
        boolean isStatic,
        boolean isAbstract,
        List<ParameterStructureDTO> parameters) {}
