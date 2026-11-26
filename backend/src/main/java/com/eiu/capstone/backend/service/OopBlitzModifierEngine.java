package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.eiu.capstone.backend.service.OopBlitzDomains.ClassDomain;
import com.eiu.capstone.backend.service.OopBlitzDomains.HierarchyDomain;
import com.eiu.capstone.backend.service.OopBlitzDomains.PackagePair;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.EngineEntry;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

/**
 * JLS Access & Modifier truth-table question engine.
 */
final class OopBlitzModifierEngine {

    private record VisibilityLevel(String keyword, String label, int rank) {
    }

    private static final List<VisibilityLevel> VISIBILITIES = List.of(
            new VisibilityLevel("private", "private", 0),
            new VisibilityLevel("", "package-private", 1),
            new VisibilityLevel("protected", "protected", 2),
            new VisibilityLevel("public", "public", 3));

    private OopBlitzModifierEngine() {
    }

    static List<EngineEntry> easyEntries() {
        return List.of(
                entry("mod-same-pkg-access", OopBlitzModifierEngine::modSamePkgAccess),
                entry("mod-abstract-concrete", OopBlitzModifierEngine::modAbstractConcrete));
    }

    static List<EngineEntry> mediumEntries() {
        return List.of(
                entry("mod-diff-pkg-non-sub", OopBlitzModifierEngine::modDiffPkgNonSub),
                entry("mod-override-visibility-matrix", OopBlitzModifierEngine::modOverrideVisibilityMatrix),
                entry("mod-final-modifier-matrix", OopBlitzModifierEngine::modFinalModifierMatrix));
    }

    static List<EngineEntry> hardEntries() {
        return List.of(
                entry("mod-diff-pkg-protected-ref", OopBlitzModifierEngine::modDiffPkgProtectedRef),
                entry("mod-interface-member-rules", OopBlitzModifierEngine::modInterfaceMemberRules));
    }

    private static EngineEntry entry(String key, Function<RandomGenerator, StoredQuestion> builder) {
        return new EngineEntry(key, OopBlitzQuestionBank.KIND_MODIFIER, builder);
    }

    // ==================== EASY (2) ====================

    private static StoredQuestion modSamePkgAccess(RandomGenerator rng) {
        PackagePair p = OopBlitzDomains.pickPackagePair(rng);
        int idx = rng.nextInt(4); // 0=private, 1=package-private, 2=protected, 3=public
        VisibilityLevel vis = VISIBILITIES.get(idx);
        boolean compiles = idx >= 1;
        String modPrefix = vis.keyword().isEmpty() ? "" : vis.keyword() + " ";
        String successLabel = "Compiles and returns " + p.intValue();
        String errLabel = "Compile error: " + p.member() + " has " + vis.label() + " access";
        return OopBlitzDomains.choiceQuestion(
                "mod-same-pkg-access", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_EASY, 15,
                p.parent() + " and " + p.outsider() + " are in the same package. Does " + p.outsider() + " compile?",
                List.of(
                        "package " + p.pkgA() + ";",
                        "class " + p.parent() + " { " + modPrefix + "int " + p.member() + " = " + p.intValue() + "; }",
                        "class " + p.outsider() + " {",
                        "    int read(" + p.parent() + " ref) { return ref." + p.member() + "; }",
                        "}"),
                compiles ? successLabel : errLabel,
                List.of(
                        compiles ? "Compile error: " + p.member() + " is hidden" : successLabel,
                        "Returns 0",
                        "Runtime IllegalAccessException"),
                compiles
                        ? vis.label() + " members are accessible to any class in the same package (" + p.pkgA() + ")."
                        : "private members are accessible only inside " + p.parent() + ", even within the same package.",
                rng);
    }

    private static StoredQuestion modAbstractConcrete(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        boolean isAbstractClass = rng.nextBoolean();
        String header = isAbstractClass
                ? "public abstract class " + d.cls() + " {"
                : "public class " + d.cls() + " {";
        String correct = isAbstractClass
                ? "Compiles cleanly (cannot instantiate directly)"
                : "Compile error: " + d.cls() + " must be declared abstract";
        return OopBlitzDomains.choiceQuestion(
                "mod-abstract-concrete", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What happens when compiling " + d.cls() + "?",
                List.of(
                        header,
                        "    public abstract void " + d.method() + "();",
                        "}"),
                correct,
                List.of(
                        isAbstractClass ? "Compile error: missing method body" : "Compiles with empty " + d.method() + "()",
                        "Throws AbstractMethodError at compile time",
                        "Compiles as an interface"),
                isAbstractClass
                        ? "An abstract class may contain abstract methods without bodies."
                        : "A concrete (non-abstract) class cannot declare an abstract method.",
                rng);
    }

    // ==================== MEDIUM (3) ====================

    private static StoredQuestion modDiffPkgNonSub(RandomGenerator rng) {
        PackagePair p = OopBlitzDomains.pickPackagePair(rng);
        int idx = rng.nextInt(4); // 0=private, 1=package-private, 2=protected, 3=public
        VisibilityLevel vis = VISIBILITIES.get(idx);
        boolean compiles = idx == 3;
        String modPrefix = vis.keyword().isEmpty() ? "" : vis.keyword() + " ";
        String successLabel = "Compiles and returns " + p.intValue();
        String errLabel = "Compile error: " + p.member() + " is not accessible from " + p.pkgB();
        return OopBlitzDomains.choiceQuestion(
                "mod-diff-pkg-non-sub", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                p.outsider() + " is in package " + p.pkgB() + " (not a subclass). Does it compile?",
                List.of(
                        "// package " + p.pkgA() + ": public class " + p.parent() + " { " + modPrefix + "int " + p.member() + " = " + p.intValue() + "; }",
                        "package " + p.pkgB() + ";",
                        "public class " + p.outsider() + " {",
                        "    int read(" + p.pkgA() + "." + p.parent() + " ref) { return ref." + p.member() + "; }",
                        "}"),
                compiles ? successLabel : errLabel,
                List.of(
                        compiles ? "Compile error: cross-package field denied" : successLabel,
                        "Compiles only if " + p.member() + " is static",
                        "Runtime IllegalAccessError"),
                compiles
                        ? "public members are accessible from any package."
                        : vis.label() + " members cannot be accessed by a non-subclass in another package (" + p.pkgB() + ").",
                rng);
    }

    private static StoredQuestion modOverrideVisibilityMatrix(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        // Parent visibility among package-private (1), protected (2), public (3)
        int parentRank = 1 + rng.nextInt(3);
        // Child visibility among package-private (1), protected (2), public (3)
        int childRank = 1 + rng.nextInt(3);
        VisibilityLevel parentVis = VISIBILITIES.get(parentRank);
        VisibilityLevel childVis = VISIBILITIES.get(childRank);
        boolean legal = childRank >= parentRank;

        String pMod = parentVis.keyword().isEmpty() ? "" : parentVis.keyword() + " ";
        String cMod = childVis.keyword().isEmpty() ? "" : childVis.keyword() + " ";
        String validLabel = "Compiles: valid override (" + childVis.label() + " >= " + parentVis.label() + ")";
        String invalidLabel = "Compile error: cannot reduce visibility (" + parentVis.label() + " -> " + childVis.label() + ")";
        return OopBlitzDomains.choiceQuestion(
                "mod-override-visibility-matrix", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "Both classes are in the same package. Does " + h.child() + " compile?",
                List.of(
                        "class " + h.parent() + " { " + pMod + "void " + h.method() + "() { } }",
                        "class " + h.child() + " extends " + h.parent() + " {",
                        "    @Override",
                        "    " + cMod + "void " + h.method() + "() { }",
                        "}"),
                legal ? validLabel : invalidLabel,
                List.of(
                        legal ? "Compile error: visibility must match exactly" : validLabel,
                        "Compiles with runtime warning",
                        "Throws IllegalAccessError"),
                legal
                        ? "An overriding method may keep or widen visibility (" + childVis.label() + " is at least as accessible as " + parentVis.label() + ")."
                        : "An overriding method cannot assign weaker access privileges than the inherited " + parentVis.label() + " method.",
                rng);
    }

    private static StoredQuestion modFinalModifierMatrix(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        boolean finalClass = rng.nextBoolean();
        if (finalClass) {
            return OopBlitzDomains.choiceQuestion(
                    "mod-final-modifier-matrix", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                    "What happens when compiling " + h.child() + "?",
                    List.of(
                            "public final class " + h.parent() + " { }",
                            "public class " + h.child() + " extends " + h.parent() + " { }"),
                    "Compile error: cannot inherit from final " + h.parent(),
                    List.of(
                            "Compiles and inherits all methods",
                            "Compiles unless " + h.parent() + " has fields",
                            "Runtime ClassCastException"),
                    "A class marked final cannot be extended.",
                    rng);
        }
        return OopBlitzDomains.choiceQuestion(
                "mod-final-modifier-matrix", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "Can " + h.child() + " call final method " + h.method() + "() inherited from " + h.parent() + "?",
                List.of(
                        "class " + h.parent() + " { public final String " + h.method() + "() { return \"" + h.parentOut() + "\"; } }",
                        "class " + h.child() + " extends " + h.parent() + " {",
                        "    String run() { return " + h.method() + "(); }",
                        "}"),
                "Compiles: final methods can be called, just not overridden",
                List.of(
                        "Compile error: final method is private to " + h.parent(),
                        "Compile error: " + h.child() + " must redeclare final",
                        "Runtime UnsupportedOperationException"),
                "final prevents overriding in subclasses, but the method is still inherited and callable.",
                rng);
    }

    // ==================== HARD (2) ====================

    private static StoredQuestion modDiffPkgProtectedRef(RandomGenerator rng) {
        PackagePair p = OopBlitzDomains.pickPackagePair(rng);
        boolean viaThis = rng.nextBoolean();
        List<String> lines = viaThis
                ? List.of(
                        "// package " + p.pkgA() + ": public class " + p.parent() + " { protected int " + p.member() + " = " + p.intValue() + "; }",
                        "package " + p.pkgB() + ";",
                        "public class " + p.child() + " extends " + p.pkgA() + "." + p.parent() + " {",
                        "    int read(" + p.child() + " sub) { return sub." + p.member() + "; }",
                        "}")
                : List.of(
                        "// package " + p.pkgA() + ": public class " + p.parent() + " { protected int " + p.member() + " = " + p.intValue() + "; }",
                        "package " + p.pkgB() + ";",
                        "public class " + p.child() + " extends " + p.pkgA() + "." + p.parent() + " {",
                        "    int read(" + p.pkgA() + "." + p.parent() + " ref) { return ref." + p.member() + "; }",
                        "}");
        String ok = "Compiles and returns " + p.intValue();
        String err = "Compile error: access via " + p.child() + "/this only, not " + p.parent() + " ref";
        return OopBlitzDomains.choiceQuestion(
                "mod-diff-pkg-protected-ref", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_HARD, 20,
                p.child() + " is in package " + p.pkgB() + ", " + p.parent() + " is in " + p.pkgA() + ". Does read(...) compile?",
                lines,
                viaThis ? ok : err,
                List.of(
                        viaThis ? err : ok,
                        "Compiles only if " + p.member() + " is final",
                        "Runtime SecurityException"),
                viaThis
                        ? "Inside a cross-package subclass, protected members are accessible on a " + p.child() + " (or this) reference."
                        : "Cross-package protected members cannot be accessed via a superclass (" + p.parent() + ") reference.",
                rng);
    }

    private static StoredQuestion modInterfaceMemberRules(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int val = rng.nextInt(1, 50);
        boolean testField = rng.nextBoolean();
        if (testField) {
            return OopBlitzDomains.choiceQuestion(
                    "mod-interface-member-rules", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_HARD, 20,
                    "What are the implicit modifiers on field VAL inside an interface?",
                    List.of(
                            "public interface " + d.cls() + "Config {",
                            "    int VAL = " + val + ";",
                            "}"),
                    "public static final",
                    List.of(
                            "package-private instance field",
                            "protected static final",
                            "private final"),
                    "Every field declared inside a Java interface is implicitly public static final.",
                    rng);
        }
        return OopBlitzDomains.choiceQuestion(
                "mod-interface-member-rules", OopBlitzQuestionBank.KIND_MODIFIER, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What happens when " + d.cls() + "Impl implements " + d.method() + "() without public?",
                List.of(
                        "interface " + d.cls() + "Api { void " + d.method() + "(); }",
                        "class " + d.cls() + "Impl implements " + d.cls() + "Api {",
                        "    void " + d.method() + "() { }",
                        "}"),
                "Compile error: attempting to assign weaker access (must be public)",
                List.of(
                        "Compiles as package-private implementation",
                        "Compiles if both are in the same file",
                        "Runtime IllegalAccessError"),
                "Interface methods are implicitly public; implementing without public reduces visibility to package-private.",
                rng);
    }
}
