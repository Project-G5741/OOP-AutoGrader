package com.eiu.capstone.backend.service.compile;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;

/**
 * One short Class-card / testcase line for a javac failure.
 */
public final class CompileErrorMessage {

    private static final Pattern EXPECTED_TOKEN = Pattern.compile("'([^']+)' expected");
    private static final Pattern SYMBOL_LINE = Pattern.compile(
            "(?m)^\\s*symbol:\\s+(?:class|interface|enum|record|variable|method|constructor)\\s+(\\S+)");
    private static final Pattern SYMBOL_INLINE = Pattern.compile(
            "(?i)cannot find symbol[:\\s]+(?:class|interface|enum|record|variable|method|constructor)\\s+(\\S+)");
    private static final Pattern LINE_NUMBER = Pattern.compile("(?i)\\bline\\s+(\\d+)");
    private static final String FILENAME_MISMATCH = "Class name must match file";
    private static final String LEGACY_POINTER = "Compilation Error on ";

    private CompileErrorMessage() {
    }

    public static String forDiagnostic(Diagnostic<?> diagnostic) {
        if (diagnostic == null) {
            return "Compile error";
        }
        String raw = diagnostic.getMessage(Locale.ENGLISH);
        if (raw == null || raw.isBlank()) {
            raw = diagnostic.getMessage(Locale.getDefault());
        }
        return summarize(raw == null ? "" : raw, diagnostic.getLineNumber());
    }

    public static String see(String rootClassName) {
        if (rootClassName == null || rootClassName.isBlank()) {
            return "See failed class";
        }
        return "See " + rootClassName;
    }

    public static String forFileRoot(String typeName,
                                     String fileStem,
                                     Set<String> declaredInFile,
                                     String fileDiagnostic) {
        String diagnostic = isBlank(fileDiagnostic) ? "Compile error" : fileDiagnostic;
        Set<String> declared = declaredInFile == null ? Set.of() : declaredInFile;
        boolean declaredHere = typeName != null && declared.contains(typeName);
        if (fileStem != null && fileStem.equals(typeName) && !declaredHere) {
            return "Wrong class in this file";
        }
        if (isStrayTopLevel(typeName, fileStem, declared, declaredHere, diagnostic)) {
            return "Declared in " + fileStem + ".java";
        }
        return diagnostic;
    }

    public static String summarize(String raw) {
        if (isBlank(raw)) {
            return "Compile error";
        }
        String first = firstLine(raw);
        if (first.regionMatches(true, 0, "See ", 0, 4)) {
            return first;
        }
        if (first.regionMatches(true, 0, LEGACY_POINTER, 0, LEGACY_POINTER.length())) {
            return see(first.substring(LEGACY_POINTER.length()).trim());
        }
        return summarize(raw, extractLineNumber(raw));
    }

    public static String summarize(String raw, long line) {
        if (isBlank(raw)) {
            return withLine("Compile error", line);
        }
        String text = raw.replace('\r', '\n');
        String lower = text.toLowerCase(Locale.ENGLISH);

        if (lower.contains("should be declared in a file named")) {
            return FILENAME_MISMATCH;
        }
        if (lower.contains("cannot find symbol")) {
            String symbol = extractSymbol(text);
            return symbol != null ? symbol + " not found" : "Symbol not found";
        }
        if (lower.contains("reached end of file")) {
            return withLine("Unclosed class", line);
        }
        if (lower.contains("duplicate class")) {
            return "Duplicate class";
        }

        Matcher expected = EXPECTED_TOKEN.matcher(text);
        if (expected.find()) {
            return withLine("Missing " + expected.group(1), line);
        }
        if (lower.contains("illegal start")) {
            return withLine("Invalid syntax", line);
        }

        String first = firstLine(text).replaceFirst("(?i)^error:\\s*", "");
        if (line > 0 && !first.toLowerCase(Locale.ENGLISH).contains("line")) {
            return first + " on line " + line;
        }
        return first;
    }

    private static boolean isStrayTopLevel(String typeName,
                                           String fileStem,
                                           Set<String> declared,
                                           boolean declaredHere,
                                           String diagnostic) {
        return fileStem != null
                && declaredHere
                && !fileStem.equals(typeName)
                && typeName.indexOf('.') < 0
                && !declared.contains(fileStem)
                && FILENAME_MISMATCH.equals(diagnostic);
    }

    private static String extractSymbol(String text) {
        Matcher matcher = SYMBOL_LINE.matcher(text);
        if (!matcher.find()) {
            matcher = SYMBOL_INLINE.matcher(text);
            if (!matcher.find()) {
                return null;
            }
        }
        String symbol = matcher.group(1);
        int paren = symbol.indexOf('(');
        return paren > 0 ? symbol.substring(0, paren) : symbol;
    }

    private static long extractLineNumber(String raw) {
        Matcher matcher = LINE_NUMBER.matcher(raw);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
    }

    private static String firstLine(String raw) {
        return raw.lines()
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .findFirst()
                .orElse("Compile error");
    }

    private static String withLine(String summary, long line) {
        return line <= 0 ? summary : summary + " on line " + line;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
