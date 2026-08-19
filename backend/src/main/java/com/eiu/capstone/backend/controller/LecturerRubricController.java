package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabDeadlineRequest;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseDryRunRequest;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.TestcaseDryRunService;
import com.eiu.capstone.backend.service.TestcaseRubricService;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api/lecturer/labs")
public class LecturerRubricController {

    private final LabStructureService labStructureService;
    private final TestcaseRubricService testcaseRubricService;
    private final TestcaseDryRunService testcaseDryRunService;
    private final JwtAuthHelper jwtAuthHelper;

    public LecturerRubricController(LabStructureService labStructureService,
                                    TestcaseRubricService testcaseRubricService,
                                    TestcaseDryRunService testcaseDryRunService,
                                    JwtAuthHelper jwtAuthHelper) {
        this.labStructureService = labStructureService;
        this.testcaseRubricService = testcaseRubricService;
        this.testcaseDryRunService = testcaseDryRunService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    private void requireLecturer(String authHeader) {
        Claims claims = jwtAuthHelper.parseBearerToken(authHeader);
        jwtAuthHelper.requireRole(claims, "LECTURER");
    }

    @GetMapping("/{labId}/structure")
    public LabStructureResponse getStructure(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId) {
        requireLecturer(authHeader);
        return labStructureService.loadForEditor(labId);
    }

    @PutMapping("/{labId}/structure")
    public LabStructureResponse saveStructure(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId,
            @RequestBody LabStructureResponse payload) {
        requireLecturer(authHeader);
        return labStructureService.saveLabStructure(labId, payload);
    }

    @PostMapping
    public ResponseEntity<LabStructureResponse> createLab(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreateLabRequest request) {
        requireLecturer(authHeader);
        LabStructureResponse created = labStructureService.createLab(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{labId}/deadline")
    public LabStructureResponse updateDeadline(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId,
            @RequestBody UpdateLabDeadlineRequest request) {
        requireLecturer(authHeader);
        return labStructureService.updateLabDeadline(labId, request);
    }

    @DeleteMapping("/{labId}")
    public ResponseEntity<Void> deleteLab(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId) {
        requireLecturer(authHeader);
        labStructureService.deleteLabCascade(labId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{labId}/challenges/{challengeId}/testcases")
    public ChallengeTestcasesResponse getTestcases(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId) {
        requireLecturer(authHeader);
        return testcaseRubricService.loadForChallenge(labId, challengeId);
    }

    @PutMapping("/{labId}/challenges/{challengeId}/testcases")
    public ChallengeTestcasesResponse saveTestcases(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestBody List<TestcaseStructureDTO> testcases) {
        requireLecturer(authHeader);
        return testcaseRubricService.saveForChallenge(labId, challengeId, testcases);
    }

    @PostMapping("/{labId}/challenges/{challengeId}/testcases/dry-run")
    public TestcaseResultDTO dryRunTestcase(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestBody TestcaseDryRunRequest request) {
        requireLecturer(authHeader);
        return testcaseDryRunService.dryRun(labId, challengeId, request);
    }
}
