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
        List<ParameterStructureDTO> parameters,
        List<String> names,
        List<String> returnTypes) {

    public MethodStructureDTO {
        if (names != null) {
            names = List.copyOf(names);
            if (!names.isEmpty()) {
                name = names.get(0);
            }
        }
        if (returnTypes != null) {
            returnTypes = List.copyOf(returnTypes);
            if (!returnTypes.isEmpty()) {
                returnType = returnTypes.get(0);
            }
        }
    }

    /** Legacy single-value clients remain accepted while ordered lists are rolled out. */
    public MethodStructureDTO(
            UUID id,
            String name,
            String returnType,
            Integer scopeId,
            boolean isStatic,
            boolean isAbstract,
            List<ParameterStructureDTO> parameters) {
        this(id, name, returnType, scopeId, isStatic, isAbstract, parameters, null, null);
    }
}
