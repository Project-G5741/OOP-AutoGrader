package com.eiu.capstone.backend.DTO.plagiarism;

import java.math.BigDecimal;
import java.util.UUID;

public record PlagiarismMatchDTO(
        UUID submissionId,
        UUID otherSubmissionId,
        String studentName,
        String studentCode,
        String otherStudentName,
        String otherStudentCode,
        boolean gitMatch,
        boolean metadataMatch,
        BigDecimal hashSimilarity,
        boolean flagged) {}
