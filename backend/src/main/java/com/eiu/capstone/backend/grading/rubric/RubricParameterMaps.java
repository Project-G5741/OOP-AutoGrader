package com.eiu.capstone.backend.grading.rubric;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.eiu.capstone.backend.model.Parameter;

public final class RubricParameterMaps {

    private RubricParameterMaps() {}

    public static Map<UUID, List<String>> byMethod(List<Parameter> params) {
        return groupByOwner(params, p -> p.getMethod().getId());
    }

    public static Map<UUID, List<String>> byConstructor(List<Parameter> params) {
        return groupByOwner(params, p -> p.getConstructorEntity().getId());
    }

    public static Map<UUID, List<ParameterRubric>> aliasesByMethod(
            List<Parameter> params,
            Map<UUID, List<String>> namesByParameter,
            Map<UUID, List<String>> typesByParameter) {
        return groupAliasesByOwner(params, p -> p.getMethod().getId(), namesByParameter, typesByParameter);
    }

    public static Map<UUID, List<ParameterRubric>> aliasesByConstructor(
            List<Parameter> params,
            Map<UUID, List<String>> namesByParameter,
            Map<UUID, List<String>> typesByParameter) {
        return groupAliasesByOwner(
                params, p -> p.getConstructorEntity().getId(), namesByParameter, typesByParameter);
    }

    private static Map<UUID, List<String>> groupByOwner(
            List<Parameter> params, Function<Parameter, UUID> ownerId) {
        return params.stream()
                .collect(Collectors.groupingBy(ownerId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                                .map(Parameter::getDataType)
                                .collect(Collectors.toList())));
    }

    private static Map<UUID, List<ParameterRubric>> groupAliasesByOwner(
            List<Parameter> params,
            Function<Parameter, UUID> ownerId,
            Map<UUID, List<String>> namesByParameter,
            Map<UUID, List<String>> typesByParameter) {
        return params.stream()
                .collect(Collectors.groupingBy(ownerId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                                .map(parameter -> new ParameterRubric(
                                        namesByParameter.getOrDefault(
                                                parameter.getId(), List.of(parameter.getName())),
                                        typesByParameter.getOrDefault(
                                                parameter.getId(), List.of(parameter.getDataType()))))
                                .toList()));
    }
}
