package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.CreateTermRequest;
import com.eiu.capstone.backend.DTO.EnrollStudentsRequest;
import com.eiu.capstone.backend.DTO.ImportStudentsRequest;
import com.eiu.capstone.backend.DTO.ImportStudentsResult;
import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.TermRosterDTO;
import com.eiu.capstone.backend.DTO.TermStudentDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.DTO.TermSyncLabsRequest;
import com.eiu.capstone.backend.DTO.TermSyncLabsResponse;
import com.eiu.capstone.backend.desktop.pack.DesktopPackExportService;
import com.eiu.capstone.backend.service.TermService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lecturer/terms")
public class LecturerTermController {

    private final TermService termService;
    private final DesktopPackExportService desktopPackExportService;

    public LecturerTermController(TermService termService, DesktopPackExportService desktopPackExportService) {
        this.termService = termService;
        this.desktopPackExportService = desktopPackExportService;
    }

    @GetMapping("/list")
    public List<TermSummaryDTO> listTerms() {
        return termService.listTerms();
    }

    @PostMapping("/create")
    public TermSummaryDTO createTerm(@Valid @RequestBody CreateTermRequest request) {
        return termService.createTerm(request);
    }

    @PostMapping("/{termId}/current")
    public TermSummaryDTO setCurrent(@PathVariable UUID termId) {
        return termService.setCurrentTerm(termId);
    }

    @GetMapping("/{termId}/roster")
    public TermRosterDTO listRoster(@PathVariable UUID termId) {
        return termService.listRoster(termId);
    }

    @GetMapping("/{termId}/sync-labs")
    public TermSyncLabsResponse listSyncLabs(@PathVariable UUID termId) {
        return termService.listSyncLabSources(termId);
    }

    @PostMapping("/{termId}/sync-labs")
    public CloneLabsResponse syncLabs(
            @PathVariable UUID termId,
            @RequestBody TermSyncLabsRequest request) {
        List<UUID> sourceLabIds = request != null && request.sourceLabIds() != null
                ? request.sourceLabIds()
                : List.of();
        return termService.syncLabs(termId, sourceLabIds);
    }

    @GetMapping("/{termId}/desktop-pack")
    public ResponseEntity<byte[]> downloadDesktopPack(@PathVariable UUID termId) {
        var download = desktopPackExportService.exportTermPack(termId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(download.body());
    }

    @GetMapping("/{termId}/students")
    public List<TermStudentDTO> listStudents(@PathVariable UUID termId) {
        return termService.listEnrolledStudents(termId);
    }

    @GetMapping("/{termId}/available-students")
    public List<TermStudentDTO> listAvailableStudents(@PathVariable UUID termId) {
        return termService.listAvailableStudents(termId);
    }

    @PostMapping("/{termId}/students")
    public List<TermStudentDTO> enrollStudents(
            @PathVariable UUID termId,
            @Valid @RequestBody EnrollStudentsRequest request) {
        return termService.enrollStudents(termId, request.studentIds());
    }

    @PostMapping("/{termId}/students/import")
    public ImportStudentsResult importStudents(
            @PathVariable UUID termId,
            @Valid @RequestBody ImportStudentsRequest request) {
        return termService.importStudents(termId, request);
    }

    @DeleteMapping("/{termId}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable UUID termId, @PathVariable UUID studentId) {
        termService.removeStudent(termId, studentId);
    }

    @DeleteMapping("/{termId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTerm(@PathVariable UUID termId) {
        termService.deleteTerm(termId);
    }
}
