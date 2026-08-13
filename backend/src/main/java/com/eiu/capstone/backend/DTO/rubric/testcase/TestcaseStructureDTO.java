package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.TestcaseComparisonMethod;
import com.eiu.capstone.backend.model.TestcaseType;

public record TestcaseStructureDTO(
        UUID id,
        String name,
        TestcaseType testcaseType,
        TestcaseComparisonMethod comparisonMethod,
        int weight,
        int orderIndex,
        boolean hidden,
        InvocationStructureDTO invocation,
        List<InstanceStructureDTO> instances,
        List<AssertionStructureDTO> assertions) {}
