package com.eiu.capstone.backend.analytics.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.StudentReportResponse;

public class AnalyticsMapper {

    public static AnalyticsDashboardResponse.LabTrendItem toLabTrendItem(Object[] row) {
        if (row == null || row.length < 4) return null;
        return new AnalyticsDashboardResponse.LabTrendItem(
                toString(row[0]),   
                toString(row[1]),
                toBigDecimal(row[2]),
                toLong(row[3])
        );
    }

    public static AnalyticsDashboardResponse.StudentOverviewItem toStudentOverviewItem(Object[] row) {
        if (row == null || row.length < 5) return null;
        BigDecimal avg = toBigDecimal(row[3]);
        return new AnalyticsDashboardResponse.StudentOverviewItem(
                toString(row[0]),
                toString(row[1]),
                toString(row[2]),
                avg,
                toInt(row[4]),
                computeStatus(avg)
        );
    }

    public static AnalyticsDashboardResponse.AtRiskLabItem toAtRiskLabItem(Object[] row) {
        if (row == null || row.length < 5) return null;
        BigDecimal averageScore = toBigDecimal(row[2]);
        return new AnalyticsDashboardResponse.AtRiskLabItem(
                toString(row[0]),
                toString(row[1]),
                toString(row[4]),
                averageScore,
                "Low average score and challenge failure trend.",
                List.of(
                        new AnalyticsDashboardResponse.ResourceLink("Review lab instructions", "https://example.com/lab-resources"),
                        new AnalyticsDashboardResponse.ResourceLink("Practice exercises", "https://example.com/practice")
                )
        );
    }

    public static AnalyticsDashboardResponse.AtRiskStudentItem toAtRiskStudentItem(Object[] row) {
        if (row == null || row.length < 4) return null;
        return new AnalyticsDashboardResponse.AtRiskStudentItem(
                toString(row[0]),
                toString(row[1]),
                toBigDecimal(row[2]),
                List.of("Challenge review", "Syntax accuracy", "Test coverage"),
                "Focus on concept review and more practice tests."
        );
    }

    public static StudentReportResponse.GradeTrendItem toGradeTrendItem(Object[] row) {
        if (row == null || row.length < 3) return null;
        return new StudentReportResponse.GradeTrendItem(
                toString(row[0]),
                toString(row[1]),
                toBigDecimal(row[2])
        );
    }

    public static StudentReportResponse.ChallengeBreakdownItem toChallengeBreakdownItem(Object[] row) {
        if (row == null || row.length < 3) return null;
        int scorePercent = 0;
        BigDecimal correct = toBigDecimal(row[1]);
        BigDecimal total = toBigDecimal(row[2]);
        if (correct != null && total != null && total.compareTo(BigDecimal.ZERO) > 0) {
            scorePercent = correct.multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return new StudentReportResponse.ChallengeBreakdownItem(toString(row[0]), scorePercent);
    }

    public static StudentReportResponse.SubmissionHistoryItem toSubmissionHistoryItem(Object[] row) {
        if (row == null || row.length < 5) return null;
        return new StudentReportResponse.SubmissionHistoryItem(
                toString(row[0]),
                toInt(row[1]),
                toBigDecimal(row[2]),
                toString(row[3]),
                toBoolean(row[4])
        );
    }

    public static StudentReportResponse.StudentProfile toStudentProfile(Object[] row) {
        if (row == null || row.length < 7) return null;
        return new StudentReportResponse.StudentProfile(
                toString(row[0]),
                toString(row[1]),
                toString(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                toInt(row[5]),
                toLong(row[6])
        );
    }

    public static List<String> toWeakSkills(List<Object[]> rows) {
        List<String> skills = new ArrayList<>();
        for (Object[] row : rows) {
            if (row != null && row.length > 0) {
                skills.add(toString(row[0]));
            }
        }
        return skills;
    }

    public static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return null;
    }

    public static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return 0;
    }

    public static long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return 0L;
    }

    public static boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return false;
    }

    private static String computeStatus(BigDecimal average) {
        if (average == null) {
            return "At Risk";
        }
        if (average.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return "Excellent";
        }
        if (average.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "Average";
        }
        return "At Risk";
    }
}
