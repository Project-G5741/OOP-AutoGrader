package com.eiu.capstone.backend.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Stored lab prefix. Only Official labs count in the lecturer Score total; Practice and Midterm do not. */
public final class LabPrefix {

    public static final String OFFICIAL = "Official";
    public static final String PRACTICE = "Practice";
    public static final String MIDTERM = "Midterm";

    private LabPrefix() {}

    public static String require(String raw) {
        if (OFFICIAL.equals(raw) || PRACTICE.equals(raw) || MIDTERM.equals(raw)) {
            return raw;
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "prefix must be Official, Practice, or Midterm");
    }

    /** Whether the lab column value is included in Score Total (Official-only average). */
    public static boolean countsInScoreTotal(String prefix) {
        return OFFICIAL.equals(prefix);
    }
}
