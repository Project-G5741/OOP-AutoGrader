package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.eiu.capstone.backend.service.OopBlitzDomains.ClassDomain;
import com.eiu.capstone.backend.service.OopBlitzDomains.HierarchyDomain;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.EngineEntry;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

/**
 * Procedural Bug Hunter generator: combines 24 classic Java OOP compiler traps
 * with domain substitution and valid-code noise injection so bug line numbers vary.
 */
final class OopBlitzBugEngine {

    private OopBlitzBugEngine() {
    }

    static List<EngineEntry> easyEntries() {
        return List.of(
                entry("bug-static-this", OopBlitzBugEngine::bugStaticThis),
                entry("bug-abstract-new", OopBlitzBugEngine::bugAbstractNew),
                entry("bug-final-reassign", OopBlitzBugEngine::bugFinalReassign),
                entry("bug-interface-body", OopBlitzBugEngine::bugInterfaceBody),
                entry("bug-ctor-void-return", OopBlitzBugEngine::bugCtorVoidReturn),
                entry("bug-abstract-in-concrete", OopBlitzBugEngine::bugAbstractInConcrete),
                entry("bug-multiple-extends", OopBlitzBugEngine::bugMultipleExtends),
                entry("bug-void-return-value", OopBlitzBugEngine::bugVoidReturnValue));
    }

    static List<EngineEntry> mediumEntries() {
        return List.of(
                entry("bug-this-ctor-order", OopBlitzBugEngine::bugThisCtorOrder),
                entry("bug-override-visibility", OopBlitzBugEngine::bugOverrideVisibility),
                entry("bug-this-and-super", OopBlitzBugEngine::bugThisAndSuper),
                entry("bug-missing-return", OopBlitzBugEngine::bugMissingReturn),
                entry("bug-extend-final-class", OopBlitzBugEngine::bugExtendFinalClass),
                entry("bug-static-calls-instance", OopBlitzBugEngine::bugStaticCallsInstance),
                entry("bug-duplicate-overload", OopBlitzBugEngine::bugDuplicateOverload),
                entry("bug-private-super-field", OopBlitzBugEngine::bugPrivateSuperField));
    }

    static List<EngineEntry> hardEntries() {
        return List.of(
                entry("bug-missing-super", OopBlitzBugEngine::bugMissingSuper),
                entry("bug-final-method-override", OopBlitzBugEngine::bugFinalMethodOverride),
                entry("bug-interface-impl-visibility", OopBlitzBugEngine::bugInterfaceImplVisibility),
                entry("bug-override-static", OopBlitzBugEngine::bugOverrideStatic),
                entry("bug-unchecked-downcast", OopBlitzBugEngine::bugUncheckedDowncast),
                entry("bug-final-param-reassign", OopBlitzBugEngine::bugFinalParamReassign),
                entry("bug-abstract-final-class", OopBlitzBugEngine::bugAbstractFinalClass),
                entry("bug-incompatible-override-return", OopBlitzBugEngine::bugIncompatibleOverrideReturn));
    }

    private static EngineEntry entry(String key, Function<RandomGenerator, StoredQuestion> builder) {
        return new EngineEntry(key, OopBlitzQuestionBank.KIND_BUG, builder);
    }

    // ==================== EASY (8) ====================

    private static StoredQuestion bugStaticThis(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int initVal = rng.nextInt(1, 10);
        return buildShiftedBug(
                "bug-static-this", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of(
                        "    private int " + d.intField() + " = " + initVal + ";",
                        "    public static void reset() {"),
                "        this." + d.intField() + " = 0;",
                List.of("    }", "}"),
                "Cannot reference 'this' or instance field '" + d.intField() + "' inside a static method.",
                rng);
    }

    private static StoredQuestion bugAbstractNew(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        List<String> top = List.of(
                "public abstract class " + h.parent() + " {",
                "    public abstract String " + h.method() + "();",
                "}",
                "public class Main {");
        return buildShiftedBug(
                "bug-abstract-new", OopBlitzQuestionBank.DIFF_EASY, 15,
                top,
                mainNoisePool(d, rng),
                List.of(),
                "    " + h.parent() + " item = new " + h.parent() + "();",
                List.of("}"),
                "Abstract class " + h.parent() + " cannot be instantiated directly with new.",
                rng);
    }

    private static StoredQuestion bugFinalReassign(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int a = rng.nextInt(2, 9);
        int b = a + rng.nextInt(2, 8);
        return buildShiftedBug(
                "bug-final-reassign", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of(
                        "    public final int " + d.intField() + " = " + a + ";",
                        "    public void update() {"),
                "        " + d.intField() + " = " + b + ";",
                List.of("    }", "}"),
                "Cannot reassign final field '" + d.intField() + "' after initialization.",
                rng);
    }

    private static StoredQuestion bugInterfaceBody(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int cap = rng.nextInt(10, 99);
        return buildShiftedBug(
                "bug-interface-body", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public interface " + d.cls() + "Rule {"),
                List.of(
                        "    int MAX_CAP = " + cap + ";",
                        "    String tag();"),
                List.of(),
                "    void " + d.method() + "() { System.out.println(\"" + d.val1() + "\"); }",
                List.of("}"),
                "Interface methods without default or static cannot have a method body.",
                rng);
    }

    private static StoredQuestion bugCtorVoidReturn(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-ctor-void-return", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public class " + d.cls() + " {"),
                List.of("    private int " + d.intField() + " = 1;", "    private static final int VER = 1;"),
                List.of(
                        "    private String " + d.strField() + ";",
                        "    public void " + d.cls() + "(String v) { this." + d.strField() + " = v; }",
                        "    public " + d.cls() + "() {"),
                "        this(\"" + d.val1() + "\");",
                List.of("    }", "}"),
                "'void " + d.cls() + "(String)' is a method, not a constructor, so this(...) fails.",
                rng);
    }

    private static StoredQuestion bugAbstractInConcrete(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-abstract-in-concrete", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of(),
                "    public abstract void " + d.method() + "();",
                List.of("}"),
                "Concrete class " + d.cls() + " cannot declare an abstract method without being abstract.",
                rng);
    }

    private static StoredQuestion bugMultipleExtends(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-multiple-extends", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of(
                        "class " + h.parent() + " { }",
                        "class " + d.cls() + "Base { }"),
                List.of(),
                List.of(),
                "class " + h.child() + " extends " + h.parent() + ", " + d.cls() + "Base {",
                List.of("    private int flag = 1;", "}"),
                "Java classes support single inheritance only and cannot extend multiple classes.",
                rng);
    }

    private static StoredQuestion bugVoidReturnValue(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int v = rng.nextInt(10, 50);
        return buildShiftedBug(
                "bug-void-return-value", OopBlitzQuestionBank.DIFF_EASY, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of("    public void " + d.method() + "() {"),
                "        return " + v + ";",
                List.of("    }", "}"),
                "A void method cannot return a value (" + v + ").",
                rng);
    }

    // ==================== MEDIUM (8) ====================

    private static StoredQuestion bugThisCtorOrder(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-this-ctor-order", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of("public class " + d.cls() + " {"),
                List.of("    private int " + d.intField() + " = 1;", "    private static final int VER = 1;"),
                List.of(
                        "    private String " + d.strField() + ";",
                        "    public " + d.cls() + "(String v) { this." + d.strField() + " = v; }",
                        "    public " + d.cls() + "() {",
                        "        System.out.println(\"init\");"),
                "        this(\"" + d.val1() + "\");",
                List.of("    }", "}"),
                "Call to this(...) must be the very first statement in a constructor.",
                rng);
    }

    private static StoredQuestion bugOverrideVisibility(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-override-visibility", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of(
                        "class " + h.parent() + " {",
                        "    public String " + h.method() + "() { return \"" + h.parentOut() + "\"; }",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                classNoisePool(d),
                List.of("    @Override"),
                "    protected String " + h.method() + "() { return \"" + h.childOut() + "\"; }",
                List.of("}"),
                "Overriding method cannot reduce visibility from public to protected.",
                rng);
    }

    private static StoredQuestion bugThisAndSuper(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return buildShiftedBug(
                "bug-this-and-super", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of(
                        "class " + h.parent() + " { " + h.parent() + "(String s) { } }",
                        "class " + h.child() + " extends " + h.parent() + " {",
                        "    " + h.child() + "(String s) { super(s); }",
                        "    " + h.child() + "() {",
                        "        this(\"" + h.childTag() + "\");"),
                List.of(),
                List.of(),
                "        super(\"" + h.parentTag() + "\");",
                List.of("    }", "}"),
                "A constructor cannot invoke both this(...) and super(...).",
                rng);
    }

    private static StoredQuestion bugMissingReturn(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int threshold = rng.nextInt(1, 20);
        return buildShiftedBug(
                "bug-missing-return", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of(
                        "    public int check(int v) {",
                        "        if (v > " + threshold + ") return v;"),
                "    }",
                List.of("}"),
                "Method must return an int on every path (missing return when v <= " + threshold + ").",
                rng);
    }

    private static StoredQuestion bugExtendFinalClass(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-extend-final-class", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of(
                        "public final class " + h.parent() + " {",
                        "    private int code = " + rng.nextInt(1, 9) + ";",
                        "}"),
                List.of("// Subclass definition below", "interface HelperTag { }"),
                List.of(),
                "public class " + h.child() + " extends " + h.parent() + " {",
                List.of("    private String note = \"" + d.val1() + "\";", "}"),
                "Final class " + h.parent() + " cannot be subclassed.",
                rng);
    }

    private static StoredQuestion bugStaticCallsInstance(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-static-calls-instance", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of(
                        "    public void " + d.method() + "() { }",
                        "    public static void runAll() {"),
                "        " + d.method() + "();",
                List.of("    }", "}"),
                "Non-static method " + d.method() + "() cannot be called from a static context without an instance.",
                rng);
    }

    private static StoredQuestion bugDuplicateOverload(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-duplicate-overload", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of("    public int compute(int x) { return x + 1; }"),
                "    public String compute(int x) { return String.valueOf(x); }",
                List.of("}"),
                "Overloading cannot differ by return type alone when parameter types are identical.",
                rng);
    }

    private static StoredQuestion bugPrivateSuperField(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int secret = rng.nextInt(10, 99);
        return buildShiftedBug(
                "bug-private-super-field", OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                List.of(
                        "class " + h.parent() + " {",
                        "    private int secret = " + secret + ";",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                List.of("    private String label = \"" + h.childTag() + "\";"),
                List.of("    int peek() {"),
                "        return secret;",
                List.of("    }", "}"),
                "Subclass " + h.child() + " cannot directly access private field 'secret' of " + h.parent() + ".",
                rng);
    }

    // ==================== HARD (8) ====================

    private static StoredQuestion bugMissingSuper(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return buildShiftedBug(
                "bug-missing-super", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of(
                        "class " + h.parent() + " {",
                        "    private final String id;",
                        "    " + h.parent() + "(String id) { this.id = id; }",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                List.of("    private int rank = 1;"),
                List.of(),
                "    " + h.child() + "() { }",
                List.of("}"),
                h.parent() + " has no no-arg constructor; " + h.child() + "() must call super(id).",
                rng);
    }

    private static StoredQuestion bugFinalMethodOverride(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return buildShiftedBug(
                "bug-final-method-override", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of(
                        "class " + h.parent() + " {",
                        "    public final String " + h.method() + "() { return \"" + h.parentOut() + "\"; }",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                List.of("    private boolean ready = true;"),
                List.of("    @Override"),
                "    public String " + h.method() + "() { return \"" + h.childOut() + "\"; }",
                List.of("}"),
                "Method " + h.method() + "() is marked final in " + h.parent() + " and cannot be overridden.",
                rng);
    }

    private static StoredQuestion bugInterfaceImplVisibility(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int val = rng.nextInt(10, 50);
        return buildShiftedBug(
                "bug-interface-impl-visibility", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of(
                        "interface " + d.cls() + "Spec {",
                        "    int " + d.method() + "();",
                        "}",
                        "class " + d.cls() + "Impl implements " + d.cls() + "Spec {"),
                classNoisePool(d),
                List.of(),
                "    int " + d.method() + "() { return " + val + "; }",
                List.of("}"),
                "Interface methods are implicitly public; " + d.cls() + "Impl." + d.method() + "() must be public.",
                rng);
    }

    private static StoredQuestion bugOverrideStatic(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return buildShiftedBug(
                "bug-override-static", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of(
                        "class " + h.parent() + " {",
                        "    public static String tag() { return \"" + h.parentTag() + "\"; }",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                List.of("    private int level = 2;"),
                List.of("    @Override"),
                "    public static String tag() { return \"" + h.childTag() + "\"; }",
                List.of("}"),
                "Static methods are hidden, not overridden, so @Override causes a compile error.",
                rng);
    }

    private static StoredQuestion bugUncheckedDowncast(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return buildShiftedBug(
                "bug-unchecked-downcast", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of("public class Main {"),
                List.of("    static int count = 0;", "    String title = \"" + h.parentTag() + "\";"),
                List.of(
                        "    public static void main(String[] args) {",
                        "        " + h.parent() + " ref = new " + h.child() + "();"),
                "        " + h.child() + " sub = ref;",
                List.of("    }", "}"),
                "Downcasting from " + h.parent() + " to " + h.child() + " requires an explicit cast (" + h.child() + ") ref.",
                rng);
    }

    private static StoredQuestion bugFinalParamReassign(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int delta = rng.nextInt(1, 9);
        return buildShiftedBug(
                "bug-final-param-reassign", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of("public class " + d.cls() + " {"),
                classNoisePool(d),
                List.of("    public int adjust(final int amount) {"),
                "        amount = amount + " + delta + ";",
                List.of("        return amount;", "    }", "}"),
                "Final parameter 'amount' cannot be reassigned inside the method.",
                rng);
    }

    private static StoredQuestion bugAbstractFinalClass(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        return buildShiftedBug(
                "bug-abstract-final-class", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of("// Base declaration"),
                List.of("interface " + d.cls() + "Tag { }"),
                List.of(),
                "public abstract final class " + d.cls() + " {",
                List.of("    private int " + d.intField() + " = 0;", "}"),
                "A class cannot be both abstract (must be extended) and final (cannot be extended).",
                rng);
    }

    private static StoredQuestion bugIncompatibleOverrideReturn(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int code = rng.nextInt(1, 9);
        return buildShiftedBug(
                "bug-incompatible-override-return", OopBlitzQuestionBank.DIFF_HARD, 20,
                List.of(
                        "class " + h.parent() + " {",
                        "    public int code() { return " + code + "; }",
                        "}",
                        "class " + h.child() + " extends " + h.parent() + " {"),
                List.of("    private String tag = \"" + h.childTag() + "\";"),
                List.of("    @Override"),
                "    public String code() { return \"" + h.childTag() + "\"; }",
                List.of("}"),
                "Return type String is incompatible with overridden int code() in " + h.parent() + ".",
                rng);
    }

    private static List<String> classNoisePool(ClassDomain d) {
        return List.of(
                "    private String " + d.strField() + " = \"" + d.val1() + "\";",
                "    public String get" + cap(d.strField()) + "() { return " + d.strField() + "; }",
                "    private static final int VER = 1;");
    }

    private static List<String> mainNoisePool(ClassDomain d, RandomGenerator rng) {
        return List.of(
                "    int seed = " + rng.nextInt(1, 20) + ";",
                "    String label = \"" + d.val1() + "\";");
    }

    private static StoredQuestion buildShiftedBug(
            String key,
            String difficulty,
            int timeLimitSec,
            List<String> prefixLines,
            List<String> noiseCandidates,
            List<String> beforeBugLines,
            String bugLine,
            List<String> afterBugLines,
            String reasonTail,
            RandomGenerator rng) {
        List<String> lines = new ArrayList<>();
        lines.addAll(prefixLines);

        if (!noiseCandidates.isEmpty()) {
            List<String> copy = new ArrayList<>(noiseCandidates);
            OopBlitzDomains.shuffle(copy, rng);
            int noiseCount = rng.nextInt(Math.min(3, copy.size() + 1));
            for (int i = 0; i < noiseCount; i++) {
                lines.add(copy.get(i));
            }
        }

        lines.addAll(beforeBugLines);
        int bugLineIndex = lines.size();
        lines.add(bugLine);
        lines.addAll(afterBugLines);

        return new StoredQuestion(
                key,
                OopBlitzQuestionBank.KIND_BUG,
                difficulty,
                timeLimitSec,
                "Click the line that causes a compile error:",
                List.copyOf(lines),
                List.of(),
                bugLineIndex,
                "Line " + (bugLineIndex + 1) + ": " + reasonTail);
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
