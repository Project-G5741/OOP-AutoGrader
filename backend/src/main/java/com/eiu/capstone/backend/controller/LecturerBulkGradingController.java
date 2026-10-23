package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkGradeResponse;
import com.eiu.capstone.backend.service.bulk.BulkGradeMode;
import com.eiu.capstone.backend.service.bulk.LecturerBulkGradingService;

/**
 * Lecturer-only ephemeral bulk grade: one student folder per call, no roster writes.
 */
@RestController
@RequestMapping("/api/lecturer/labs")
public class LecturerBulkGradingController {

    private final LecturerBulkGradingService lecturerBulkGradingService;

    public LecturerBulkGradingController(LecturerBulkGradingService lecturerBulkGradingService) {
        this.lecturerBulkGradingService = lecturerBulkGradingService;
    }

    @PostMapping(value = "/{labId}/bulk-grade", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkGradeResponse> bulkGrade(
            @PathVariable UUID labId,
            @RequestParam("mode") String modeParam,
            @RequestParam("files") List<MultipartFile> files) {
        BulkGradeMode mode;
        try {
            mode = BulkGradeMode.fromParam(modeParam);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ResponseEntity.ok(lecturerBulkGradingService.gradeOne(labId, mode, files));
    }
}
