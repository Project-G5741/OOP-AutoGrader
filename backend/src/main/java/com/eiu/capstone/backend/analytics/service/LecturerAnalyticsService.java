package com.eiu.capstone.backend.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.analytics.dto.ChallengeStudentRowDTO;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewLabColumnDTO;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewStudentRowDTO;
import com.eiu.capstone.backend.analytics.dto.LabAttemptHistoryItemDTO;
import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.SubmissionSummaryDTO;
import com.eiu.capstone.backend.analytics.mapper.AnalyticsMapper;
import com.eiu.capstone.backend.analytics.repository.LecturerAnalyticsRepository;
import com.eiu.capstone.backend.service.ChallengeService;

@Service
public class LecturerAnalyticsService {

    private static final DateTimeFormatter SUBMISSION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LecturerAnalyticsRepository lecturerAnalyticsRepository;
    private final ChallengeService challengeService;
    private final LecturerOverviewCache lecturerOverviewCache;
    private final LabStatisticsCache labStatisticsCache;

    public LecturerAnalyticsService(LecturerAnalyticsRepository lecturerAnalyticsRepository,
                                    ChallengeService challengeService,
                                    LecturerOverviewCache lecturerOverviewCache,
                                    LabStatisticsCache labStatisticsCache) {
        this.lecturerAnalyticsRepository = lecturerAnalyticsRepository;
        this.challengeService = challengeService;
        this.lecturerOverviewCache = lecturerOverviewCache;
        this.labStatisticsCache = labStatisticsCache;
    }

    public LecturerOverviewResponse getOverview() {
        return lecturerOverviewCache.get(this::loadOverview);
    }

    private LecturerOverviewResponse loadOverview() {
        Object[] metrics = lecturerAnalyticsRepository.findOverviewMetrics();
        long totalStudents = 0L;
        long totalLabs = 0L;
        BigDecimal averageScore = null;
        long atRiskStudents = 0L;
        long activeStudents = 0L;
        if (metrics != null && metrics.length >= 5) {
            totalStudents = AnalyticsMapper.toLong(metrics[0]);
            totalLabs = AnalyticsMapper.toLong(metrics[1]);
            averageScore = AnalyticsMapper.toBigDecimal(metrics[2]);
            atRiskStudents = AnalyticsMapper.toLong(metrics[3]);
            activeStudents = AnalyticsMapper.toLong(metrics[4]);
        }

        List<LecturerOverviewResponse.RecentSubmissionItem> recentSubmissions = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findRecentSubmissions(10)) {
            LecturerOverviewResponse.RecentSubmissionItem item = toRecentSubmission(row);
            if (item != null) {
                recentSubmissions.add(item);
            }
        }

        return new LecturerOverviewResponse(
                totalStudents,
                totalLabs,
                averageScore,
                atRiskStudents,
                recentSubmissions,
                activeStudents
        );
    }

    public LabStatisticsResponse getLabStatistics(UUID labId) {
        return labStatisticsCache.get(labId, () -> loadLabStatistics(labId));
    }

    private LabStatisticsResponse loadLabStatistics(UUID labId) {
        Object[] summary = lecturerAnalyticsRepository.findLabStatisticsSummary(labId);
        long activeEnrolledCount = lecturerAnalyticsRepository.countActiveEnrolledStudentsForLab(labId);
        long studentsSubmitted = lecturerAnalyticsRepository.countActiveStudentsSubmittedForLab(labId);

        if (summary == null && activeEnrolledCount == 0) {
            return emptyLabStatistics(labId);
        }

        BigDecimal completionRate = null;
        if (activeEnrolledCount > 0) {
            completionRate = BigDecimal.valueOf(studentsSubmitted)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(activeEnrolledCount), 2, RoundingMode.HALF_UP);
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
                activeEnrolledCount,
                studentsSubmitted,
                completionRate,
                gradeDistribution
        );
    }

    public Page<SubmissionSummaryDTO> getLabSubmissions(UUID labId,
                                                        int page,
                                                        int size,
                                                        String sort,
                                                        String afterName,
                                                        UUID afterId) {
        int safeSize = size <= 0 ? 5 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        SortSpec sortSpec = resolveLabSort(sort);

        long total = lecturerAnalyticsRepository.countEnrolledStudentsForLab(labId);
        List<SubmissionSummaryDTO> items = new ArrayList<>();
        if (total > 0) {
            List<Object[]> rows;
            if (afterName != null && afterId != null) {
                rows = lecturerAnalyticsRepository.findLabStudentRosterAfter(
                        labId, sortSpec.column(), sortSpec.direction(), afterName, afterId, safeSize);
            } else {
                int offset = safePage * safeSize;
                rows = lecturerAnalyticsRepository.findLabStudentRoster(
                        labId, sortSpec.column(), sortSpec.direction(), offset, safeSize);
            }
            for (Object[] row : rows) {
                SubmissionSummaryDTO item = toLabRosterRow(row);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return new PageImpl<>(items, PageRequest.of(safePage, safeSize), total);
    }

    public List<SubmissionSummaryDTO> getLabSubmissionsExport(UUID labId, String sort) {
        SortSpec sortSpec = resolveLabSort(sort);
        List<SubmissionSummaryDTO> items = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findLabStudentRosterExport(
                labId, sortSpec.column(), sortSpec.direction())) {
            SubmissionSummaryDTO item = toLabRosterRow(row);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public Page<ChallengeStudentRowDTO> getChallengeStudentRoster(UUID labId,
                                                                  UUID challengeId,
                                                                  int page,
                                                                  int size,
                                                                  String sort) {
        int safeSize = size <= 0 ? 5 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        SortSpec sortSpec = resolveChallengeSort(sort);

        long total = lecturerAnalyticsRepository.countEnrolledStudentsForLab(labId);
        List<ChallengeStudentRowDTO> items = new ArrayList<>();
        if (total > 0) {
            int offset = safePage * safeSize;
            for (Object[] row : lecturerAnalyticsRepository.findChallengeStudentRoster(
                    labId, challengeId, sortSpec.column(), sortSpec.direction(), offset, safeSize)) {
                ChallengeStudentRowDTO item = toChallengeRosterRow(row, challengeId);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return new PageImpl<>(items, PageRequest.of(safePage, safeSize), total);
    }

    public GradeOverviewResponse getGradeOverview(int page, int size) {
        int safeSize = size <= 0 ? 5 : Math.min(size, 100);
        int safePage = Math.max(page, 0);

        List<GradeOverviewLabColumnDTO> labs = new ArrayList<>();
        List<UUID> labIds = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findAllLabsOrdered()) {
            UUID labId = AnalyticsMapper.toUuid(row[0]);
            String labName = AnalyticsMapper.toString(row[1]);
            if (labId != null) {
                labIds.add(labId);
                labs.add(new GradeOverviewLabColumnDTO(labId, labName));
            }
        }

        long totalStudents = lecturerAnalyticsRepository.countGradeOverviewStudents();
        List<GradeOverviewStudentRowDTO> rows = new ArrayList<>();
        int labCount = labIds.size();
        if (totalStudents > 0) {
            int offset = safePage * safeSize;
            List<Object[]> students = lecturerAnalyticsRepository.findGradeOverviewStudents(offset, safeSize);
            List<UUID> studentIds = new ArrayList<>();
            for (Object[] row : students) {
                UUID studentId = AnalyticsMapper.toUuid(row[0]);
                if (studentId != null) {
                    studentIds.add(studentId);
                }
            }

            Map<UUID, Map<UUID, BigDecimal>> scoresByStudent = new HashMap<>();
            for (Object[] row : lecturerAnalyticsRepository.findLabScoresForStudents(studentIds)) {
                UUID studentId = AnalyticsMapper.toUuid(row[0]);
                UUID labId = AnalyticsMapper.toUuid(row[1]);
                BigDecimal score = AnalyticsMapper.toBigDecimal(row[2]);
                if (studentId == null || labId == null) {
                    continue;
                }
                scoresByStudent.computeIfAbsent(studentId, ignored -> new HashMap<>()).put(labId, score);
            }

            for (Object[] row : students) {
                UUID studentId = AnalyticsMapper.toUuid(row[0]);
                if (studentId == null) {
                    continue;
                }
                Map<UUID, BigDecimal> studentScores = scoresByStudent.getOrDefault(studentId, Map.of());
                List<BigDecimal> labScores = new ArrayList<>();
                BigDecimal sum = BigDecimal.ZERO;
                for (UUID labId : labIds) {
                    BigDecimal score = studentScores.get(labId);
                    labScores.add(score);
                    sum = sum.add(score != null ? score : BigDecimal.ZERO);
                }
                BigDecimal totalScore = labCount > 0
                        ? sum.divide(BigDecimal.valueOf(labCount), 2, RoundingMode.HALF_UP)
                        : null;
                rows.add(new GradeOverviewStudentRowDTO(
                        studentId,
                        AnalyticsMapper.toString(row[1]),
                        AnalyticsMapper.toString(row[2]),
                        totalScore,
                        labScores
                ));
            }
        }

        int totalPages = safeSize > 0 ? (int) Math.ceil((double) totalStudents / safeSize) : 0;
        return new GradeOverviewResponse(labs, rows, safePage, safeSize, totalStudents, totalPages);
    }

    public List<LabAttemptHistoryItemDTO> getLabAttemptHistory(UUID labId, UUID studentId) {
        List<LabAttemptHistoryItemDTO> items = new ArrayList<>();
        for (Object[] row : lecturerAnalyticsRepository.findLabAttemptHistory(labId, studentId)) {
            LabAttemptHistoryItemDTO item = toAttemptHistoryRow(row);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
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
        if (row == null || row.length < 8) {
            return null;
        }
        UUID submissionId = AnalyticsMapper.toUuid(row[7]);
        return new SubmissionSummaryDTO(
                AnalyticsMapper.toUuid(row[0]),
                AnalyticsMapper.toString(row[1]),
                AnalyticsMapper.toString(row[2]),
                AnalyticsMapper.toBigDecimal(row[3]),
                AnalyticsMapper.toInt(row[4]),
                formatTimestamp(row[5]),
                AnalyticsMapper.toBoolean(row[6]),
                submissionId,
                submissionId != null
        );
    }

    private ChallengeStudentRowDTO toChallengeRosterRow(Object[] row, UUID challengeId) {
        if (row == null || row.length < 8) {
            return null;
        }
        UUID submissionId = AnalyticsMapper.toUuid(row[7]);
        boolean hasSubmission = AnalyticsMapper.toBoolean(row[6]);
        BigDecimal score = AnalyticsMapper.toBigDecimal(row[3]);
        if (score == null && submissionId != null && hasSubmission) {
            Integer computed = challengeService.computeChallengeScoreForSubmission(submissionId, challengeId);
            if (computed != null) {
                score = BigDecimal.valueOf(computed);
            }
        }
        return new ChallengeStudentRowDTO(
                AnalyticsMapper.toUuid(row[0]),
                AnalyticsMapper.toString(row[1]),
                AnalyticsMapper.toString(row[2]),
                score,
                AnalyticsMapper.toInt(row[4]),
                formatTimestamp(row[5]),
                hasSubmission,
                submissionId
        );
    }

    private LabAttemptHistoryItemDTO toAttemptHistoryRow(Object[] row) {
        if (row == null || row.length < 5) {
            return null;
        }
        return new LabAttemptHistoryItemDTO(
                AnalyticsMapper.toInt(row[0]),
                AnalyticsMapper.toBigDecimal(row[1]),
                formatTimestamp(row[2]),
                AnalyticsMapper.toUuid(row[3]),
                AnalyticsMapper.toBoolean(row[4])
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
            return SUBMISSION_TIME_FORMAT.format(localDateTime);
        }
        if (value instanceof java.time.Instant instant) {
            return SUBMISSION_TIME_FORMAT.format(instant.atZone(java.time.ZoneId.systemDefault()));
        }
        return value.toString();
    }

    private SortSpec resolveChallengeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("u.full_name", "ASC");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim().toLowerCase();
        String direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc") ? "DESC" : "ASC";
        String column = switch (field) {
            case "score" -> "challenge_sub.scr_score";
            case "attempts", "attempt" -> "challenge_attempts.attempt_count";
            case "studentname", "student_name" -> "u.full_name";
            case "studentcode", "student_code" -> "COALESCE(u.student_code, u.teacher_code)";
            case "submittedat", "submitted_at" -> "challenge_sub.submitted_at";
            default -> "u.full_name";
        };
        return new SortSpec(column, direction);
    }

    private SortSpec resolveLabSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("u.full_name", "ASC");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim().toLowerCase();
        String direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc") ? "DESC" : "ASC";
        String column = switch (field) {
            case "score" -> "p.highest_score";
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

