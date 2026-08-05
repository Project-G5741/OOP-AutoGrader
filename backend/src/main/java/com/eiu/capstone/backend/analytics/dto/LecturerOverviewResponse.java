package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record LecturerOverviewResponse(
        long totalStudents,
        long totalLabs,
        BigDecimal averageScore,
        long atRiskStudents,
        List<RecentSubmissionItem> recentSubmissions,
        long activeStudents) {

    public record RecentSubmissionItem(
            String studentName,
            String studentCode,
            String labName,
            BigDecimal score,
            int attempt,
            String submittedAt) {
    }
}
