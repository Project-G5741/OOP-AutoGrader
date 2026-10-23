package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsDashboardResponse {

    public final BigDecimal overallAverage;
    public final String lowestAverageLab;
    public final BigDecimal lowestAverageScore;
    public final String mostDifficultTopic;
    public final List<LabTrendItem> labTrend;
    public final List<StudentOverviewItem> studentOverview;
    public final List<AtRiskLabItem> atRiskLabs;
    public final List<AtRiskStudentItem> atRiskStudents;
    public final AiSummary aiSummary;

    public AnalyticsDashboardResponse(
            BigDecimal overallAverage,
            String lowestAverageLab,
            BigDecimal lowestAverageScore,
            String mostDifficultTopic,
            List<LabTrendItem> labTrend,
            List<StudentOverviewItem> studentOverview,
            List<AtRiskLabItem> atRiskLabs,
            List<AtRiskStudentItem> atRiskStudents,
            AiSummary aiSummary) {
        this.overallAverage = overallAverage;
        this.lowestAverageLab = lowestAverageLab;
        this.lowestAverageScore = lowestAverageScore;
        this.mostDifficultTopic = mostDifficultTopic;
        this.labTrend = labTrend;
        this.studentOverview = studentOverview;
        this.atRiskLabs = atRiskLabs;
        this.atRiskStudents = atRiskStudents;
        this.aiSummary = aiSummary;
    }

    public static record LabTrendItem(
            String labId,
            String labName,
            BigDecimal averageScore,
            long submissionCount) {
    }

    public static record StudentOverviewItem(
            String studentId,
            String studentName,
            String studentCode,
            BigDecimal overallAverage,
            int completedLabs,
            String status) {
    }

    public static record AtRiskLabItem(
            String labId,
            String labName,
            String lowestChallenge,
            BigDecimal averageScore,
            String reason,
            List<ResourceLink> recommendedResources) {
    }

    public static record AtRiskStudentItem(
            String studentId,
            String studentName,
            BigDecimal currentAverage,
            List<String> weakSkills,
            String improvementSuggestions) {
    }

    public static record AiSummary(
            String title,
            String details,
            List<ResourceLink> recommendedResources) {
    }

    public static record ResourceLink(String title, String url) {
    }
}
