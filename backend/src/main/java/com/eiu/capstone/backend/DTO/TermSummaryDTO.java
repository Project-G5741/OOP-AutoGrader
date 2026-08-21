package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;
import java.util.UUID;

public record TermSummaryDTO(
        UUID id,
        String label,
        LocalDate endDate,
        String yearLabel,
        int termNumber,
        boolean current,
        int studentCount) {
}
