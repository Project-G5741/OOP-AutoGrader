package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record LecturerOverviewResponse(
        long totalStudents,
        long totalLabs,
        BigDecimal averageScore,
        long atRiskStudents,
        List<RecentSubmissionItem> recentSubmissions,
        long activeStudents,
        List<OverviewStudentItem> students,
        List<OverviewLabItem> labs,
        List<OverviewScoreItem> scoreRows) {

    public record RecentSubmissionItem(
            String studentName,
            String studentCode,
            String labName,
            BigDecimal score,
            int attempt,
            String submittedAt) {
    }

    /** Enrolled active student. {@code atRisk} is total score &lt; 70, with missing labs as 0. */
    public record OverviewStudentItem(
            String studentId,
            String studentName,
            String studentCode,
            BigDecimal totalScore,
            boolean atRisk) {
    }

    public record OverviewLabItem(
            String labId,
            String labName,
            BigDecimal averageScore,
            long studentsSubmitted) {
    }

    /** One qualifying best score. The overview average is the mean of these rows. */
    public record OverviewScoreItem(
            String studentId,
            String studentName,
            String studentCode,
            String labId,
            String labName,
            BigDecimal score) {
    }
}
