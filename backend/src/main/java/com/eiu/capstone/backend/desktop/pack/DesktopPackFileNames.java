package com.eiu.capstone.backend.desktop.pack;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Canonical offline practice pack filenames: {@code {uuid}.term.agpack} and {@code {uuid}.lab.agpack}.
 */
public final class DesktopPackFileNames {

    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\.(term|lab)\\.agpack$");

    public enum PackKind {
        TERM,
        LAB
    }

    private DesktopPackFileNames() {}

    public static String termFilename(UUID termId) {
        return termId + ".term.agpack";
    }

    public static String labFilename(UUID labId) {
        return labId + ".lab.agpack";
    }

    public static ParsedFilename parse(String filename) {
        if (filename == null || filename.isBlank()) {
            throw invalidName(filename);
        }
        String base = filename.trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        Matcher matcher = FILE_PATTERN.matcher(base);
        if (!matcher.matches()) {
            throw invalidName(base);
        }
        UUID id = UUID.fromString(matcher.group(1));
        PackKind kind = "term".equalsIgnoreCase(matcher.group(2)) ? PackKind.TERM : PackKind.LAB;
        return new ParsedFilename(base, kind, id);
    }

    public static void assertMatchesPayload(ParsedFilename parsed, DesktopPackManifest manifest,
                                            DesktopPackInnerPayload inner) {
        if (parsed.kind() == PackKind.TERM) {
            if (!parsed.id().equals(manifest.termId()) || !parsed.id().equals(inner.termId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Term pack filename must match the term id inside the pack");
            }
            return;
        }
        if (manifest.labIds().size() != 1 || inner.labs().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab pack must contain exactly one lab");
        }
        UUID labId = manifest.labIds().get(0);
        UUID innerLabId = inner.labs().get(0).lab().id();
        if (!parsed.id().equals(labId) || !parsed.id().equals(innerLabId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lab pack filename must match the lab id inside the pack");
        }
    }

    private static ResponseStatusException invalidName(String name) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid practice pack name. Use {term-uuid}.term.agpack or {lab-uuid}.lab.agpack (example: "
                        + UUID.randomUUID() + ".term.agpack)");
    }

    public record ParsedFilename(String filename, PackKind kind, UUID id) {
        public ParsedFilename {
            filename = filename == null ? "" : filename;
        }
    }

    public static boolean isPracticePackName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            parse(name);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }
}
