package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.OopBlitzAbandonRequest;
import com.eiu.capstone.backend.DTO.OopBlitzAnswerRequest;
import com.eiu.capstone.backend.DTO.OopBlitzAnswerResponse;
import com.eiu.capstone.backend.DTO.OopBlitzStartRequest;
import com.eiu.capstone.backend.DTO.OopBlitzStartResponse;
import com.eiu.capstone.backend.DTO.OopBlitzStatusView;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentOopBlitzService;

@RestController
@RequestMapping("/api/students/oop-blitz")
@Profile("!desktop & !desktop-lecturer")
public class StudentOopBlitzController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentOopBlitzService oopBlitzService;

    public StudentOopBlitzController(
            JwtAuthHelper jwtAuthHelper,
            StudentOopBlitzService oopBlitzService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.oopBlitzService = oopBlitzService;
    }

    @GetMapping("/status")
    public OopBlitzStatusView status(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return oopBlitzService.getStatus(user.getId());
    }

    @PostMapping("/start")
    public OopBlitzStartResponse start(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody(required = false) OopBlitzStartRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return oopBlitzService.start(user.getId(), request == null ? null : request.mode());
    }

    @PostMapping("/answer")
    public OopBlitzAnswerResponse answer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody OopBlitzAnswerRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return oopBlitzService.answer(user.getId(), request);
    }

    @PostMapping("/abandon")
    public OopBlitzStatusView abandon(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody OopBlitzAbandonRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return oopBlitzService.abandon(user.getId(), request == null ? null : request.sessionId());
    }
}
