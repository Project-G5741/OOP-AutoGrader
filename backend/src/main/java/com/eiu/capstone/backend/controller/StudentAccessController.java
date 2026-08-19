package com.eiu.capstone.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.TermAccessDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.service.StudentTermAccessService;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
public class StudentAccessController {

    private final JwtAuthHelper jwtAuthHelper;
    private final UserAccountRepository userAccountRepository;
    private final StudentTermAccessService studentTermAccessService;

    public StudentAccessController(JwtAuthHelper jwtAuthHelper,
                                   UserAccountRepository userAccountRepository,
                                   StudentTermAccessService studentTermAccessService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.userAccountRepository = userAccountRepository;
        this.studentTermAccessService = studentTermAccessService;
    }

    @GetMapping("/term-access")
    public TermAccessDTO termAccess(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Claims claims = jwtAuthHelper.parseBearerToken(authHeader);
        jwtAuthHelper.requireRole(claims, "STUDENT");
        String email = claims.get("email", String.class);
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        return new TermAccessDTO(studentTermAccessService.isInCurrentTerm(user));
    }
}
