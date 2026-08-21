package com.eiu.capstone.backend.DTO;

import java.util.List;

public record StudentHistoryResponse(
        List<StudentSubmissionHistoryItemDTO> submissions,
        StudentHistoryStatsDTO stats,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
