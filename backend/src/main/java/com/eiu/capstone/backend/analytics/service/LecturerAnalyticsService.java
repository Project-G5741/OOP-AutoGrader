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
        long enrolledCount = lecturerAnalyticsRepository.countEnrolledStudentsForLab(labId);
        long studentsSubmitted = lecturerAnalyticsRepository.countStudentsSubmittedForLab(labId);

        if (summary == null && enrolledCount == 0) {
            return emptyLabStatistics(labId);
        }

        BigDecimal completionRate = null;
        if (enrolledCount > 0) {
            completionRate = BigDecimal.valueOf(studentsSubmitted)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(enrolledCount), 2, RoundingMode.HALF_UP);
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

        String labName = summary != null
                ? AnalyticsMapper.toString(summary[1])
                : lecturerAnalyticsRepository.findLabName(labId);
        BigDecimal averageScore = summary != null ? AnalyticsMapper.toBigDecimal(summary[2]) : null;
        BigDecimal highestScore = summary != null ? AnalyticsMapper.toBigDecimal(summary[3]) : null;
        BigDecimal lowestScore = summary != null ? AnalyticsMapper.toBigDecimal(summary[4]) : null;
        long submissionCount = summary != null ? AnalyticsMapper.toLong(summary[5]) : 0L;

        return new LabStatisticsResponse(
                labId,
                labName,
                averageScore,
                highestScore,
                lowestScore,
                submissionCount,
                enrolledCount,
                studentsSubmitted,
                completionRate,
                gradeDistribution
        );
    }

    public Page<SubmissionSummaryDTO> getLabSubmissions(UUID labId, int page, int size, String sort) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        SortSpec sortSpec = resolveLabSort(sort);

        long total = lecturerAnalyticsRepository.countEnrolledStudentsForLab(labId);
        List<SubmissionSummaryDTO> items = new ArrayList<>();
        if (total > 0) {
            int offset = safePage * safeSize;
            for (Object[] row : lecturerAnalyticsRepository.findLabStudentRoster(
                    labId, sortSpec.column(), sortSpec.direction(), offset, safeSize)) {
                SubmissionSummaryDTO item = toLabRosterRow(row);
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

    private SubmissionSummaryDTO toLabRosterRow(Object[] row) {
        if (row == null || row.length < 7) {
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

    private SortSpec resolveLabSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("u.full_name", "ASC");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim().toLowerCase();
        String direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc") ? "DESC" : "ASC";
        String column = switch (field) {
            case "score" -> "latest_sub.score";
            case "attempt" -> "latest_sub.attempt_number";
            case "studentname", "student_name" -> "u.full_name";
            case "studentcode", "student_code" -> "COALESCE(u.student_code, u.teacher_code)";
            case "submittedat", "submitted_at" -> "latest_sub.submitted_at";
            default -> "u.full_name";
        };
        return new SortSpec(column, direction);
    }

    private record SortSpec(String column, String direction) {
    }
}

