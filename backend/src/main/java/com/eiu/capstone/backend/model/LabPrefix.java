package com.eiu.capstone.backend.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Stored lab prefix. Only Official labs count in the lecturer Score total; Practice, Midterm, and Final do not. */
public final class LabPrefix {

    public static final String OFFICIAL = "Official";
    public static final String PRACTICE = "Practice";
    public static final String MIDTERM = "Midterm";
    public static final String FINAL = "Final";

    private LabPrefix() {}

    public static String require(String raw) {
        if (OFFICIAL.equals(raw) || PRACTICE.equals(raw) || MIDTERM.equals(raw) || FINAL.equals(raw)) {
            return raw;
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "prefix must be Official, Practice, Midterm, or Final");
    }

    /** Flat student upload folder (no {@code challenge_n}); remapped before compile. */
    public static boolean usesFlatStudentUpload(String prefix) {
        return MIDTERM.equals(prefix) || FINAL.equals(prefix);
    }

    /** Whether the lab column value is included in Score Total (Official-only average). */
    public static boolean countsInScoreTotal(String prefix) {
        return OFFICIAL.equals(prefix);
    }
}
