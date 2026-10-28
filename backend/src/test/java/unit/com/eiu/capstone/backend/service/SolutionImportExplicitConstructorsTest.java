package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.SolutionImportExplicitConstructors;

class SolutionImportExplicitConstructorsTest {

    @Test
    void emptyClass_hasNoExplicitConstructor() {
        assertFalse(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public class Solo { public int n; }",
                "Solo"));
    }

    @Test
    void explicitNoArg_isDetected() {
        assertTrue(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public class Solo { public Solo() {} }",
                "Solo"));
    }

    @Test
    void explicitParameterized_isDetected() {
        assertTrue(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public class Solo { public Solo(int x) {} }",
                "Solo"));
    }

    @Test
    void methodWithReturnType_isNotConstructor() {
        assertFalse(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public class Solo { public int Solo() { return 0; } }",
                "Solo"));
    }

    @Test
    void enumConstant_isNotConstructor() {
        assertFalse(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public enum Color { RED, GREEN }",
                "Color"));
    }

    @Test
    void enumExplicitConstructor_isDetected() {
        assertTrue(SolutionImportExplicitConstructors.hasExplicitConstructor(
                """
                        public enum Planet {
                            EARTH(1.0);
                            Planet(double m) {}
                        }
                        """,
                "Planet",
                "enum"));
    }

    @Test
    void recordHeaderCountsAsExplicitConstructor() {
        assertTrue(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public record Point(int x, int y) {}",
                "Point",
                "record"));
    }

    @Test
    void recordWithoutHeaderParams_hasNoExplicitConstructor() {
        assertFalse(SolutionImportExplicitConstructors.hasExplicitConstructor(
                "public record Empty {}",
                "Empty",
                "record"));
    }

    @Test
    void nestedStaticClass_detectsExplicitConstructor() {
        assertTrue(SolutionImportExplicitConstructors.hasExplicitConstructor(
                """
                        public class Pen {
                            public static class PenBuilder {
                                public PenBuilder() {}
                            }
                        }
                        """,
                "PenBuilder"));
    }
}
