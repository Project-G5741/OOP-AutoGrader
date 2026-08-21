package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.service.TermService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lecturer/terms")
public class LecturerTermController {

    private final TermService termService;
    private final JwtAuthHelper jwtAuthHelper;

    public LecturerTermController(TermService termService, JwtAuthHelper jwtAuthHelper) {
        this.termService = termService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @GetMapping
    public List<TermSummaryDTO> listTerms(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.listTerms();
    }

    @PostMapping
    public TermSummaryDTO createTerm(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateTermRequest request) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.createTerm(request);
    }

    @PostMapping("/{termId}/current")
    public TermSummaryDTO setCurrent(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.setCurrentTerm(termId);
    }

    @GetMapping("/{termId}/roster")
    public TermRosterDTO listRoster(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.listRoster(termId);
    }

    @GetMapping("/{termId}/students")
    public List<TermStudentDTO> listStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.listEnrolledStudents(termId);
    }

    @GetMapping("/{termId}/available-students")
    public List<TermStudentDTO> listAvailableStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.listAvailableStudents(termId);
    }

    @PostMapping("/{termId}/students")
    public List<TermStudentDTO> enrollStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId,
            @Valid @RequestBody EnrollStudentsRequest request) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.enrollStudents(termId, request.studentIds());
    }

    @PostMapping("/{termId}/students/import")
    public ImportStudentsResult importStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId,
            @Valid @RequestBody ImportStudentsRequest request) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.importStudents(termId, request);
    }

    @DeleteMapping("/{termId}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID termId,
            @PathVariable UUID studentId) {
        jwtAuthHelper.requireLecturer(authHeader);
        termService.removeStudent(termId, studentId);
    }
}
