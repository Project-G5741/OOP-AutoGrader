package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record ConstructorRubric(
        UUID id,
        String scope,
        boolean isDefault,
        List<String> parameterTypes,
        List<ParameterRubric> parameters) {

    public ConstructorRubric {
        parameterTypes = List.copyOf(parameterTypes);
        parameters = List.copyOf(parameters);
    }

    public ConstructorRubric(UUID id, String scope, boolean isDefault, List<String> parameterTypes) {
        this(
                id,
                scope,
                isDefault,
                parameterTypes,
                parameterTypes.stream().map(type -> new ParameterRubric("", type)).toList());
    }
}
