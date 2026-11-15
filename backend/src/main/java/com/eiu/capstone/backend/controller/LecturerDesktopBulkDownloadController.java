package com.eiu.capstone.backend.controller;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.LecturerDesktopBulkBundleService;
import com.eiu.capstone.backend.service.LecturerDesktopBulkBundleService.BundleDownload;

@RestController
@RequestMapping("/api/lecturer")
@Profile("!desktop & !desktop-lecturer")
public class LecturerDesktopBulkDownloadController {

    private final JwtAuthHelper jwtAuthHelper;
    private final LecturerDesktopBulkBundleService lecturerDesktopBulkBundleService;

    public LecturerDesktopBulkDownloadController(
            JwtAuthHelper jwtAuthHelper,
            LecturerDesktopBulkBundleService lecturerDesktopBulkBundleService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.lecturerDesktopBulkBundleService = lecturerDesktopBulkBundleService;
    }

    @GetMapping("/desktop-bulk-bundle")
    public ResponseEntity<Resource> downloadDesktopBulkBundle(
            @AuthenticationPrincipal JwtUserPrincipal principal) throws IOException {
        jwtAuthHelper.requireActiveUser(principal);
        BundleDownload bundle = lecturerDesktopBulkBundleService.buildForLecturer();
        FileSystemResource resource = new FileSystemResource(bundle.zipFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + bundle.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
