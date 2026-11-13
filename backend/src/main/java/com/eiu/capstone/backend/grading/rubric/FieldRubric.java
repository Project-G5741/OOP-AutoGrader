package com.eiu.capstone.backend.grading.rubric;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public static FieldRubric fromJson(
            @JsonProperty("id") UUID id,
            @JsonProperty("names") List<String> names,
            @JsonProperty("name") String name,
            @JsonProperty("scope") String scope,
            @JsonProperty("dataTypes") List<String> dataTypes,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("isStatic") boolean isStatic,
            @JsonProperty("isFinal") boolean isFinal) {
        List<String> resolvedNames = names == null || names.isEmpty()
                ? List.of(name)
                : names;
        List<String> resolvedDataTypes = dataTypes == null || dataTypes.isEmpty()
                ? Collections.singletonList(dataType)
                : dataTypes;
        return new FieldRubric(id, resolvedNames, scope, resolvedDataTypes, isStatic, isFinal);
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
