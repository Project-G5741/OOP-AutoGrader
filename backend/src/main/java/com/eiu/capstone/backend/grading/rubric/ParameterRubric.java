package com.eiu.capstone.backend.grading.rubric;

import java.util.List;

public record ParameterRubric(List<String> names, List<String> dataTypes) {

    public ParameterRubric {
        names = List.copyOf(names);
        dataTypes = List.copyOf(dataTypes);
        if (names.isEmpty() || dataTypes.isEmpty()) {
            throw new IllegalArgumentException("Parameter rubric alias lists must not be empty");
        }
    }

    public ParameterRubric(String name, String dataType) {
        this(List.of(name), List.of(dataType));
    }

    public String name() {
        return names.get(0);
    }

    public String dataType() {
        return dataTypes.get(0);
    }
}
