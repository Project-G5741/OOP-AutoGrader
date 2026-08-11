package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;
import com.eiu.capstone.backend.model.TestcaseType;

public record TestcaseRubric(
        UUID id,
        String name,
        TestcaseType testcaseType,
        TestcaseComparisonMethod comparisonMethod,
        int weight,
        int orderIndex,
        boolean hidden,
        InvocationRubric invocation,
        List<InstanceRubric> instances,
        List<AssertionRubric> assertions) {}
