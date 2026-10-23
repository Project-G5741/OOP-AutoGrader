package com.eiu.capstone.backend.desktop.pack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.service.TermService;

/**
 * Canonical offline practice pack filenames: {@code Rubric_{yearLabel}_Q{n}.agpack}
 * and {@code Rubric_{labName}.agpack}.
 */
public final class DesktopPackFileNames {

    private static final Pattern TERM_PATTERN = Pattern.compile("^Rubric_(.+)_Q(\\d+)\\.agpack$");
    private static final Pattern LAB_PATTERN = Pattern.compile("^Rubric_(.+)\\.agpack$");

    public enum PackKind {
        TERM,
        LAB
    }

    private DesktopPackFileNames() {}

    public static String termFilename(String yearLabel, int termNumber) {
        String year = yearLabel == null ? "" : yearLabel.trim();
        if (year.isEmpty() || termNumber < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quarter for practice pack name");
        }
        return "Rubric_" + year + "_Q" + termNumber + ".agpack";
    }

    public static String labFilename(String labName) {
        String name = LabNameRules.requireValid(labName);
        return "Rubric_" + name + ".agpack";
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
        Matcher termMatcher = TERM_PATTERN.matcher(base);
        if (termMatcher.matches()) {
            String yearLabel = termMatcher.group(1);
            int termNumber = Integer.parseInt(termMatcher.group(2));
            if (yearLabel.isBlank() || termNumber < 1) {
                throw invalidName(base);
            }
            return new ParsedFilename(base, PackKind.TERM, yearLabel, termNumber, null);
        }
        Matcher labMatcher = LAB_PATTERN.matcher(base);
        if (labMatcher.matches()) {
            String labName = labMatcher.group(1);
            LabNameRules.requireValid(labName);
            return new ParsedFilename(base, PackKind.LAB, null, null, labName);
        }
        throw invalidName(base);
    }

    public static void assertMatchesPayload(ParsedFilename parsed, DesktopPackManifest manifest,
                                            DesktopPackInnerPayload inner) {
        if (parsed.kind() == PackKind.TERM) {
            String expectedLabel = TermService.buildTermLabel(parsed.yearLabel(), parsed.termNumber());
            String manifestLabel = manifest.termLabel() == null ? "" : manifest.termLabel();
            String innerLabel = inner.termLabel() == null ? "" : inner.termLabel();
            if (!expectedLabel.equals(manifestLabel) || !expectedLabel.equals(innerLabel)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Term pack filename must match the quarter inside the pack");
            }
            return;
        }
        if (manifest.labIds().size() != 1 || inner.labs().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab pack must contain exactly one lab");
        }
        String labName = inner.labs().get(0).lab().name();
        if (labName == null || !labName.equals(parsed.labName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lab pack filename must match the lab name inside the pack");
        }
    }

    private static ResponseStatusException invalidName(String name) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid practice pack name. Use Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack "
                        + "(example: Rubric_2026-2027_Q1.agpack)");
    }

    public record ParsedFilename(
            String filename,
            PackKind kind,
            String yearLabel,
            Integer termNumber,
            String labName) {
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
