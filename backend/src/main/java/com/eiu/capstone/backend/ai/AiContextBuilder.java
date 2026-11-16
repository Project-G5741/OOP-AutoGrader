package com.eiu.capstone.backend.ai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Builds minimised Gemini context. Never includes expected identifiers, signatures,
 * hidden tests, or raw student source.
 */
@Component
public class AiContextBuilder {

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
                "mustNot", java.util.List.of(
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
