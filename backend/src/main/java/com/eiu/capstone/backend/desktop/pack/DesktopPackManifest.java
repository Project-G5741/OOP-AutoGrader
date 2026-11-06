package com.eiu.capstone.backend.desktop.pack;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cleartext metadata for a term desktop practice pack (inner payload describes rubric graph).
 */
public record DesktopPackManifest(
        int formatVersion,
        String packVersion,
        UUID termId,
        String termLabel,
        Instant createdAt,
        List<UUID> labIds,
        String contentSha256
) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public DesktopPackManifest {
        if (formatVersion != CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported formatVersion: " + formatVersion);
        }
        if (packVersion == null || packVersion.isBlank()) {
            throw new IllegalArgumentException("packVersion is required");
        }
        if (termId == null) {
            throw new IllegalArgumentException("termId is required");
        }
        if (labIds == null || labIds.isEmpty()) {
            throw new IllegalArgumentException("labIds must not be empty");
        }
        if (contentSha256 == null || contentSha256.isBlank()) {
            throw new IllegalArgumentException("contentSha256 is required");
        }
    }
}
