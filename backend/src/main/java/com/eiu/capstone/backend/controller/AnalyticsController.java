package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.StudentReportResponse;
import com.eiu.capstone.backend.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String course) {
        try {
            AnalyticsDashboardResponse resp = analyticsService.getDashboard(academicYearId, semesterId, labId, course);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.warn("Analytics dashboard returned empty response due to query issue", e);
            return ResponseEntity.ok(analyticsService.emptyDashboard());
        }
    }

    @GetMapping("/lab-trend")
    public List<AnalyticsDashboardResponse.LabTrendItem> getLabTrend(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String course) {
        return analyticsService.getLabTrend(academicYearId, semesterId, labId, course);
    }

    @GetMapping("/student-overview")
    public AnalyticsService.StudentOverviewPage getStudentOverview(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "overallAverage") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return analyticsService.getStudentOverview(academicYearId, semesterId, labId, search, sort, direction, page, size);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentReportResponse> getStudentReport(@PathVariable UUID studentId) {
        StudentReportResponse report = analyticsService.getStudentReport(studentId);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(report);
    }
}
