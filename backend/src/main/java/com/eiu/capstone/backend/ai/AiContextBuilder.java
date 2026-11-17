package com.eiu.capstone.backend.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Builds minimised Gemini context. Never includes hidden tests, reference solution
 * implementations, or raw student source files.
 */
@Component
public class AiContextBuilder {

    public Map<String, Object> buildChallengeContext(
            String labName,
            String challengeName,
            List<FailingClassInfo> failingClasses) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("role", "oop_learning_assistant");
        ctx.put("labTopic", sanitize(labName));
        ctx.put("challengeName", sanitize(challengeName));
        ctx.put("language", "English");
        ctx.put("failingClasses", failingClasses != null ? failingClasses : List.of());
        ctx.put("instructions", Map.of(
                "style", "Supportive, conceptual OOP guidance with actionable advice and thoughtful questions",
                "mustNot", List.of(
                        "complete Java code blocks or implementation solutions",
                        "hidden testcase details or reference solution code",
                        "backticks around identifiers or words"),
                "outputFormat", "JSON object with challengeSummary and classes array"));
        return ctx;
    }

    public Map<String, Object> build(
            String labName,
            AiLearningArea area,
            String language,
            String policyVersion) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("role", "conceptual_learning_assistant");
        ctx.put("hintLevel", "CONCEPTUAL");
        ctx.put("learningArea", area.name());
        ctx.put("language", language);
        ctx.put("policyVersion", policyVersion);
        ctx.put("assignmentTopic", sanitize(labName));
        ctx.put("studentVisibleFacts", Map.of(
                "gradingComplete", true,
                "focus", focusBlurb(area),
                "displayLabelsOnly", true));
        ctx.put("instructions", Map.of(
                "style", "Socratic Conceptual questions and brief concept explanations",
                "mustNot", List.of(
                        "exact missing field/method/constructor names",
                        "Java code, signatures, or solution pseudocode",
                        "hidden tests or reference solutions",
                        "score changes or pass/fail decisions"),
                "output", "JSON with summary, question, explanation, concept"));
        return ctx;
    }

    private static String focusBlurb(AiLearningArea area) {
        return switch (area) {
            case FIELD_MODELING -> "Student may need to reconsider which information the class should model.";
            case METHODS -> "Student may need to reconsider object behaviours and responsibilities.";
            case CONSTRUCTORS -> "Student may need to reconsider how objects are initialised.";
        };
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "Lab";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 120) {
            return trimmed.substring(0, 120);
        }
        return trimmed.replaceAll("[\\r\\n\\t]+", " ");
    }
}
