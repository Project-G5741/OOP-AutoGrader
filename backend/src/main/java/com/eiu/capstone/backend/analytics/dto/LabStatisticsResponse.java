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
        /** Sum of {@code student_lab_progress.attempts_count} for this lab (lifetime uploads per student). */
        long totalAttempts,
        long studentCount,
        long studentsSubmitted,
        BigDecimal completionRate,
        BigDecimal plagiarismRate,
        List<GradeDistributionBucket> gradeDistribution) {

    public record GradeDistributionBucket(String range, long count) {
    }
}
