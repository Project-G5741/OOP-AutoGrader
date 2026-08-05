package com.eiu.capstone.backend.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.SubmissionSummaryDTO;
import com.eiu.capstone.backend.analytics.mapper.AnalyticsMapper;
import com.eiu.capstone.backend.analytics.repository.LecturerAnalyticsRepository;

@Service
public class LecturerAnalyticsService {

    private static final DateTimeFormatter SUBMISSION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final LecturerAnalyticsRepository lecturerAnalyticsRepository;

    public LecturerAnalyticsService(LecturerAnalyticsRepository lecturerAnalyticsRepository) {
        this.lecturerAnalyticsRepository = lecturerAnalyticsRepository;
    }

    public LecturerOverviewResponse getOverview() {
        List<LecturerOverviewResponse.RecentSubmissionItem> recentSubmissions = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findRecentSubmissions(10)) {
            LecturerOverviewResponse.RecentSubmissionItem item = toRecentSubmission(row);
            if (item != null) {
                recentSubmissions.add(item);
            }
        }

        return new LecturerOverviewResponse(
                lecturerAnalyticsRepository.countActiveStudents(),
                lecturerAnalyticsRepository.countLabs(),
                AnalyticsMapper.toBigDecimal(lecturerAnalyticsRepository.findAverageScore()),
                lecturerAnalyticsRepository.countAtRiskStudents(),
                recentSubmissions,
                lecturerAnalyticsRepository.countActiveStudentsWithSubmissions()
        );
    }

    public LabStatisticsResponse getLabStatistics(UUID labId) {
        Object[] summary = lecturerAnalyticsRepository.findLabStatisticsSummary(labId);
        if (summary == null) {
            return emptyLabStatistics(labId);
        }

        long totalStudents = lecturerAnalyticsRepository.countTotalActiveStudents();
        long studentCount = AnalyticsMapper.toLong(summary[6]);
        BigDecimal completionRate = null;
        if (totalStudents > 0) {
            completionRate = BigDecimal.valueOf(studentCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalStudents), 2, RoundingMode.HALF_UP);
        }

        List<LabStatisticsResponse.GradeDistributionBucket> gradeDistribution = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findGradeDistribution(labId)) {
            if (row != null && row.length >= 2) {
                gradeDistribution.add(new LabStatisticsResponse.GradeDistributionBucket(
                        AnalyticsMapper.toString(row[0]),
                        AnalyticsMapper.toLong(row[1])
                ));
            }
        }

        return new LabStatisticsResponse(
                labId,
                AnalyticsMapper.toString(summary[1]),
                AnalyticsMapper.toBigDecimal(summary[2]),
                AnalyticsMapper.toBigDecimal(summary[3]),
                AnalyticsMapper.toBigDecimal(summary[4]),
                AnalyticsMapper.toLong(summary[5]),
                studentCount,
                completionRate,
                gradeDistribution
        );
    }

    public Page<SubmissionSummaryDTO> getLabSubmissions(UUID labId, int page, int size, String sort) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        SortSpec sortSpec = resolveSort(sort);

        long total = lecturerAnalyticsRepository.countLabSubmissions(labId);
        List<SubmissionSummaryDTO> items = new ArrayList<>();
        if (total > 0) {
            int offset = safePage * safeSize;
            for (Object[] row : lecturerAnalyticsRepository.findLabSubmissions(
                    labId, sortSpec.column(), sortSpec.direction(), offset, safeSize)) {
                SubmissionSummaryDTO item = toSubmissionSummary(row);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return new PageImpl<>(items, PageRequest.of(safePage, safeSize), total);
    }

    private LabStatisticsResponse emptyLabStatistics(UUID labId) {
        String labName = lecturerAnalyticsRepository.findLabName(labId);
        return new LabStatisticsResponse(
                labId,
                labName,
                null,
                null,
                null,
                0L,
                0L,
                null,
                List.of()
        );
    }

    private LecturerOverviewResponse.RecentSubmissionItem toRecentSubmission(Object[] row) {
        if (row == null || row.length < 6) {
            return null;
        }
        return new LecturerOverviewResponse.RecentSubmissionItem(
                AnalyticsMapper.toString(row[0]),
                AnalyticsMapper.toString(row[1]),
                AnalyticsMapper.toString(row[2]),
                AnalyticsMapper.toBigDecimal(row[3]),
                AnalyticsMapper.toInt(row[4]),
                formatTimestamp(row[5])
        );
    }

    private SubmissionSummaryDTO toSubmissionSummary(Object[] row) {
        if (row == null || row.length < 6) {
            return null;
        }
        return new SubmissionSummaryDTO(
                AnalyticsMapper.toString(row[0]),
                AnalyticsMapper.toString(row[1]),
                AnalyticsMapper.toBigDecimal(row[2]),
                AnalyticsMapper.toInt(row[3]),
                formatTimestamp(row[4]),
                AnalyticsMapper.toBoolean(row[5])
        );
    }

    private String formatTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.OffsetDateTime offsetDateTime) {
            return SUBMISSION_TIME_FORMAT.format(offsetDateTime);
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.toString();
        }
        return value.toString();
    }

    private SortSpec resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("s.submitted_at", "DESC");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim().toLowerCase();
        String direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc") ? "ASC" : "DESC";
        String column = switch (field) {
            case "score" -> "s.score";
            case "attempt" -> "s.attempt_number";
            case "studentname", "student_name" -> "u.full_name";
            case "studentcode", "student_code" -> "COALESCE(u.student_code, u.teacher_code)";
            default -> "s.submitted_at";
        };
        return new SortSpec(column, direction);
    }

    private record SortSpec(String column, String direction) {
    }
}
