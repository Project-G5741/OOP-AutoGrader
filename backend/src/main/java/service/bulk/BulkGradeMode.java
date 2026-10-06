package com.eiu.capstone.backend.service.bulk;

public enum BulkGradeMode {
    LAB,
    EXAM;

    public static BulkGradeMode fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("mode is required (LAB or EXAM)");
        }
        try {
            return BulkGradeMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("mode must be LAB or EXAM");
        }
    }
}
