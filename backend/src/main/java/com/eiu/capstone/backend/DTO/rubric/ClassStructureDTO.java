package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record ClassStructureDTO(
        UUID id,
        String name,
        Integer scopeId,
        Integer declaringTypeId,
        boolean isAbstract,
        boolean isStatic,
        List<FieldStructureDTO> fields,
        List<MethodStructureDTO> methods,
        List<ConstructorStructureDTO> constructors,
        UUID outerClassId,
        int weight) {

    public ClassStructureDTO(UUID id,
                             String name,
                             Integer scopeId,
                             Integer declaringTypeId,
                             boolean isAbstract,
                             List<FieldStructureDTO> fields,
                             List<MethodStructureDTO> methods,
                             List<ConstructorStructureDTO> constructors) {
        this(id, name, scopeId, declaringTypeId, isAbstract, false, fields, methods, constructors, null, 1);
    }

    public ClassStructureDTO(UUID id,
                             String name,
                             Integer scopeId,
                             Integer declaringTypeId,
                             boolean isAbstract,
                             List<FieldStructureDTO> fields,
                             List<MethodStructureDTO> methods,
                             List<ConstructorStructureDTO> constructors,
                             int weight) {
        this(id, name, scopeId, declaringTypeId, isAbstract, false, fields, methods, constructors, null, weight);
    }
}
