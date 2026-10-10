package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CloneLabsRequest;
import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.CloneSourcesResponse;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabDeadlineRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabStudentAccessRequest;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.LabStructureSaveRequest;
import com.eiu.capstone.backend.DTO.rubric.SolutionImportResponse;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseDryRunRequest;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.service.LabCloneService;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.SolutionImportService;
import com.eiu.capstone.backend.service.TestcaseDryRunService;
import com.eiu.capstone.backend.service.TestcaseRubricService;
import com.eiu.capstone.backend.desktop.pack.DesktopPackExportService;

@RestController
@RequestMapping("/api/lecturer/labs")
public class LecturerRubricController {

    private final LabStructureService labStructureService;
    private final TestcaseRubricService testcaseRubricService;
    private final TestcaseDryRunService testcaseDryRunService;
    private final LabCloneService labCloneService;
    private final DesktopPackExportService desktopPackExportService;
    private final SolutionImportService solutionImportService;

    public LecturerRubricController(LabStructureService labStructureService,
                                    TestcaseRubricService testcaseRubricService,
                                    TestcaseDryRunService testcaseDryRunService,
                                    LabCloneService labCloneService,
                                    DesktopPackExportService desktopPackExportService,
                                    SolutionImportService solutionImportService) {
        this.labStructureService = labStructureService;
        this.testcaseRubricService = testcaseRubricService;
        this.testcaseDryRunService = testcaseDryRunService;
        this.labCloneService = labCloneService;
        this.desktopPackExportService = desktopPackExportService;
        this.solutionImportService = solutionImportService;
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

    @GetMapping("/{labId}/desktop-pack")
    public ResponseEntity<byte[]> downloadDesktopPack(@PathVariable UUID labId) {
        var download = desktopPackExportService.exportLabPack(labId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(download.body());
    }

    @PutMapping("/{labId}/structure")
    public LabStructureResponse saveStructure(
            @PathVariable UUID labId,
            @RequestBody LabStructureSaveRequest request) {
        return labStructureService.saveLabStructure(
                labId, request.toStructure(), request.replacedChallengeIdsOrEmpty());
    }

    @PostMapping(path = "/{labId}/solution-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SolutionImportResponse importSolution(
            @PathVariable UUID labId,
            @RequestParam("files") List<MultipartFile> files) {
        return solutionImportService.importSolution(labId, files);
    }

    @PostMapping("/create")
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
