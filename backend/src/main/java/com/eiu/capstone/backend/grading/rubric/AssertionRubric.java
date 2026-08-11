package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;

public record AssertionRubric(
        UUID id,
        AssertionKind kind,
        UUID invocationId,
        UUID fieldId,
        String fieldName,
        String fieldDataType,
        String expectedValueJson,
        ComparisonMode comparisonMode,
        int orderIndex) {}
