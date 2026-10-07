package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.SolutionBootstrapResponse;
import com.eiu.capstone.backend.analytics.dto.UsersBootstrapResponse;
import com.eiu.capstone.backend.analytics.service.LecturerTabBootstrapService;

/**
 * One-request first-useful-data payloads for lecturer nav tabs (always-fresh; no multi-minute TTL).
 */
@RestController
@RequestMapping("/api/lecturer/bootstrap")
public class LecturerTabBootstrapController {

    private final LecturerTabBootstrapService bootstrapService;

    public LecturerTabBootstrapController(LecturerTabBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<LecturerOverviewResponse> dashboard() {
        return ResponseEntity.ok(bootstrapService.dashboard());
    }

    @GetMapping("/score")
    public ResponseEntity<GradeOverviewResponse> score() {
        return ResponseEntity.ok(bootstrapService.score());
    }

    @GetMapping("/grading")
    public ResponseEntity<List<SolutionBootstrapResponse.LabItem>> grading() {
        return ResponseEntity.ok(bootstrapService.gradingLabs());
    }

    @GetMapping("/users")
    public ResponseEntity<UsersBootstrapResponse> users() {
        return ResponseEntity.ok(bootstrapService.users());
    }

    @GetMapping("/quarters")
    public ResponseEntity<List<TermSummaryDTO>> quarters() {
        return ResponseEntity.ok(bootstrapService.quarters());
    }

    @GetMapping("/reports")
    public ResponseEntity<AnalyticsDashboardResponse> reports() {
        return ResponseEntity.ok(bootstrapService.reports());
    }

    @GetMapping("/solution")
    public ResponseEntity<SolutionBootstrapResponse> solution() {
        return ResponseEntity.ok(bootstrapService.solution());
    }
}
