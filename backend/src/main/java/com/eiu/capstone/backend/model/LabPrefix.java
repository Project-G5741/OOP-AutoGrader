package com.eiu.capstone.backend.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Stored lab prefix. Official labs count in the lecturer Score total; Practice labs do not. */
public final class LabPrefix {

    public static final String OFFICIAL = "Official";
    public static final String PRACTICE = "Practice";

    private LabPrefix() {}

    public static String require(String raw) {
        if (OFFICIAL.equals(raw) || PRACTICE.equals(raw)) {
            return raw;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prefix must be Official or Practice");
    }
}
