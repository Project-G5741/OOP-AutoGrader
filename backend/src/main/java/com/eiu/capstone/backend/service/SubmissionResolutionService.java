package com.eiu.capstone.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.repository.LabSubmissionRepository;

@Service
public class SubmissionResolutionService {

    private final LabSubmissionRepository labSubmissionRepository;

    public SubmissionResolutionService(LabSubmissionRepository labSubmissionRepository) {
        this.labSubmissionRepository = labSubmissionRepository;
    }

    /** Latest attempt for (student, lab) — used for all student-facing grading display. */
    public UUID resolveLatestSubmissionId(UUID labId, UUID studentId) {
        if (studentId == null || labId == null) {
            return null;
        }
        return labSubmissionRepository
                .findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc(studentId, labId)
                .map(s -> s.getId())
                .orElse(null);
    }
}
