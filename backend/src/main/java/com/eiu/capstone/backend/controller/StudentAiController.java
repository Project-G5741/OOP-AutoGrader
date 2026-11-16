package com.eiu.capstone.backend.controller;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.ai.StudentAiFeedbackService;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentTermAccessService;

@RestController
@RequestMapping("/api/students/ai")
@Profile("!desktop")
public class StudentAiController {

    private final StudentAiFeedbackService aiFeedbackService;
    private final JwtAuthHelper jwtAuthHelper;
    private final StudentTermAccessService studentTermAccessService;

    public StudentAiController(
            StudentAiFeedbackService aiFeedbackService,
            JwtAuthHelper jwtAuthHelper,
            StudentTermAccessService studentTermAccessService) {
        this.aiFeedbackService = aiFeedbackService;
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentTermAccessService = studentTermAccessService;
    }

    @GetMapping("/status")
    public StudentAiFeedbackService.FeatureStatus status(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) UUID labId) {
        var user = jwtAuthHelper.requireActiveUser(principal);
        return aiFeedbackService.status(user.getId(), labId);
    }

    @GetMapping("/token")
    public StudentAiFeedbackService.TokenStatus token(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var user = jwtAuthHelper.requireActiveUser(principal);
        return aiFeedbackService.tokenStatusFor(user.getId());
    }

    @PostMapping("/connect")
    public StudentAiFeedbackService.TokenStatus connect(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody ConnectRequest request) {
        var user = jwtAuthHelper.requireActiveUser(principal);
        if (request == null || request.apiKey() == null || request.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apiKey is required");
        }
        return aiFeedbackService.saveKey(user.getId(), request.apiKey());
    }

    @DeleteMapping("/connect")
    public StudentAiFeedbackService.TokenStatus disconnect(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var user = jwtAuthHelper.requireActiveUser(principal);
        return aiFeedbackService.disconnect(user.getId());
    }

    @PostMapping("/hint")
    public StudentAiFeedbackService.HintResponse hint(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody HintRequest request) {
        var user = jwtAuthHelper.requireActiveUser(principal);
        if (request == null || request.labId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "labId is required");
        }
        if (principal.isStudentOnly()) {
            studentTermAccessService.requireUploadAccess(principal.email(), request.labId());
        }
        return aiFeedbackService.requestHint(
                user.getId(),
                request.labId(),
                request.submissionId(),
                request.learningArea(),
                request.language());
    }

    public record HintRequest(UUID labId, UUID submissionId, String learningArea, String language) {}

    public record ConnectRequest(String apiKey) {}
}
