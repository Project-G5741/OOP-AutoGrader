package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.eiu.capstone.backend.service.OopBlitzDomains.UmlTuple;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.EngineEntry;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

/**
 * 2-Directional UML / Mermaid MMD question engine covering all 6 relation types
 * plus arrow-direction traps.
 */
final class OopBlitzUmlEngine {

    private OopBlitzUmlEngine() {
    }

    static List<EngineEntry> easyEntries() {
        return List.of(
                entry("uml-code-to-extends", OopBlitzUmlEngine::umlCodeToExtends),
                entry("uml-code-to-implements", OopBlitzUmlEngine::umlCodeToImplements),
                entry("uml-code-to-association", OopBlitzUmlEngine::umlCodeToAssociation),
                entry("uml-arrow-to-meaning", OopBlitzUmlEngine::umlArrowToMeaning));
    }

    static List<EngineEntry> mediumEntries() {
        return List.of(
                entry("uml-code-to-composition", OopBlitzUmlEngine::umlCodeToComposition),
                entry("uml-code-to-aggregation", OopBlitzUmlEngine::umlCodeToAggregation),
                entry("uml-to-code-lifecycle", OopBlitzUmlEngine::umlToCodeLifecycle));
    }

    static List<EngineEntry> hardEntries() {
        return List.of(
                entry("uml-code-to-dependency", OopBlitzUmlEngine::umlCodeToDependency),
                entry("uml-multi-relation-pair", OopBlitzUmlEngine::umlMultiRelationPair));
    }

    private static EngineEntry entry(String key, Function<RandomGenerator, StoredQuestion> builder) {
        return new EngineEntry(key, OopBlitzQuestionBank.KIND_UML, builder);
    }

    // ==================== EASY (4) ====================

    private static StoredQuestion umlCodeToExtends(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.EXTENDS_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-extends", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                "Which Mermaid UML relation matches this code?",
                List.of(
                        "public class " + t.left() + " { }",
                        "public class " + t.right() + " extends " + t.left() + " { }"),
                t.left() + " <|-- " + t.right(),
                List.of(
                        t.right() + " <|-- " + t.left(),
                        t.left() + " <|.. " + t.right(),
                        t.left() + " *-- " + t.right()),
                "extends is Inheritance (solid line, hollow triangle pointing to superclass): " + t.left() + " <|-- " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlCodeToImplements(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.IMPLEMENTS_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-implements", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                "Which Mermaid UML relation matches this code?",
                List.of(
                        "public interface " + t.left() + " { void " + t.member() + "(); }",
                        "public class " + t.right() + " implements " + t.left() + " {",
                        "    public void " + t.member() + "() { }",
                        "}"),
                t.left() + " <|.. " + t.right(),
                List.of(
                        t.right() + " <|.. " + t.left(),
                        t.left() + " <|-- " + t.right(),
                        t.left() + " --> " + t.right()),
                "implements is Realization (dashed line, hollow triangle pointing to interface): " + t.left() + " <|.. " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlCodeToAssociation(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.ASSOCIATION_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-association", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                t.left() + " stores an independent " + t.right() + " link. Pick the UML relation:",
                List.of(
                        "public class " + t.left() + " {",
                        "    private " + t.right() + " " + t.fieldName() + ";",
                        "    public void " + t.member() + "(" + t.right() + " target) { this." + t.fieldName() + " = target; }",
                        "}"),
                t.left() + " --> " + t.right(),
                List.of(
                        t.left() + " <|-- " + t.right(),
                        t.left() + " <|.. " + t.right(),
                        t.left() + " *-- " + t.right()),
                "Storing a link to an independent peer object is Association: " + t.left() + " --> " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlArrowToMeaning(RandomGenerator rng) {
        int kind = rng.nextInt(4);
        if (kind == 0) {
            UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.EXTENDS_TUPLES, rng);
            return OopBlitzDomains.choiceQuestion(
                    "uml-arrow-to-meaning", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                    "Which Java header matches this Mermaid line?",
                    List.of("// Mermaid MMD", t.left() + " <|-- " + t.right()),
                    "class " + t.right() + " extends " + t.left(),
                    List.of(
                            "class " + t.left() + " extends " + t.right(),
                            "class " + t.right() + " implements " + t.left(),
                            "class " + t.left() + " implements " + t.right()),
                    "In " + t.left() + " <|-- " + t.right() + ", the hollow triangle points to superclass " + t.left() + ".",
                    rng);
        }
        if (kind == 1) {
            UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.IMPLEMENTS_TUPLES, rng);
            return OopBlitzDomains.choiceQuestion(
                    "uml-arrow-to-meaning", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                    "Which Java header matches this Mermaid line?",
                    List.of("// Mermaid MMD", t.left() + " <|.. " + t.right()),
                    "class " + t.right() + " implements " + t.left(),
                    List.of(
                            "class " + t.left() + " implements " + t.right(),
                            "class " + t.right() + " extends " + t.left(),
                            "interface " + t.left() + " extends " + t.right()),
                    "<|.. is Realization pointing to interface " + t.left() + ", so " + t.right() + " implements " + t.left() + ".",
                    rng);
        }
        if (kind == 2) {
            UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.COMPOSITION_TUPLES, rng);
            return OopBlitzDomains.choiceQuestion(
                    "uml-arrow-to-meaning", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                    "What relationship does this Mermaid line represent?",
                    List.of("// Mermaid MMD", t.left() + " *-- " + t.right()),
                    "Composition (strong ownership)",
                    List.of(
                            "Aggregation (shared reference)",
                            "Inheritance (extends)",
                            "Realization (implements)"),
                    "Filled diamond (*--) denotes Composition: " + t.right() + " is owned exclusively by " + t.left() + ".",
                    rng);
        }
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.AGGREGATION_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-arrow-to-meaning", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What relationship does this Mermaid line represent?",
                List.of("// Mermaid MMD", t.left() + " o-- " + t.right()),
                "Aggregation (weak whole-part)",
                List.of(
                        "Composition (exclusive lifetime)",
                        "Realization (implements)",
                        "Dependency (temporary param)"),
                "Hollow diamond (o--) denotes Aggregation: " + t.right() + " can outlive " + t.left() + ".",
                rng);
    }

    // ==================== MEDIUM (3) ====================

    private static StoredQuestion umlCodeToComposition(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.COMPOSITION_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-composition", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                t.right() + " cannot exist without " + t.left() + ". Pick the UML relation:",
                List.of(
                        "public class " + t.left() + " {",
                        "    private final " + t.right() + " " + t.fieldName() + " = new " + t.right() + "();",
                        "}"),
                t.left() + " *-- " + t.right(),
                List.of(
                        t.left() + " o-- " + t.right(),
                        t.right() + " *-- " + t.left(),
                        t.left() + " ..> " + t.right()),
                "Exclusive lifetime ownership is Composition (filled diamond on whole): " + t.left() + " *-- " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlCodeToAggregation(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.AGGREGATION_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-aggregation", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                t.right() + " instances are created outside and outlive " + t.left() + ". Pick UML:",
                List.of(
                        "public class " + t.left() + " {",
                        "    private List<" + t.right() + "> " + t.fieldName() + ";",
                        "    public " + t.left() + "(List<" + t.right() + "> items) { this." + t.fieldName() + " = items; }",
                        "}"),
                t.left() + " o-- " + t.right(),
                List.of(
                        t.left() + " *-- " + t.right(),
                        t.right() + " o-- " + t.left(),
                        t.left() + " <|.. " + t.right()),
                "Shared/independent whole-part collection is Aggregation (hollow diamond): " + t.left() + " o-- " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlToCodeLifecycle(RandomGenerator rng) {
        boolean isComposition = rng.nextBoolean();
        if (isComposition) {
            UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.COMPOSITION_TUPLES, rng);
            return OopBlitzDomains.choiceQuestion(
                    "uml-to-code-lifecycle", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                    "Which Java design inside " + t.left() + " best matches this Mermaid UML?",
                    List.of("// Mermaid MMD", t.left() + " *-- " + t.right()),
                    "private final " + t.right() + " " + t.fieldName() + " = new " + t.right() + "();",
                    List.of(
                            "public void use(" + t.right() + " x) { }",
                            "class " + t.right() + " extends " + t.left() + " { }",
                            "public " + t.left() + "(List<" + t.right() + "> external) { }"),
                    "Composition (*--) creates and owns the " + t.right() + " inside " + t.left() + " with a matching lifecycle.",
                    rng);
        }
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.AGGREGATION_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-to-code-lifecycle", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "Which Java design inside " + t.left() + " best matches this Mermaid UML?",
                List.of("// Mermaid MMD", t.left() + " o-- " + t.right()),
                "Inject existing List<" + t.right() + "> via constructor",
                List.of(
                        "Create private final " + t.right() + " = new " + t.right() + "()",
                        "Declare class " + t.right() + " extends " + t.left(),
                        "Pass " + t.right() + " only as local method param without field"),
                "Aggregation (o--) stores injected parts whose lifecycle is independent of " + t.left() + ".",
                rng);
    }

    // ==================== HARD (2) ====================

    private static StoredQuestion umlCodeToDependency(RandomGenerator rng) {
        UmlTuple t = OopBlitzDomains.pickUml(OopBlitzDomains.DEPENDENCY_TUPLES, rng);
        return OopBlitzDomains.choiceQuestion(
                "uml-code-to-dependency", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_HARD, 20,
                t.left() + " uses " + t.right() + " only as a parameter (no field). Pick UML:",
                List.of(
                        "public class " + t.left() + " {",
                        "    public void " + t.member() + "(" + t.right() + " " + t.fieldName() + ") {",
                        "        " + t.fieldName() + ".run();",
                        "    }",
                        "}"),
                t.left() + " ..> " + t.right(),
                List.of(
                        t.left() + " --> " + t.right(),
                        t.left() + " *-- " + t.right(),
                        t.left() + " o-- " + t.right()),
                "Transient usage via method parameter/local without holding a field is Dependency: " + t.left() + " ..> " + t.right() + ".",
                rng);
    }

    private static StoredQuestion umlMultiRelationPair(RandomGenerator rng) {
        UmlTuple ext = OopBlitzDomains.pickUml(OopBlitzDomains.EXTENDS_TUPLES, rng);
        UmlTuple imp = OopBlitzDomains.pickUml(OopBlitzDomains.IMPLEMENTS_TUPLES, rng);
        String targetClass = ext.right();
        return OopBlitzDomains.choiceQuestion(
                "uml-multi-relation-pair", OopBlitzQuestionBank.KIND_UML, OopBlitzQuestionBank.DIFF_HARD, 20,
                "Pick the exact Mermaid pair describing " + targetClass + ":",
                List.of(
                        "public class " + targetClass + " extends " + ext.left() + " implements " + imp.left() + " {",
                        "    public void " + imp.member() + "() { }",
                        "}"),
                ext.left() + " <|-- " + targetClass + " & " + imp.left() + " <|.. " + targetClass,
                List.of(
                        targetClass + " <|-- " + ext.left() + " & " + targetClass + " <|.. " + imp.left(),
                        ext.left() + " <|.. " + targetClass + " & " + imp.left() + " <|-- " + targetClass,
                        ext.left() + " *-- " + targetClass + " & " + imp.left() + " ..> " + targetClass),
                "Both arrows point to the parent/interface: " + ext.left() + " <|-- " + targetClass + " and " + imp.left() + " <|.. " + targetClass + ".",
                rng);
    }
}
