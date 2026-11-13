package com.eiu.capstone.backend.grading.rubric;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParameterRubric(List<String> names, List<String> dataTypes) {

    public ParameterRubric {
        names = List.copyOf(names);
        dataTypes = List.copyOf(dataTypes);
        if (names.isEmpty() || dataTypes.isEmpty()) {
            throw new IllegalArgumentException("Parameter rubric alias lists must not be empty");
        }
    }

    @JsonCreator
    public static ParameterRubric fromJson(
            @JsonProperty("names") List<String> names,
            @JsonProperty("name") String name,
            @JsonProperty("dataTypes") List<String> dataTypes,
            @JsonProperty("dataType") String dataType) {
        return new ParameterRubric(
                names == null || names.isEmpty() ? List.of(name) : names,
                dataTypes == null || dataTypes.isEmpty() ? List.of(dataType) : dataTypes);
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
