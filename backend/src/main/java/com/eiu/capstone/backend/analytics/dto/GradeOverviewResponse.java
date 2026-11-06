package com.eiu.capstone.backend.analytics.dto;

import java.util.List;

public record GradeOverviewResponse(
        List<GradeOverviewLabColumnDTO> labs,
        List<GradeOverviewStudentRowDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
