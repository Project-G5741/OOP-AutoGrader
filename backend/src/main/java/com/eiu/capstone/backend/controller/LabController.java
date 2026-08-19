package com.eiu.capstone.backend.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.analytics.dto.LabAttemptHistoryItemDTO;
import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;
import com.eiu.capstone.backend.analytics.dto.SubmissionSummaryDTO;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.LabDeadlineHelper;
import com.eiu.capstone.backend.service.LabDeadlineHelper.UrgencyState;
import com.eiu.capstone.backend.service.StatsService;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabRepository labRepository;
    private final StatsService statsService;
    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final LabDeadlineHelper labDeadlineHelper;

    public LabController(LabRepository labRepository,
                         StatsService statsService,
                         LecturerAnalyticsService lecturerAnalyticsService,
                         LabDeadlineHelper labDeadlineHelper) {
        this.labRepository = labRepository;
        this.statsService = statsService;
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.labDeadlineHelper = labDeadlineHelper;
    }

    @GetMapping
    public List<LabSummary> listLabs() {
        return labRepository.findAll().stream()
                .sorted(Comparator.comparing(Lab::getName, labDeadlineHelper.naturalLabNameComparator()))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private LabSummary toSummary(Lab lab) {
        UrgencyState urgency = labDeadlineHelper.urgencyState(lab.getDeadlineDate(), java.time.Instant.now());
        return new LabSummary(lab.getId(), lab.getName(), lab.getDeadlineDate(), urgency.name());
    }

    public record LabSummary(UUID id, String name, LocalDate deadlineDate, String urgencyState) {}

    /** Lab-scoped stats for parallel dashboard load (same data as challenge stats route). */
    @GetMapping("/{labId}/stats")
    public StatsDTO getStats(@PathVariable UUID labId,
                             @RequestParam(required = false) UUID studentId) {
        return statsService.getStats(labId, studentId);
    }

    @GetMapping("/{labId}/statistics")
    public LabStatisticsResponse getStatistics(@PathVariable UUID labId) {
        return lecturerAnalyticsService.getLabStatistics(labId);
    }

    @GetMapping("/{labId}/submissions")
    public org.springframework.data.domain.Page<SubmissionSummaryDTO> getSubmissions(@PathVariable UUID labId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "5") int size,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(required = false) String afterName,
                                                     @RequestParam(required = false) UUID afterId) {
        return lecturerAnalyticsService.getLabSubmissions(labId, page, size, sort, afterName, afterId);
    }

    @GetMapping("/{labId}/submissions/export")
    public List<SubmissionSummaryDTO> exportSubmissions(@PathVariable UUID labId,
                                                        @RequestParam(required = false) String sort) {
        return lecturerAnalyticsService.getLabSubmissionsExport(labId, sort);
    }

    @GetMapping("/{labId}/students/{studentId}/attempts")
    public List<LabAttemptHistoryItemDTO> getStudentAttempts(@PathVariable UUID labId,
                                                             @PathVariable UUID studentId) {
        return lecturerAnalyticsService.getLabAttemptHistory(labId, studentId);
    }
}
