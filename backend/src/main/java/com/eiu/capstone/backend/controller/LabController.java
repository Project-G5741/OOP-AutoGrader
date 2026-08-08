package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
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
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.StatsService;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabRepository labRepository;
    private final StatsService statsService;
    private final LecturerAnalyticsService lecturerAnalyticsService;

    public LabController(LabRepository labRepository,
                         StatsService statsService,
                         LecturerAnalyticsService lecturerAnalyticsService) {
        this.labRepository = labRepository;
        this.statsService = statsService;
        this.lecturerAnalyticsService = lecturerAnalyticsService;
    }

    @GetMapping
    public List<LabSummary> listLabs() {
        return labRepository.findAll().stream()
                .map(lab -> new LabSummary(lab.getId(), lab.getName()))
                .collect(Collectors.toList());
    }

    public record LabSummary(UUID id, String name) {}

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
    public Page<SubmissionSummaryDTO> getSubmissions(@PathVariable UUID labId,
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