package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.UUID;

import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;

public record AssertionStructureDTO(
        UUID id,
        UUID invocationId,
        AssertionKind assertionKind,
        UUID fieldId,
        String expectedValue,
        ComparisonMode comparisonMode,
        int orderIndex) {}
