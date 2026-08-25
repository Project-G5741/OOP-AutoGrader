package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
import com.eiu.capstone.backend.DTO.TermRosterDTO;
import com.eiu.capstone.backend.DTO.TermStudentDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.service.TermService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lecturer/terms")
public class LecturerTermController {

    private final TermService termService;

    public LecturerTermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping
    public List<TermSummaryDTO> listTerms() {
        return termService.listTerms();
    }

    @PostMapping
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
}
