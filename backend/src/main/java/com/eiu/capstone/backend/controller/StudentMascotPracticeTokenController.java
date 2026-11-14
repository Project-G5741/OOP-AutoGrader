package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.MascotPracticeTokenView;
import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.MascotPracticeTokenService;
import com.eiu.capstone.backend.service.StudentMascotService;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/students")
@Profile("!desktop")
public class StudentMascotPracticeTokenController {

    private final JwtAuthHelper jwtAuthHelper;
    private final MascotPracticeTokenService practiceTokens;
    private final StudentMascotService studentMascotService;

    public StudentMascotPracticeTokenController(
            JwtAuthHelper jwtAuthHelper,
            MascotPracticeTokenService practiceTokens,
            StudentMascotService studentMascotService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.practiceTokens = practiceTokens;
        this.studentMascotService = studentMascotService;
    }

    @PostMapping("/mascot-practice-token")
    public MascotPracticeTokenView issue(@AuthenticationPrincipal JwtUserPrincipal principal) {
        MascotPracticeTokenService.requireStudentOrAdmin(principal);
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return practiceTokens.issue(user, false);
    }

    @PostMapping("/mascot-practice-token/regenerate")
    public MascotPracticeTokenView regenerate(@AuthenticationPrincipal JwtUserPrincipal principal) {
        MascotPracticeTokenService.requireStudentOrAdmin(principal);
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return practiceTokens.issue(user, true);
    }

    @GetMapping("/mascot-snapshot")
    public MascotSnapshotView snapshot(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentMascotService.snapshot(user.getId());
    }
}
