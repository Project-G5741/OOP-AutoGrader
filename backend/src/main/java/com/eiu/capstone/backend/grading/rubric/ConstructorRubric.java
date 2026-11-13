package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public static ConstructorRubric fromJson(
            @JsonProperty("id") UUID id,
            @JsonProperty("scope") String scope,
            @JsonProperty("isDefault") boolean isDefault,
            @JsonProperty("parameterTypes") List<String> parameterTypes,
            @JsonProperty("parameters") List<ParameterRubric> parameters) {
        List<String> resolvedTypes = parameterTypes != null
                ? parameterTypes
                : (parameters == null ? List.of() : parameters.stream()
                        .map(ParameterRubric::dataType)
                        .toList());
        List<ParameterRubric> resolvedParameters = parameters != null
                ? parameters
                : resolvedTypes.stream().map(type -> new ParameterRubric("", type)).toList();
        return new ConstructorRubric(id, scope, isDefault, resolvedTypes, resolvedParameters);
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
