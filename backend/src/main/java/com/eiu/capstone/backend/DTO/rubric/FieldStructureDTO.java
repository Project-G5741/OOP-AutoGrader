package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record FieldStructureDTO(
        UUID id,
        String name,
        String dataType,
        Integer scopeId,
        boolean isStatic,
        boolean isFinal,
        List<String> names,
        List<String> dataTypes) {

    public FieldStructureDTO {
        if (names != null) {
            names = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(names));
            if (!names.isEmpty()) {
                name = names.get(0);
            }
        }
        if (dataTypes != null) {
            dataTypes = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(dataTypes));
            if (!dataTypes.isEmpty()) {
                dataType = dataTypes.get(0);
            }
        }
    }

    /** Legacy single-value clients remain accepted while ordered lists are rolled out. */
    public FieldStructureDTO(
            UUID id,
            String name,
            String dataType,
            Integer scopeId,
            boolean isStatic,
            boolean isFinal) {
        this(id, name, dataType, scopeId, isStatic, isFinal, null, null);
    }
}
