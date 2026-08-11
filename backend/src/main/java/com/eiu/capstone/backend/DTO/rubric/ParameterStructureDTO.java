package com.eiu.capstone.backend.DTO.rubric;

import java.util.UUID;

public record ParameterStructureDTO(UUID id, String name, String dataType, int orderIndex, boolean isFinal) {}
