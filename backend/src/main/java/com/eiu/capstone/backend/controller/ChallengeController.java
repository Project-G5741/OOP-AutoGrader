package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.ClassTabResponse;
import com.eiu.capstone.backend.DTO.MmdResponseDTO;
import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.analytics.dto.ChallengeStudentRowDTO;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.StatsService;
import com.eiu.capstone.backend.service.StudentTermAccessService;

@RestController
@RequestMapping("/api/labs/{labId}/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ClassStructureService classStructureService;
    private final StatsService statsService;
    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final JwtAuthHelper jwtAuthHelper;
    private final LabRepository labRepository;
    private final StudentTermAccessService studentTermAccessService;

    public ChallengeController(ChallengeService challengeService,
                                ClassStructureService classStructureService,
                                StatsService statsService,
                                LecturerAnalyticsService lecturerAnalyticsService,
                                JwtAuthHelper jwtAuthHelper,
                                LabRepository labRepository,
                                StudentTermAccessService studentTermAccessService) {
        this.challengeService = challengeService;
        this.classStructureService = classStructureService;
        this.statsService = statsService;
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.jwtAuthHelper = jwtAuthHelper;
        this.labRepository = labRepository;
        this.studentTermAccessService = studentTermAccessService;
    }

    @GetMapping
    public List<ChallengeDTO> getChallenges(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @RequestParam(required = false) UUID studentId) {
        requireStudentLabAccessIfNeeded(principal, labId);
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        return challengeService.getChallengesForLab(labId, scopedStudentId);
    }

    @GetMapping("/{challengeId}/mmd")
    public MmdResponseDTO getMmd(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID submissionId) {
        requireStudentLabAccessIfNeeded(principal, labId);
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        var disclosureMode = jwtAuthHelper.resolveDisclosureMode(principal, studentId);
        return classStructureService.getMmdData(labId, challengeId, scopedStudentId, submissionId, disclosureMode);
    }

    @GetMapping("/{challengeId}/class")
    public ClassTabResponse getClassData(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID submissionId) {
        requireStudentLabAccessIfNeeded(principal, labId);
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        var disclosureMode = jwtAuthHelper.resolveDisclosureMode(principal, studentId);
        return classStructureService.getClassData(labId, challengeId, scopedStudentId, submissionId, disclosureMode);
    }

    @GetMapping("/{challengeId}/testcases")
    public List<TestcaseResultDTO> getTestcases(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID submissionId) {
        requireStudentLabAccessIfNeeded(principal, labId);
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        return classStructureService.getTestcaseData(labId, challengeId, scopedStudentId, submissionId);
    }

    @GetMapping("/{challengeId}/stats")
    public StatsDTO getStats(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) UUID studentId) {
        requireStudentLabAccessIfNeeded(principal, labId);
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        return statsService.getStats(labId, scopedStudentId);
    }

    @GetMapping("/{challengeId}/students")
    public Page<ChallengeStudentRowDTO> getChallengeStudents(
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String sort) {
        return lecturerAnalyticsService.getChallengeStudentRoster(labId, challengeId, page, size, sort);
    }

    private void requireStudentLabAccessIfNeeded(JwtUserPrincipal principal, UUID labId) {
        if (principal == null || !principal.isStudentOnly()) {
            return;
        }
        Lab lab = labRepository.findByIdWithTerm(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        studentTermAccessService.requireStudentLabAccess(
                jwtAuthHelper.requireActiveUser(principal), lab);
    }
}
