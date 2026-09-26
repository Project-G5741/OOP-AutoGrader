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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CloneLabsRequest;
import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.CloneSourcesResponse;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabDeadlineRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabStudentAccessRequest;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseDryRunRequest;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.service.LabCloneService;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.TestcaseDryRunService;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@RestController
@RequestMapping("/api/lecturer/labs")
public class LecturerRubricController {

    private final LabStructureService labStructureService;
    private final TestcaseRubricService testcaseRubricService;
    private final TestcaseDryRunService testcaseDryRunService;
    private final LabCloneService labCloneService;

    public LecturerRubricController(LabStructureService labStructureService,
                                    TestcaseRubricService testcaseRubricService,
                                    TestcaseDryRunService testcaseDryRunService,
                                    LabCloneService labCloneService) {
        this.labStructureService = labStructureService;
        this.testcaseRubricService = testcaseRubricService;
        this.testcaseDryRunService = testcaseDryRunService;
        this.labCloneService = labCloneService;
    }

    @GetMapping("/clone-sources")
    public CloneSourcesResponse listCloneSources() {
        return labCloneService.listCloneSources();
    }

    @PostMapping("/clone")
    public CloneLabsResponse cloneLabs(@RequestBody CloneLabsRequest request) {
        Term previous = labCloneService.findPreviousCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No previous current quarter to copy from"));
        return labCloneService.cloneLabs(
                request.sourceLabIds(),
                request.targetTermId(),
                previous.getId());
    }

    @GetMapping("/{labId}/structure")
    public LabStructureResponse getStructure(@PathVariable UUID labId) {
        return labStructureService.loadForEditor(labId);
    }

    @PutMapping("/{labId}/structure")
    public LabStructureResponse saveStructure(
            @PathVariable UUID labId,
            @RequestBody LabStructureResponse payload) {
        return labStructureService.saveLabStructure(labId, payload);
    }

    @PostMapping
    public ResponseEntity<LabStructureResponse> createLab(@RequestBody CreateLabRequest request) {
        LabStructureResponse created = labStructureService.createLab(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{labId}/deadline")
    public LabStructureResponse updateDeadline(
            @PathVariable UUID labId,
            @RequestBody UpdateLabDeadlineRequest request) {
        return labStructureService.updateLabDeadline(labId, request);
    }

    @PatchMapping("/{labId}/student-access")
    public LabStructureResponse updateStudentAccess(
            @PathVariable UUID labId,
            @RequestBody UpdateLabStudentAccessRequest request) {
        return labStructureService.updateLabStudentAccess(labId, request);
    }

    @DeleteMapping("/{labId}")
    public ResponseEntity<Void> deleteLab(@PathVariable UUID labId) {
        labStructureService.deleteLabCascade(labId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{labId}/challenges/{challengeId}/testcases")
    public ChallengeTestcasesResponse getTestcases(
            @PathVariable UUID labId,
            @PathVariable UUID challengeId) {
        return testcaseRubricService.loadForChallenge(labId, challengeId);
    }

    @PutMapping("/{labId}/challenges/{challengeId}/testcases")
    public ChallengeTestcasesResponse saveTestcases(
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestBody List<TestcaseStructureDTO> testcases) {
        return testcaseRubricService.saveForChallenge(labId, challengeId, testcases);
    }

    @PostMapping("/{labId}/challenges/{challengeId}/testcases/dry-run")
    public TestcaseResultDTO dryRunTestcase(
            @PathVariable UUID labId,
            @PathVariable UUID challengeId,
            @RequestBody TestcaseDryRunRequest request) {
        return testcaseDryRunService.dryRun(labId, challengeId, request);
    }
}
