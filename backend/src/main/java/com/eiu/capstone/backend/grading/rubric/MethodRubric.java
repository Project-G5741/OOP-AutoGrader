package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record MethodRubric(
        UUID id,
        List<String> names,
        String scope,
        List<String> returnTypes,
        boolean isStatic,
        boolean isAbstract,
        boolean isFinal,
        List<ParameterRubric> parameters) {

    public MethodRubric {
        names = List.copyOf(names);
        returnTypes = List.copyOf(returnTypes);
        parameters = List.copyOf(parameters);
        if (names.isEmpty() || returnTypes.isEmpty()) {
            throw new IllegalArgumentException("Method rubric alias lists must not be empty");
        }
    }

    public MethodRubric(
            UUID id,
            String name,
            String scope,
            String returnType,
            boolean isStatic,
            boolean isAbstract,
            boolean isFinal,
            List<String> parameterTypes) {
        this(
                id,
                List.of(name),
                scope,
                List.of(returnType),
                isStatic,
                isAbstract,
                isFinal,
                parameterTypes.stream().map(type -> new ParameterRubric("", type)).toList());
    }

    public String name() {
        return names.get(0);
    }

    public String returnType() {
        return returnTypes.get(0);
    }

    public List<String> parameterTypes() {
        return parameters.stream().map(ParameterRubric::dataType).toList();
    }
}
