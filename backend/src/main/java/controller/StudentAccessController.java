package com.eiu.capstone.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.TermAccessDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentTermAccessService;

@RestController
@RequestMapping("/api/students")
public class StudentAccessController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentTermAccessService studentTermAccessService;

    public StudentAccessController(JwtAuthHelper jwtAuthHelper,
                                   StudentTermAccessService studentTermAccessService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentTermAccessService = studentTermAccessService;
    }

    @GetMapping("/term-access")
    public TermAccessDTO termAccess(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return new TermAccessDTO(studentTermAccessService.isInCurrentTerm(user));
    }
}
