package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record MethodRubric(
        UUID id,
        String name,
        String scope,
        String returnType,
        boolean isStatic,
        boolean isAbstract,
        boolean isFinal,
        List<String> parameterTypes) {}
