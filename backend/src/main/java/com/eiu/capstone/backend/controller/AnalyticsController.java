package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.StudentReportResponse;
import com.eiu.capstone.backend.analytics.service.AnalyticsService;
import com.eiu.capstone.backend.security.JwtAuthHelper;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtAuthHelper jwtAuthHelper;
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    public AnalyticsController(AnalyticsService analyticsService, JwtAuthHelper jwtAuthHelper) {
        this.analyticsService = analyticsService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String course) {
        jwtAuthHelper.requireLecturer(authHeader);
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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String course) {
        jwtAuthHelper.requireLecturer(authHeader);
        return analyticsService.getLabTrend(academicYearId, semesterId, labId, course);
    }

    @GetMapping("/student-overview")
    public AnalyticsService.StudentOverviewPage getStudentOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID labId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "overallAverage") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        jwtAuthHelper.requireLecturer(authHeader);
        return analyticsService.getStudentOverview(academicYearId, semesterId, labId, search, sort, direction, page, size);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentReportResponse> getStudentReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID studentId) {
        jwtAuthHelper.requireLecturer(authHeader);
        StudentReportResponse report = analyticsService.getStudentReport(studentId);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(report);
    }
}
