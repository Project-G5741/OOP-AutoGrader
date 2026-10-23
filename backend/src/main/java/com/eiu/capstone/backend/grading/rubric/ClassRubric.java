package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record ClassRubric(
        UUID id,
        String name,
        String outerClassName,
        String scope,
        String declaringType,
        boolean isAbstract,
        boolean isStatic,
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
        this(id, name, null, scope, declaringType, isAbstract, false, fields, methods, constructors, 1);
    }

    public ClassRubric(UUID id,
                       String name,
                       String scope,
                       String declaringType,
                       boolean isAbstract,
                       List<FieldRubric> fields,
                       List<MethodRubric> methods,
                       List<ConstructorRubric> constructors,
                       int weight) {
        this(id, name, null, scope, declaringType, isAbstract, false, fields, methods, constructors, weight);
    }

    public boolean isNested() {
        return outerClassName != null && !outerClassName.isBlank();
    }

    public String qualifiedName() {
        if (outerClassName == null || outerClassName.isBlank()) {
            return name;
        }
        return outerClassName + "." + name;
    }
}
