package com.eiu.capstone.backend.grading.rubric;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record FieldRubric(
        UUID id,
        List<String> names,
        String scope,
        List<String> dataTypes,
        boolean isStatic,
        boolean isFinal) {

    public FieldRubric {
        names = List.copyOf(names);
        dataTypes = dataTypes.size() == 1 && dataTypes.get(0) == null
                ? Collections.singletonList(null)
                : List.copyOf(dataTypes);
        if (names.isEmpty() || dataTypes.isEmpty()) {
            throw new IllegalArgumentException("Field rubric alias lists must not be empty");
        }
    }

    public FieldRubric(
            UUID id,
            String name,
            String scope,
            String dataType,
            boolean isStatic,
            boolean isFinal) {
        this(id, List.of(name), scope, Collections.singletonList(dataType), isStatic, isFinal);
    }

    public String name() {
        return names.get(0);
    }

    public String dataType() {
        return dataTypes.get(0);
    }
}
