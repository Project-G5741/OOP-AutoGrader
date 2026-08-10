package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

import com.eiu.capstone.backend.model.TestcaseCheckType;
import com.eiu.capstone.backend.model.TestcaseTargetType;

public record TestcaseRubric(
        UUID id,
        String name,
        TestcaseCheckType checkType,
        TestcaseTargetType targetType,
        UUID targetId,
        int weight,
        int orderIndex) {}
