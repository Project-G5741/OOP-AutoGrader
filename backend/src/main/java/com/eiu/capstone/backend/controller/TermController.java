package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.service.TermService;

@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermService termService;
    private final JwtAuthHelper jwtAuthHelper;

    public TermController(TermService termService, JwtAuthHelper jwtAuthHelper) {
        this.termService = termService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @GetMapping
    public List<TermSummaryDTO> listTerms(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        jwtAuthHelper.requireLecturer(authHeader);
        return termService.listTerms();
    }
}
