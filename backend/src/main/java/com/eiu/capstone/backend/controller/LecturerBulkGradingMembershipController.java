package com.eiu.capstone.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.BulkCurrentTermStudentIrnsResponse;
import com.eiu.capstone.backend.service.bulk.LecturerBulkGradingService;

/**
 * Lecturer Bulk Grading membership helpers (IRN set for drop-time gates).
 */
@RestController
@RequestMapping("/api/lecturer/bulk-grading")
public class LecturerBulkGradingMembershipController {

    private final LecturerBulkGradingService lecturerBulkGradingService;

    public LecturerBulkGradingMembershipController(LecturerBulkGradingService lecturerBulkGradingService) {
        this.lecturerBulkGradingService = lecturerBulkGradingService;
    }

    @GetMapping("/current-term-student-irns")
    public ResponseEntity<BulkCurrentTermStudentIrnsResponse> currentTermStudentIrns() {
        return ResponseEntity.ok(lecturerBulkGradingService.listCurrentTermStudentIrns());
    }
}
