package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.CurrentTermMailRequest;
import com.eiu.capstone.backend.DTO.CurrentTermMailResult;
import com.eiu.capstone.backend.service.CurrentTermMailService;

import jakarta.validation.Valid;

@RestController
@Profile("!desktop")
@RequestMapping("/api/lecturer/terms")
public class LecturerTermMailController {

    private final CurrentTermMailService currentTermMailService;

    public LecturerTermMailController(CurrentTermMailService currentTermMailService) {
        this.currentTermMailService = currentTermMailService;
    }

    @PostMapping("/mail")
    public CurrentTermMailResult mailCurrentTerm(@Valid @RequestBody CurrentTermMailRequest request) {
        return currentTermMailService.sendToCurrentTerm(request);
    }
}
