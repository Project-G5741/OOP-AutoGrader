package com.eiu.capstone.backend.analytics.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.StudentReportResponse;
import com.eiu.capstone.backend.analytics.mapper.AnalyticsMapper;
import com.eiu.capstone.backend.analytics.repository.AnalyticsRepository;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AnalyticsDashboardResponse getDashboard(UUID academicYearId, UUID semesterId, UUID labId, String course) {
        Object[] summary = safeFindDashboardSummary(labId, semesterId, academicYearId, course);
        BigDecimal overallAverage = null;
        BigDecimal lowestAverageScore = null;
        String lowestAverageLab = null;
        if (summary != null) {
            overallAverage = AnalyticsMapper.toBigDecimal(summary[0]);
            lowestAverageScore = AnalyticsMapper.toBigDecimal(summary[1]);
            lowestAverageLab = AnalyticsMapper.toString(summary[2]);
        }

        List<AnalyticsDashboardResponse.LabTrendItem> labTrend = safeFindLabTrend(labId, semesterId, academicYearId, course);

        List<AnalyticsDashboardResponse.StudentOverviewItem> studentOverview = new ArrayList<>();
        for (Object[] row : safeFindStudentOverview(labId, semesterId, academicYearId, null, "overallAverage", "desc", 0, 5)) {
            AnalyticsDashboardResponse.StudentOverviewItem item = AnalyticsMapper.toStudentOverviewItem(row);
            if (item != null) studentOverview.add(item);
        }

        List<AnalyticsDashboardResponse.AtRiskLabItem> atRiskLabs = new ArrayList<>();
        for (Object[] row : safeFindAtRiskLabs(academicYearId, semesterId)) {
            AnalyticsDashboardResponse.AtRiskLabItem item = AnalyticsMapper.toAtRiskLabItem(row);
            if (item != null) atRiskLabs.add(item);
        }

        List<AnalyticsDashboardResponse.AtRiskStudentItem> atRiskStudents = new ArrayList<>();
        for (Object[] row : safeFindAtRiskStudents(academicYearId, semesterId)) {
            AnalyticsDashboardResponse.AtRiskStudentItem item = AnalyticsMapper.toAtRiskStudentItem(row);
            if (item != null) atRiskStudents.add(item);
        }

        AnalyticsDashboardResponse.AiSummary aiSummary = new AnalyticsDashboardResponse.AiSummary(
                "AI trends still coming soon",
                "This panel will provide deeper, AI-generated recommendations once the feature is available.",
                List.of(
                        new AnalyticsDashboardResponse.ResourceLink("Student support guide", "https://example.com/support"),
                        new AnalyticsDashboardResponse.ResourceLink("Grading best practices", "https://example.com/grading")
                )
        );

        String mostDifficultTopic = labTrend.stream()
            .filter(Objects::nonNull)
            .min((a, b) -> {
                if (a.averageScore() == null) return 1;
                if (b.averageScore() == null) return -1;
                return a.averageScore().compareTo(b.averageScore());
            })
            .map(AnalyticsDashboardResponse.LabTrendItem::labName)
            .orElse(null);

        return new AnalyticsDashboardResponse(
                overallAverage,
                lowestAverageLab,
                lowestAverageScore,
                mostDifficultTopic,
                labTrend,
                studentOverview,
                atRiskLabs,
                atRiskStudents,
                aiSummary
        );
    }

    public AnalyticsDashboardResponse emptyDashboard() {
        return new AnalyticsDashboardResponse(
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                new AnalyticsDashboardResponse.AiSummary(
                        "AI trends still coming soon",
                        "This panel will provide deeper, AI-generated recommendations once the feature is available.",
                        Collections.emptyList()
                )
        );
    }

    private Object[] safeFindDashboardSummary(UUID labId, UUID semesterId, UUID academicYearId, String course) {
        try {
            return analyticsRepository.findDashboardSummary(labId, semesterId, academicYearId, course);
        } catch (Exception e) {
            return null;
        }
    }

    private List<AnalyticsDashboardResponse.LabTrendItem> safeFindLabTrend(
            UUID labId, UUID semesterId, UUID academicYearId, String course) {
        List<AnalyticsDashboardResponse.LabTrendItem> labTrend = new ArrayList<>();
        try {
            for (Object[] row : analyticsRepository.findLabTrend(labId, semesterId, academicYearId, course)) {
                AnalyticsDashboardResponse.LabTrendItem item = AnalyticsMapper.toLabTrendItem(row);
                if (item != null) {
                    labTrend.add(item);
                }
            }
        } catch (Exception ignored) {
            // Return empty list when analytics tables have no rows yet.
        }
        return labTrend;
    }

    private List<Object[]> safeFindStudentOverview(
            UUID labId, UUID semesterId, UUID academicYearId, String search,
            String sort, String direction, int page, int size) {
        try {
            return analyticsRepository.findStudentOverview(labId, semesterId, academicYearId, search, sort, direction, page, size);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Object[]> safeFindAtRiskLabs(UUID academicYearId, UUID semesterId) {
        try {
            return analyticsRepository.findAtRiskLabs(academicYearId, semesterId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Object[]> safeFindAtRiskStudents(UUID academicYearId, UUID semesterId) {
        try {
            return analyticsRepository.findAtRiskStudents(academicYearId, semesterId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<AnalyticsDashboardResponse.LabTrendItem> getLabTrend(UUID academicYearId, UUID semesterId, UUID labId, String course) {
        List<AnalyticsDashboardResponse.LabTrendItem> items = new ArrayList<>();
        for (Object[] row : analyticsRepository.findLabTrend(labId, semesterId, academicYearId, course)) {
            items.add(AnalyticsMapper.toLabTrendItem(row));
        }
        return items;
    }

    public StudentOverviewPage getStudentOverview(UUID academicYearId, UUID semesterId, UUID labId, String search, String sort, String direction, int page, int size) {
        List<AnalyticsDashboardResponse.StudentOverviewItem> items = new ArrayList<>();
        int offset = page * size;
        String sortBy = normalizeSort(sort);
        String sortDirection = normalizeDirection(direction);
        for (Object[] row : analyticsRepository.findStudentOverview(labId, semesterId, academicYearId, search, sortBy, sortDirection, offset, size)) {
            items.add(AnalyticsMapper.toStudentOverviewItem(row));
        }
        long total = analyticsRepository.countStudentOverview(labId, semesterId, academicYearId, search);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new StudentOverviewPage(items, page, size, total, totalPages);
    }

    public StudentReportResponse getStudentReport(UUID studentId) {
        Object[] profileRow = analyticsRepository.findStudentProfileSummary(studentId);
        if (profileRow == null) {
            return null;
        }
        StudentReportResponse.StudentProfile profile = AnalyticsMapper.toStudentProfile(profileRow);

        List<StudentReportResponse.GradeTrendItem> gradeTrend = new ArrayList<>();
        for (Object[] row : analyticsRepository.findStudentGradeTrend(studentId)) {
            gradeTrend.add(AnalyticsMapper.toGradeTrendItem(row));
        }

        List<StudentReportResponse.ChallengeBreakdownItem> breakdown = new ArrayList<>();
        for (Object[] row : analyticsRepository.findStudentChallengeBreakdown(studentId)) {
            breakdown.add(AnalyticsMapper.toChallengeBreakdownItem(row));
        }

        List<String> weakSkills = AnalyticsMapper.toWeakSkills(analyticsRepository.findStudentWeakSkills(studentId));

        List<StudentReportResponse.SubmissionHistoryItem> submissionHistory = new ArrayList<>();
        for (Object[] row : analyticsRepository.findSubmissionHistory(studentId)) {
            submissionHistory.add(AnalyticsMapper.toSubmissionHistoryItem(row));
        }

        StudentReportResponse.AiRecommendation aiRecommendation = new StudentReportResponse.AiRecommendation(
                "Personalized guidance will appear here when the AI module is enabled.",
                List.of("Review the highlighted weaknesses regularly", "Practice early to reduce failed attempts")
        );

        return new StudentReportResponse(profile, gradeTrend, breakdown, weakSkills, submissionHistory, aiRecommendation);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) return "overallAverage";
        return switch (sort) {
            case "studentName", "completedLabs", "overallAverage" -> sort;
            default -> "overallAverage";
        };
    }

    private String normalizeDirection(String direction) {
        if (direction == null) return "desc";
        return direction.equalsIgnoreCase("asc") ? "asc" : "desc";
    }

    public record StudentOverviewPage(
            List<AnalyticsDashboardResponse.StudentOverviewItem> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
