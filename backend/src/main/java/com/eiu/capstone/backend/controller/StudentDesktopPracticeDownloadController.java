package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentDesktopPracticeBundleService;
import com.eiu.capstone.backend.service.StudentDesktopPracticeBundleService.BundleDownload;

@RestController
@RequestMapping("/api/students")
@Profile("!desktop")
public class StudentDesktopPracticeDownloadController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentDesktopPracticeBundleService desktopPracticeBundleService;

    public StudentDesktopPracticeDownloadController(
            JwtAuthHelper jwtAuthHelper,
            StudentDesktopPracticeBundleService desktopPracticeBundleService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.desktopPracticeBundleService = desktopPracticeBundleService;
    }

    @GetMapping("/desktop-practice-bundle")
    public ResponseEntity<byte[]> downloadDesktopPracticeBundle(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        BundleDownload bundle = desktopPracticeBundleService.buildForStudent(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + bundle.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bundle.bytes());
    }
}
