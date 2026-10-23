package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TermSummaryDTO(
        UUID id,
        String label,
        LocalDate endDate,
        String yearLabel,
        int termNumber,
        boolean current,
        int studentCount,
        List<String> cloneErrors) {

    public TermSummaryDTO(
            UUID id,
            String label,
            LocalDate endDate,
            String yearLabel,
            int termNumber,
            boolean current,
            int studentCount) {
        this(id, label, endDate, yearLabel, termNumber, current, studentCount, List.of());
    }
}
