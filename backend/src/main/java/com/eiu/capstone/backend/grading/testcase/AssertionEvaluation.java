package com.eiu.capstone.backend.grading.testcase;

import java.util.UUID;

import com.eiu.capstone.backend.model.TestcaseResultStatus;

public record AssertionEvaluation(
        UUID assertionId,
        TestcaseResultStatus status,
        String actualValueJson,
        String feedback) {}
