package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record ParameterStructureDTO(
        UUID id,
        String name,
        String dataType,
        int orderIndex,
        boolean isFinal,
        List<String> names,
        List<String> dataTypes) {

    public ParameterStructureDTO {
        if (names != null) {
            names = List.copyOf(names);
            if (!names.isEmpty()) {
                name = names.get(0);
            }
        }
        if (dataTypes != null) {
            dataTypes = List.copyOf(dataTypes);
            if (!dataTypes.isEmpty()) {
                dataType = dataTypes.get(0);
            }
        }
    }

    /** Legacy single-value clients remain accepted while ordered lists are rolled out. */
    public ParameterStructureDTO(UUID id, String name, String dataType, int orderIndex, boolean isFinal) {
        this(id, name, dataType, orderIndex, isFinal, null, null);
    }
}
