package com.eiu.capstone.backend.ai;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Fail-closed checks before any Gemini text reaches the student.
 * Prohibited identifiers are loaded server-side only — never sent to Gemini.
 */
@Component
public class AiResponseValidator {

    private static final int MAX_SUMMARY = 1200;
    private static final int MAX_QUESTION = 500;
    private static final int MAX_EXPLANATION = 1200;

    private static final Pattern CODE_FENCE = Pattern.compile("```");
    private static final Pattern JAVAISH = Pattern.compile(
            "(?i)\\b(private|public|protected|static|final|void|class|interface|enum)\\b\\s+[\\w<>,\\[\\]\\s]+[;{]|\\bnew\\s+[A-Z]\\w*\\s*\\(");
    private static final Pattern METHOD_SIG = Pattern.compile(
            "(?i)\\b(public|private|protected)?\\s*(static\\s+)?[\\w<>\\[\\]]+\\s+[a-z]\\w*\\s*\\([^)]*\\)\\s*\\{?");
    private static final Pattern BACKTICK_IDENT = Pattern.compile("`([A-Za-z_][\\w$]*)`");

    public ValidationResult validateChallengePayload(
            ChallengeAiFeedbackPayload payload,
            Collection<String> prohibitedIdentifiers,
            Set<String> allowedClassNames) {
        if (payload == null || blank(payload.challengeSummary())) {
            return ValidationResult.reject("missing_required_summary");
        }
        if (payload.challengeSummary().length() > MAX_SUMMARY) {
            return ValidationResult.reject("summary_length");
        }

        StringBuilder blob = new StringBuilder(payload.challengeSummary());
        if (payload.classes() != null) {
            for (ClassGuidanceItem item : payload.classes()) {
                if (item.guidance() != null) {
                    for (GuidanceDetail detail : item.guidance()) {
                        if (blank(detail.title()) || blank(detail.explanation())) {
                            return ValidationResult.reject("missing_guidance_text");
                        }
                        if (detail.explanation().length() > MAX_EXPLANATION
                                || (detail.guidingQuestion() != null && detail.guidingQuestion().length() > MAX_QUESTION)) {
                            return ValidationResult.reject("guidance_length");
                        }
                        blob.append(' ').append(detail.title());
                        blob.append(' ').append(detail.explanation());
                        if (detail.guidingQuestion() != null) {
                            blob.append(' ').append(detail.guidingQuestion());
                        }
                    }
                }
            }
        }

        String text = blob.toString();
        if (CODE_FENCE.matcher(text).find() || JAVAISH.matcher(text).find() || METHOD_SIG.matcher(text).find()) {
            return ValidationResult.reject("code_or_signature");
        }
        if (looksLikeSolutionPseudocode(text)) {
            return ValidationResult.reject("solution_equivalent");
        }

        Set<String> prohibited = prohibitedIdentifiers == null ? Set.of() : Set.copyOf(prohibitedIdentifiers);
        Set<String> allowed = allowedClassNames == null ? Set.of() : allowedClassNames;

        for (String token : prohibited) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String trimmed = token.trim();
            if (allowed.stream().anyMatch(a -> a.equalsIgnoreCase(trimmed))) {
                continue;
            }
            if (containsIdentifierToken(text, trimmed)) {
                return ValidationResult.reject("identifier_leak:" + trimmed);
            }
        }

        Matcher tick = BACKTICK_IDENT.matcher(text);
        while (tick.find()) {
            String inner = tick.group(1);
            if (allowed.stream().anyMatch(a -> a.equalsIgnoreCase(inner))) {
                continue;
            }
            if (prohibited.stream().anyMatch(p -> p != null && p.equalsIgnoreCase(inner))) {
                return ValidationResult.reject("identifier_leak:" + inner);
            }
            if (inner.length() >= 2 && Character.isLetter(inner.charAt(0)) && !isCommonEnglish(inner)) {
                return ValidationResult.reject("backtick_identifier:" + inner);
            }
        }
        return ValidationResult.pass();
    }

    public ValidationResult validate(String summary, String question, String explanation, Collection<String> prohibitedIdentifiers) {
        if (blank(summary) || blank(question)) {
            return ValidationResult.reject("missing_required_text");
        }
        if (summary.length() > MAX_SUMMARY || question.length() > MAX_QUESTION
                || (explanation != null && explanation.length() > MAX_EXPLANATION)) {
            return ValidationResult.reject("length");
        }
        String blob = join(summary, question, explanation);
        if (CODE_FENCE.matcher(blob).find() || JAVAISH.matcher(blob).find() || METHOD_SIG.matcher(blob).find()) {
            return ValidationResult.reject("code_or_signature");
        }
        if (looksLikeSolutionPseudocode(blob)) {
            return ValidationResult.reject("solution_equivalent");
        }
        Set<String> prohibited = prohibitedIdentifiers == null ? Set.of() : Set.copyOf(prohibitedIdentifiers);
        for (String token : prohibited) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (containsIdentifierToken(blob, token.trim())) {
                return ValidationResult.reject("identifier_leak");
            }
        }
        Matcher tick = BACKTICK_IDENT.matcher(blob);
        while (tick.find()) {
            String inner = tick.group(1);
            if (prohibited.stream().anyMatch(p -> p != null && p.equalsIgnoreCase(inner))) {
                return ValidationResult.reject("identifier_leak");
            }
            // Fail closed on camelCase/PascalCase identifiers in backticks (likely Java names).
            if (inner.length() >= 2 && Character.isLetter(inner.charAt(0)) && !isCommonEnglish(inner)) {
                return ValidationResult.reject("backtick_identifier");
            }
        }
        return ValidationResult.pass();
    }

    static boolean containsIdentifierToken(String text, String identifier) {
        if (identifier.length() < 2) {
            return false;
        }
        Pattern p = Pattern.compile("(?<!\\w)" + Pattern.quote(identifier) + "(?!\\w)", Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    private static boolean looksLikeSolutionPseudocode(String blob) {
        String lower = blob.toLowerCase(Locale.ROOT);
        return lower.contains("add private")
                || lower.contains("declare a field")
                || lower.contains("create a method named")
                || lower.contains("write the method")
                || lower.contains("implement the constructor as");
    }

    private static boolean isCommonEnglish(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return Set.of(
                "class", "object", "method", "field", "constructor", "customer", "name", "age",
                "address", "data", "information", "state", "behavior", "responsibility", "design",
                "instance", "parameter", "type", "value", "record", "system").contains(t);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                sb.append(' ').append(p);
            }
        }
        return sb.toString();
    }

    public record ValidationResult(boolean passed, String reason) {
        public static ValidationResult pass() { return new ValidationResult(true, null); }
        public static ValidationResult reject(String reason) { return new ValidationResult(false, reason); }
    }
}
