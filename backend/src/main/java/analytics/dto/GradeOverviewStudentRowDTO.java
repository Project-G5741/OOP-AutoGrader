package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradeOverviewStudentRowDTO(
        UUID studentId,
        String studentName,
        String irn,
        BigDecimal totalScore,
        List<BigDecimal> labScores) {
}
