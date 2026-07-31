package com.eiu.capstone.backend.grading.rubric;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.eiu.capstone.backend.model.Parameter;

final class RubricParameterMaps {

    private RubricParameterMaps() {}

    static Map<UUID, List<String>> byMethod(List<Parameter> params) {
        return groupByOwner(params, p -> p.getMethod().getId());
    }

    static Map<UUID, List<String>> byConstructor(List<Parameter> params) {
        return groupByOwner(params, p -> p.getConstructorEntity().getId());
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
}
