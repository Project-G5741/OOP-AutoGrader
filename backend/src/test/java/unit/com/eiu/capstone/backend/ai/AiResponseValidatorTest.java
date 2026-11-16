package unit.com.eiu.capstone.backend.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.ai.AiResponseValidator;

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
}
