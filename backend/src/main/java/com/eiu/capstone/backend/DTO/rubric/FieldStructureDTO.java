package com.eiu.capstone.backend.DTO.rubric;

import java.util.UUID;

public record FieldStructureDTO(UUID id, String name, String dataType, Integer scopeId) {}
