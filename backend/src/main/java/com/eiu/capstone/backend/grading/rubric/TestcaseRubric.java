package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.OopPrincipleTag;
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
        List<AssertionRubric> assertions,
        List<InvocationRubric> invocations,
        OopPrincipleTag oopPrincipleTag) {

    public TestcaseRubric {
        invocations = invocations == null ? List.of() : List.copyOf(invocations);
        oopPrincipleTag = oopPrincipleTag == null ? OopPrincipleTag.Unit : oopPrincipleTag;
    }

    public TestcaseRubric(
            UUID id,
            String name,
            TestcaseType testcaseType,
            TestcaseComparisonMethod comparisonMethod,
            int weight,
            int orderIndex,
            boolean hidden,
            InvocationRubric invocation,
            List<InstanceRubric> instances,
            List<AssertionRubric> assertions) {
        this(
                id,
                name,
                testcaseType,
                comparisonMethod,
                weight,
                orderIndex,
                hidden,
                invocation,
                instances,
                assertions,
                invocation == null ? List.of() : List.of(invocation),
                OopPrincipleTag.Unit);
    }
}
