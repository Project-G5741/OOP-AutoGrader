package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Term;

/**
 * Absolute quarter index for comparing terms across academic years.
 * Q1 2025-2026 and Q4 2025-2026 are three quarters apart (retention window).
 */
public final class TermOrdinal {

    private static final int BASE_YEAR = 2000;

    private TermOrdinal() {}

    public static int fromTerm(Term term) {
        if (term == null || term.getAcademicYear() == null) {
            throw new IllegalArgumentException("Term and academic year are required");
        }
        return fromYearAndNumber(term.getAcademicYear(), term.getTermNumber());
    }

    public static int fromYearAndNumber(AcademicYear academicYear, int termNumber) {
        int startYear = parseStartYear(academicYear.getYearLabel());
        return toOrdinal(startYear, termNumber);
    }

    public static int toOrdinal(int startYear, int termNumber) {
        if (termNumber < 1 || termNumber > 4) {
            throw new IllegalArgumentException("Term number must be between 1 and 4");
        }
        return (startYear - BASE_YEAR) * 4 + (termNumber - 1);
    }

    static int parseStartYear(String yearLabel) {
        if (yearLabel == null || yearLabel.isBlank()) {
            throw new IllegalArgumentException("Academic year label is required");
        }
        String trimmed = yearLabel.trim();
        int dash = trimmed.indexOf('-');
        String yearPart = dash > 0 ? trimmed.substring(0, dash) : trimmed;
        try {
            return Integer.parseInt(yearPart.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid academic year label: " + yearLabel);
        }
    }
}
