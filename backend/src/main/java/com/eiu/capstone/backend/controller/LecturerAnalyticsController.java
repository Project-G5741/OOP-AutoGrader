package com.eiu.capstone.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;

@RestController
@RequestMapping("/api/lecturer")
public class LecturerAnalyticsController {

    private final LecturerAnalyticsService lecturerAnalyticsService;

    public LecturerAnalyticsController(LecturerAnalyticsService lecturerAnalyticsService) {
        this.lecturerAnalyticsService = lecturerAnalyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<LecturerOverviewResponse> getOverview() {
        return ResponseEntity.ok(lecturerAnalyticsService.getOverview());
    }
}
