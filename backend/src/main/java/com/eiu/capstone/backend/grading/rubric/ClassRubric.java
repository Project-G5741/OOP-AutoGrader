package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record ClassRubric(
        UUID id,
        String name,
        String scope,
        String declaringType,
        boolean isAbstract,
        List<FieldRubric> fields,
        List<MethodRubric> methods,
        List<ConstructorRubric> constructors,
        int weight) {

    public ClassRubric(UUID id,
                       String name,
                       String scope,
                       String declaringType,
                       boolean isAbstract,
                       List<FieldRubric> fields,
                       List<MethodRubric> methods,
                       List<ConstructorRubric> constructors) {
        this(id, name, scope, declaringType, isAbstract, fields, methods, constructors, 1);
    }
}
