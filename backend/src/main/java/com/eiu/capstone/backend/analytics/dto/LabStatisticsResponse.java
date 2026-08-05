package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LabStatisticsResponse(
        UUID labId,
        String labName,
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        long submissionCount,
        long studentCount,
        BigDecimal completionRate,
        List<GradeDistributionBucket> gradeDistribution) {

    public record GradeDistributionBucket(String range, long count) {
    }
}
