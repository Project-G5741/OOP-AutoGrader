package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

public record RelationRubric(
        UUID id,
        UUID sourceClassId,
        String sourceClassName,
        UUID targetClassId,
        String targetClassName,
        String relationTypeName) {}
