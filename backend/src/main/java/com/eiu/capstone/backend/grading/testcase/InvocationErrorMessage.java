package com.eiu.capstone.backend.grading.testcase;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plain-language lines for operational-testcase invocation failures shown to students.
 */
public final class InvocationErrorMessage {

    private static final Pattern JVM_MEMBER = Pattern.compile(
            "^(?:[\\w$]+\\.)*([\\w$]+)\\.(<init>|\\w+)\\(([^)]*)\\)$");
    private static final Pattern UNKNOWN_INSTANCE = Pattern.compile(
            "^Unknown named instance:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIDDEN_RECEIVER = Pattern.compile(
            "^Instance method requires a constructible hidden receiver on\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);

    private InvocationErrorMessage() {
    }

    public static String forStudent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "The test could not run your code.";
        }
        String text = raw.trim();

        Matcher ctorOrMethod = JVM_MEMBER.matcher(text);
        if (ctorOrMethod.matches()) {
            String className = ctorOrMethod.group(1);
            String member = ctorOrMethod.group(2);
            String params = formatParameterList(ctorOrMethod.group(3));
            if ("<init>".equals(member)) {
                if ("no parameters".equals(params)) {
                    return "Your " + className + " class does not have a no-argument constructor.";
                }
                return "Your " + className + " class does not have a constructor with parameters ("
                        + params + ").";
            }
            if ("no parameters".equals(params)) {
                return "Your " + className + " class does not have a method " + member + "().";
            }
            return "Your " + className + " class does not have a method " + member + "(" + params + ").";
        }

        Matcher unknown = UNKNOWN_INSTANCE.matcher(text);
        if (unknown.matches()) {
            return "This step uses object \"" + unknown.group(1).trim()
                    + "\", but it was not created in an earlier step.";
        }

        Matcher hidden = HIDDEN_RECEIVER.matcher(text);
        if (hidden.matches()) {
            return "Could not run the method on " + hidden.group(1).trim()
                    + " because the test could not construct that class automatically.";
        }

        if ("Invocation timed out".equalsIgnoreCase(text)) {
            return "Your code took too long on this test and was stopped.";
        }
        if ("No exception thrown".equalsIgnoreCase(text)) {
            return "Your code did not throw the expected exception.";
        }
        if ("Exception mismatch".equalsIgnoreCase(text)) {
            return "Your code threw a different exception than expected.";
        }
        if (text.toLowerCase(Locale.ENGLISH).startsWith("compilation error:")) {
            return text;
        }

        return text;
    }

    private static String formatParameterList(String raw) {
        if (raw == null || raw.isBlank()) {
            return "no parameters";
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(InvocationErrorMessage::simpleTypeName)
                .collect(Collectors.joining(", "));
    }

    private static String simpleTypeName(String type) {
        if (type == null) {
            return "";
        }
        String trimmed = type.trim();
        if (trimmed.startsWith("java.lang.")) {
            return trimmed.substring("java.lang.".length());
        }
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < trimmed.length() - 1) {
            return trimmed.substring(lastDot + 1);
        }
        return trimmed;
    }
}
