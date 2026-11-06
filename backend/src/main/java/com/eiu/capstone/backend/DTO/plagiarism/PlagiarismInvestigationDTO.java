package com.eiu.capstone.backend.DTO.plagiarism;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlagiarismInvestigationDTO(
        UUID labId,
        UUID focusStudentId,
        UUID originStudentId,
        String originStudentName,
        String originStudentCode,
        List<PlagiarismLineageNodeDTO> nodes,
        List<PlagiarismLineageEdgeDTO> edges,
        String message
) {
    public record PlagiarismLineageNodeDTO(
            UUID studentId,
            String studentName,
            String studentCode,
            String earliestSubmittedAt,
            String role,
            boolean origin,
            boolean focus
    ) {}

    public record PlagiarismLineageEdgeDTO(
            UUID fromStudentId,
            UUID toStudentId,
            boolean gitMatch,
            boolean metadataMatch,
            BigDecimal hashSimilarity
    ) {}
}
