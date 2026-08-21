package com.eiu.capstone.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.plagiarism.LabPlagiarismReportDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismFlagsDTO;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.security.JwtAuthHelper;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api/lecturer")
public class LecturerAnalyticsController {

    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final PlagiarismService plagiarismService;
    private final JwtAuthHelper jwtAuthHelper;

    public LecturerAnalyticsController(LecturerAnalyticsService lecturerAnalyticsService,
                                       PlagiarismService plagiarismService,
                                       JwtAuthHelper jwtAuthHelper) {
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.plagiarismService = plagiarismService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @GetMapping("/overview")
    public ResponseEntity<LecturerOverviewResponse> getOverview() {
        return ResponseEntity.ok(lecturerAnalyticsService.getOverview());
    }

    @GetMapping("/grade-overview")
    public ResponseEntity<GradeOverviewResponse> getGradeOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentName,asc") String sort) {
        return ResponseEntity.ok(lecturerAnalyticsService.getGradeOverview(page, size, sort));
    }

    @GetMapping("/plagiarism/flags")
    public PlagiarismFlagsDTO getPlagiarismFlags(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Claims claims = jwtAuthHelper.parseBearerToken(authHeader);
        jwtAuthHelper.requireRole(claims, "LECTURER");
        return plagiarismService.lecturerFlags();
    }

    @GetMapping("/labs/{labId}/plagiarism")
    public LabPlagiarismReportDTO getPlagiarism(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId) {
        Claims claims = jwtAuthHelper.parseBearerToken(authHeader);
        jwtAuthHelper.requireRole(claims, "LECTURER");
        return plagiarismService.reportForLab(labId);
    }
}
