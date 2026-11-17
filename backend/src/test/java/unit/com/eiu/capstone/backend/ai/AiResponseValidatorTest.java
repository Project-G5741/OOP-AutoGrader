package unit.com.eiu.capstone.backend.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.ai.AiResponseValidator;
import com.eiu.capstone.backend.ai.ChallengeAiFeedbackPayload;
import com.eiu.capstone.backend.ai.ClassGuidanceItem;
import com.eiu.capstone.backend.ai.GuidanceDetail;

class AiResponseValidatorTest {

    private final AiResponseValidator validator = new AiResponseValidator();

    @Test
    void acceptsSafeConceptualHint() {
        var result = validator.validate(
                "Your class models some customer information.",
                "What other information might help distinguish one customer from another?",
                "Think about data modelling responsibilities.",
                Set.of("customerName", "calculateTotal"));
        assertTrue(result.passed());
    }

    @Test
    void rejectsExactMissingFieldIdentifier() {
        var result = validator.validate(
                "Almost there.",
                "You forgot to declare customerName.",
                null,
                Set.of("customerName"));
        assertFalse(result.passed());
    }

    @Test
    void rejectsJavaCodeBlock() {
        var result = validator.validate(
                "Add this.",
                "```java\nprivate String name;\n```",
                null,
                Set.of());
        assertFalse(result.passed());
    }

    @Test
    void rejectsMethodSignature() {
        var result = validator.validate(
                "Try this approach.",
                "Declare public double calculateTotal() {",
                null,
                Set.of("calculateTotal"));
        assertFalse(result.passed());
    }

    @Test
    void rejectsBacktickIdentifierMatchingProhibited() {
        var result = validator.validate(
                "Consider the missing piece.",
                "Look at `customerName`.",
                null,
                Set.of("customerName"));
        assertFalse(result.passed());
    }

    @Test
    void acceptsSafeChallengePayloadAndAllowsVisibleClassName() {
        ChallengeAiFeedbackPayload payload = new ChallengeAiFeedbackPayload(
                "Circle class requires revisions to attributes and methods.",
                List.of(
                        new ClassGuidanceItem(
                                "Circle",
                                "CLASS",
                                "error",
                                null,
                                List.of(
                                        new GuidanceDetail(
                                                "Attribute Encapsulation",
                                                "Ensure that dimensions in Circle are private.",
                                                "What access modifier protects class attributes from direct modification?")))),
                "en");

        // "radius" is secret rubric identifier, but "Circle" is the allowed student class name
        var result = validator.validateChallengePayload(payload, Set.of("radius"), Set.of("Circle"));
        assertTrue(result.passed());
    }

    @Test
    void rejectsChallengePayloadLeakingProhibitedIdentifier() {
        ChallengeAiFeedbackPayload payload = new ChallengeAiFeedbackPayload(
                "Guidance for Challenge 1.",
                List.of(
                        new ClassGuidanceItem(
                                "Circle",
                                "CLASS",
                                "error",
                                null,
                                List.of(
                                        new GuidanceDetail(
                                                "Missing Field",
                                                "You need to declare radius as an integer.",
                                                "Have you added radius?")))),
                "en");

        var result = validator.validateChallengePayload(payload, Set.of("radius"), Set.of("Circle"));
        assertFalse(result.passed());
    }
}
