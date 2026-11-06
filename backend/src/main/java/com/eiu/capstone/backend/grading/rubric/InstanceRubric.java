package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record InstanceRubric(
        UUID id,
        String label,
        UUID constructorId,
        String className,
        List<String> parameterTypes,
        String paramsJson) {}
