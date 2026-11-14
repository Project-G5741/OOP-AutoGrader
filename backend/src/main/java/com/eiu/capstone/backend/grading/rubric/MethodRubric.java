package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public static MethodRubric fromJson(
            @JsonProperty("id") UUID id,
            @JsonProperty("names") List<String> names,
            @JsonProperty("name") String name,
            @JsonProperty("scope") String scope,
            @JsonProperty("returnTypes") List<String> returnTypes,
            @JsonProperty("returnType") String returnType,
            @JsonProperty("isStatic") boolean isStatic,
            @JsonProperty("isAbstract") boolean isAbstract,
            @JsonProperty("isFinal") boolean isFinal,
            @JsonProperty("parameters") List<ParameterRubric> parameters,
            @JsonProperty("parameterTypes") List<String> parameterTypes) {
        List<ParameterRubric> resolvedParameters = parameters != null
                ? parameters
                : (parameterTypes == null ? List.of() : parameterTypes.stream()
                        .map(type -> new ParameterRubric("", type))
                        .toList());
        return new MethodRubric(
                id,
                names == null || names.isEmpty() ? List.of(name) : names,
                scope,
                returnTypes == null || returnTypes.isEmpty() ? List.of(returnType) : returnTypes,
                isStatic,
                isAbstract,
                isFinal,
                resolvedParameters);
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
