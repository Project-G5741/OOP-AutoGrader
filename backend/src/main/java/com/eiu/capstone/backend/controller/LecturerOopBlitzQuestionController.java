package com.eiu.capstone.backend.controller;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.OopBlitzBankListResponse;
import com.eiu.capstone.backend.DTO.OopBlitzBankQuestionView;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionActiveRequest;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportRequest;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportResponse;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionUpsertRequest;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.LecturerOopBlitzQuestionService;

/**
 * Lecturer and Admin management endpoints for custom OOP Blitz questions.
 */
@RestController
@RequestMapping("/api/lecturer/oop-blitz/questions")
@Profile("!desktop & !desktop-lecturer")
public class LecturerOopBlitzQuestionController {

    private final JwtAuthHelper jwtAuthHelper;
    private final LecturerOopBlitzQuestionService questionService;

    public LecturerOopBlitzQuestionController(
            JwtAuthHelper jwtAuthHelper,
            LecturerOopBlitzQuestionService questionService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.questionService = questionService;
    }

    @GetMapping
    public OopBlitzBankListResponse list(@AuthenticationPrincipal JwtUserPrincipal principal) {
        jwtAuthHelper.requireActiveUser(principal);
        return questionService.listBank();
    }

    @PostMapping
    public OopBlitzBankQuestionView create(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody OopBlitzQuestionUpsertRequest request) {
        UserAccount actor = jwtAuthHelper.requireActiveUser(principal);
        return questionService.create(request, actor.getId());
    }

    @PutMapping("/{id}")
    public OopBlitzBankQuestionView update(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody OopBlitzQuestionUpsertRequest request) {
        jwtAuthHelper.requireActiveUser(principal);
        return questionService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public OopBlitzBankQuestionView setActive(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody OopBlitzQuestionActiveRequest request) {
        jwtAuthHelper.requireActiveUser(principal);
        return questionService.setActive(id, request != null && request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id) {
        jwtAuthHelper.requireActiveUser(principal);
        questionService.delete(id);
    }

    @PostMapping("/import")
    public OopBlitzQuestionImportResponse importBatch(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody OopBlitzQuestionImportRequest request) {
        UserAccount actor = jwtAuthHelper.requireActiveUser(principal);
        return questionService.bulkImport(request, actor.getId());
    }
}
