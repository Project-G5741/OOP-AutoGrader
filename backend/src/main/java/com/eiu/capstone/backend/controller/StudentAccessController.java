package com.eiu.capstone.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

import com.eiu.capstone.backend.DTO.StudentDailySubmissionQuotaView;
import com.eiu.capstone.backend.DTO.TermAccessDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentDailySubmissionQuotaService;
import com.eiu.capstone.backend.service.StudentTermAccessService;

@RestController
@RequestMapping("/api/students")
public class StudentAccessController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentTermAccessService studentTermAccessService;
    private final StudentDailySubmissionQuotaService dailySubmissionQuotaService;

    public StudentAccessController(JwtAuthHelper jwtAuthHelper,
                                   StudentTermAccessService studentTermAccessService,
                                   @Autowired(required = false)
                                   StudentDailySubmissionQuotaService dailySubmissionQuotaService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentTermAccessService = studentTermAccessService;
        this.dailySubmissionQuotaService = dailySubmissionQuotaService;
    }

    @GetMapping("/term-access")
    public TermAccessDTO termAccess(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentTermAccessService.termAccessFor(user);
    }

    @GetMapping("/daily-submission-quota")
    public StudentDailySubmissionQuotaView dailySubmissionQuota(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        if (dailySubmissionQuotaService == null) {
            return new StudentDailySubmissionQuotaView(0, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }
        return dailySubmissionQuotaService.viewFor(user.getId());
    }
}
