package com.eiu.capstone.backend.desktop.pack;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared lab name rules for create/edit, pack export, and pack import filenames.
 */
public final class LabNameRules {

    public static final int MAX_LENGTH = 50;

    private static final Pattern ILLEGAL = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

    private LabNameRules() {}

    public static String requireValid(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lab name must be at most " + MAX_LENGTH + " characters");
        }
        if (ILLEGAL.matcher(trimmed).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lab name cannot contain \\ / : * ? \" < > | or control characters");
        }
        return trimmed;
    }
}
