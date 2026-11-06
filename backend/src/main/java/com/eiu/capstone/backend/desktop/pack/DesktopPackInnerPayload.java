package com.eiu.capstone.backend.desktop.pack;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cleartext JSON inside the encrypted blob of a desktop practice pack.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DesktopPackInnerPayload(
        String packVersion,
        UUID termId,
        String termLabel,
        List<DesktopPackLabEntry> labs) {

    public DesktopPackInnerPayload {
        if (packVersion == null || packVersion.isBlank()) {
            throw new IllegalArgumentException("packVersion is required");
        }
        if (termId == null) {
            throw new IllegalArgumentException("termId is required");
        }
        if (labs == null || labs.isEmpty()) {
            throw new IllegalArgumentException("labs must not be empty");
        }
    }
}
