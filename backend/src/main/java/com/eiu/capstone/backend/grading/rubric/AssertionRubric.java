package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
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
        int orderIndex,
        List<String> fieldNameAliases) {

    public AssertionRubric {
        fieldNameAliases = fieldNameAliases == null || fieldNameAliases.isEmpty()
                ? (fieldName == null ? List.of() : List.of(fieldName))
                : List.copyOf(fieldNameAliases);
    }

    public AssertionRubric(
            UUID id,
            AssertionKind kind,
            UUID invocationId,
            UUID fieldId,
            String fieldName,
            String fieldDataType,
            String expectedValueJson,
            ComparisonMode comparisonMode,
            int orderIndex) {
        this(
                id, kind, invocationId, fieldId, fieldName, fieldDataType,
                expectedValueJson, comparisonMode, orderIndex, null);
    }
}
