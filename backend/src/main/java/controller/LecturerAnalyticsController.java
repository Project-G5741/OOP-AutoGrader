package com.eiu.capstone.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.plagiarism.LabPlagiarismReportDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismFlagsDTO;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;

@RestController
@RequestMapping("/api/lecturer")
public class LecturerAnalyticsController {

    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final PlagiarismService plagiarismService;

    public LecturerAnalyticsController(LecturerAnalyticsService lecturerAnalyticsService,
                                       PlagiarismService plagiarismService) {
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.plagiarismService = plagiarismService;
    }

    @GetMapping("/overview")
    public ResponseEntity<LecturerOverviewResponse> getOverview() {
        return ResponseEntity.ok(lecturerAnalyticsService.getOverview());
    }

    @GetMapping("/grade-overview")
    public ResponseEntity<GradeOverviewResponse> getGradeOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentName,asc") String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(lecturerAnalyticsService.getGradeOverview(page, size, sort, search));
    }

    @GetMapping("/plagiarism/flags")
    public PlagiarismFlagsDTO getPlagiarismFlags() {
        return plagiarismService.lecturerFlags();
    }

    @GetMapping("/labs/{labId}/plagiarism")
    public LabPlagiarismReportDTO getPlagiarism(@PathVariable UUID labId) {
        return plagiarismService.reportForLab(labId);
    }

    @GetMapping("/labs/{labId}/students/{studentId}/plagiarism")
    public com.eiu.capstone.backend.DTO.plagiarism.PlagiarismInvestigationDTO getStudentPlagiarism(
            @PathVariable UUID labId,
            @PathVariable UUID studentId) {
        return plagiarismService.investigationForStudent(labId, studentId);
    }
}
