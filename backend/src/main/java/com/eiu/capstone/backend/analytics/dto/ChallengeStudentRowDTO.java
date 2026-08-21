package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChallengeStudentRowDTO(
        UUID studentId,
        String studentName,
        String studentCode,
        BigDecimal score,
        int attempts,
        String submittedAt,
        boolean hasSubmission,
        UUID submissionId,
        boolean plagiarismFlagged) {

    public ChallengeStudentRowDTO(UUID studentId,
                                  String studentName,
                                  String studentCode,
                                  BigDecimal score,
                                  int attempts,
                                  String submittedAt,
                                  boolean hasSubmission,
                                  UUID submissionId) {
        this(studentId, studentName, studentCode, score, attempts, submittedAt,
                hasSubmission, submissionId, false);
    }
}
