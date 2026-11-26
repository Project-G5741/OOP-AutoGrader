package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.eiu.capstone.backend.service.OopBlitzDomains.ClassDomain;
import com.eiu.capstone.backend.service.OopBlitzDomains.HierarchyDomain;
import com.eiu.capstone.backend.service.OopBlitzDomains.TripleHierarchy;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.EngineEntry;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;

/**
 * Procedural Output Trace engine: synthesizes randomized Java OOP snippets,
 * executes the exact Java semantics in memory, and produces 3 plausible distractors.
 */
final class OopBlitzOutputEngine {

    private OopBlitzOutputEngine() {
    }

    static List<EngineEntry> easyEntries() {
        return List.of(
                entry("out-poly-dispatch", OopBlitzOutputEngine::outPolyDispatch),
                entry("out-encapsulation-guard", OopBlitzOutputEngine::outEncapsulationGuard),
                entry("out-instance-independence", OopBlitzOutputEngine::outInstanceIndependence),
                entry("out-string-immutable", OopBlitzOutputEngine::outStringImmutable),
                entry("out-this-chaining", OopBlitzOutputEngine::outThisChaining),
                entry("out-instanceof-basic", OopBlitzOutputEngine::outInstanceofBasic));
    }

    static List<EngineEntry> mediumEntries() {
        return List.of(
                entry("out-static-counter", OopBlitzOutputEngine::outStaticCounter),
                entry("out-ctor-order-2level", OopBlitzOutputEngine::outCtorOrder2Level),
                entry("out-string-equals", OopBlitzOutputEngine::outStringEquals),
                entry("out-super-method-chain", OopBlitzOutputEngine::outSuperMethodChain),
                entry("out-poly-array-sum", OopBlitzOutputEngine::outPolyArraySum),
                entry("out-reference-aliasing", OopBlitzOutputEngine::outReferenceAliasing),
                entry("out-stringbuilder-mutate", OopBlitzOutputEngine::outStringBuilderMutate),
                entry("out-try-catch-finally", OopBlitzOutputEngine::outTryCatchFinally));
    }

    static List<EngineEntry> hardEntries() {
        return List.of(
                entry("out-pass-by-value-vs-reassign", OopBlitzOutputEngine::outPassByValueVsReassign),
                entry("out-static-method-hiding", OopBlitzOutputEngine::outStaticMethodHiding),
                entry("out-field-shadowing", OopBlitzOutputEngine::outFieldShadowing),
                entry("out-overload-compile-time", OopBlitzOutputEngine::outOverloadCompileTime),
                entry("out-ctor-3level-init", OopBlitzOutputEngine::outCtor3LevelInit),
                entry("out-interface-default-class-wins", OopBlitzOutputEngine::outInterfaceDefaultClassWins),
                entry("out-downcast-runtime", OopBlitzOutputEngine::outDowncastRuntime),
                entry("out-integer-cache", OopBlitzOutputEngine::outIntegerCache));
    }

    private static EngineEntry entry(String key, Function<RandomGenerator, StoredQuestion> builder) {
        return new EngineEntry(key, OopBlitzQuestionBank.KIND_OUTPUT, builder);
    }

    // ==================== EASY (6) ====================

    private static StoredQuestion outPolyDispatch(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        boolean pickChild = rng.nextBoolean();
        String runtimeType = pickChild ? h.child() : h.sibling();
        String correct = pickChild ? h.childOut() : h.siblingOut();
        String otherSub = pickChild ? h.siblingOut() : h.childOut();
        return OopBlitzDomains.choiceQuestion(
                "out-poly-dispatch", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does this snippet print?",
                List.of(
                        "class " + h.parent() + " { String " + h.method() + "() { return \"" + h.parentOut() + "\"; } }",
                        "class " + h.child() + " extends " + h.parent() + " { String " + h.method() + "() { return \"" + h.childOut() + "\"; } }",
                        "class " + h.sibling() + " extends " + h.parent() + " { String " + h.method() + "() { return \"" + h.siblingOut() + "\"; } }",
                        h.parent() + " ref = new " + runtimeType + "();",
                        "System.out.println(ref." + h.method() + "());"),
                correct,
                List.of(h.parentOut(), otherSub, "Compile error"),
                "Dynamic dispatch invokes " + runtimeType + "." + h.method() + "() at runtime (\"" + correct + "\").",
                rng);
    }

    private static StoredQuestion outEncapsulationGuard(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int init = rng.nextInt(3, 12);
        int pos = rng.nextInt(2, 8);
        int neg = rng.nextInt(1, 5);
        int ans = init + pos;
        return OopBlitzDomains.choiceQuestion(
                "out-encapsulation-guard", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does this code print?",
                List.of(
                        "class " + d.cls() + " {",
                        "    private int " + d.intField() + ";",
                        "    " + d.cls() + "(int v) { this." + d.intField() + " = v; }",
                        "    void add(int d) { if (d > 0) " + d.intField() + " += d; }",
                        "    int get() { return " + d.intField() + "; }",
                        "}",
                        d.cls() + " obj = new " + d.cls() + "(" + init + "); obj.add(" + pos + "); obj.add(-" + neg + ");",
                        "System.out.println(obj.get());"),
                String.valueOf(ans),
                List.of(String.valueOf(ans - neg), String.valueOf(init), String.valueOf(pos)),
                "add(" + pos + ") adds to " + init + " (= " + ans + "); add(-" + neg + ") is blocked by (d > 0).",
                rng);
    }

    private static StoredQuestion outInstanceIndependence(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int v1 = rng.nextInt(2, 7);
        int v2 = rng.nextInt(8, 15);
        int delta = rng.nextInt(2, 6);
        String ans = (v1 + delta) + "," + v2;
        return OopBlitzDomains.choiceQuestion(
                "out-instance-independence", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does this snippet print?",
                List.of(
                        "class " + d.cls() + " {",
                        "    int " + d.intField() + ";",
                        "    " + d.cls() + "(int v) { this." + d.intField() + " = v; }",
                        "}",
                        d.cls() + " a = new " + d.cls() + "(" + v1 + ");",
                        d.cls() + " b = new " + d.cls() + "(" + v2 + ");",
                        "a." + d.intField() + " += " + delta + ";",
                        "System.out.println(a." + d.intField() + " + \",\" + b." + d.intField() + ");"),
                ans,
                List.of(
                        (v1 + delta) + "," + (v2 + delta),
                        v1 + "," + v2,
                        (v2 + delta) + "," + (v2 + delta)),
                "Instance fields belong to each object separately, so mutating a does not change b (" + v2 + ").",
                rng);
    }

    private static StoredQuestion outStringImmutable(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        String s1 = d.val1();
        String s2 = d.val2();
        boolean assignBack = rng.nextBoolean();
        String opLine = assignBack
                ? "s = s.concat(\"" + s2 + "\");"
                : "s.concat(\"" + s2 + "\");";
        String correct = assignBack ? s1 + s2 : s1;
        String wrong = assignBack ? s1 : s1 + s2;
        return OopBlitzDomains.choiceQuestion(
                "out-string-immutable", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does this code print?",
                List.of(
                        "String s = \"" + s1 + "\";",
                        opLine,
                        "System.out.println(s);"),
                correct,
                List.of(wrong, s2, "Compile error"),
                assignBack
                        ? "Reassigning s stores the concatenated String \"" + correct + "\"."
                        : "String is immutable; calling s.concat(...) without assigning discards the new String.",
                rng);
    }

    private static StoredQuestion outThisChaining(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int v = rng.nextInt(1, 5);
        int a = rng.nextInt(2, 6);
        int b = rng.nextInt(3, 8);
        int ans = v + a + b;
        return OopBlitzDomains.choiceQuestion(
                "out-this-chaining", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does this fluent chain print?",
                List.of(
                        "class " + d.cls() + " {",
                        "    int " + d.intField() + ";",
                        "    " + d.cls() + "(int v) { " + d.intField() + " = v; }",
                        "    " + d.cls() + " add(int x) { " + d.intField() + " += x; return this; }",
                        "}",
                        "int res = new " + d.cls() + "(" + v + ").add(" + a + ").add(" + b + ")." + d.intField() + ";",
                        "System.out.println(res);"),
                String.valueOf(ans),
                List.of(String.valueOf(v + b), String.valueOf(v + a), String.valueOf(a + b)),
                "return this returns the same instance so both add(" + a + ") and add(" + b + ") accumulate to " + ans + ".",
                rng);
    }

    private static StoredQuestion outInstanceofBasic(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int variant = rng.nextInt(3);
        String target = variant == 0 ? h.child() : variant == 1 ? h.parent() : h.sibling();
        boolean expected = variant != 2;
        return OopBlitzDomains.choiceQuestion(
                "out-instanceof-basic", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_EASY, 15,
                "What does instanceof evaluate to?",
                List.of(
                        "class " + h.parent() + " { }",
                        "class " + h.child() + " extends " + h.parent() + " { }",
                        "class " + h.sibling() + " extends " + h.parent() + " { }",
                        h.parent() + " ref = new " + h.child() + "();",
                        "System.out.println(ref instanceof " + target + ");"),
                String.valueOf(expected),
                List.of(String.valueOf(!expected), "Compile error", "ClassCastException"),
                expected
                        ? "Runtime object " + h.child() + " IS-A " + target + ", so instanceof returns true."
                        : "Runtime object " + h.child() + " is a sibling of " + h.sibling() + ", so instanceof returns false.",
                rng);
    }

    // ==================== MEDIUM (8) ====================

    private static StoredQuestion outStaticCounter(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int start = rng.nextInt(1, 7);
        int step = rng.nextInt(2, 6);
        int instances = rng.nextBoolean() ? 2 : 3;
        int ans = start + step * instances;
        String creates = instances == 2
                ? "new " + d.cls() + "(); new " + d.cls() + "();"
                : "new " + d.cls() + "(); new " + d.cls() + "(); new " + d.cls() + "();";
        return OopBlitzDomains.choiceQuestion(
                "out-static-counter", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does this code print?",
                List.of(
                        "class " + d.cls() + " {",
                        "    static int total = " + start + ";",
                        "    " + d.cls() + "() { total += " + step + "; }",
                        "}",
                        creates,
                        "System.out.println(" + d.cls() + ".total);"),
                String.valueOf(ans),
                List.of(String.valueOf(start + step), String.valueOf(start), String.valueOf(ans + step)),
                "static field total is shared across all " + instances + " instances: " + start + " + " + instances + "*" + step + " = " + ans + ".",
                rng);
    }

    private static StoredQuestion outCtorOrder2Level(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        String p = h.parentTag();
        String c = h.childTag();
        return OopBlitzDomains.choiceQuestion(
                "out-ctor-order-2level", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What is printed when executing new " + h.child() + "()?",
                List.of(
                        "class " + h.parent() + " { " + h.parent() + "() { System.out.print(\"" + p + "\"); } }",
                        "class " + h.child() + " extends " + h.parent() + " {",
                        "    " + h.child() + "() { System.out.print(\"" + c + "\"); }",
                        "}",
                        "new " + h.child() + "();"),
                p + c,
                List.of(c + p, c, p),
                "Implicit super() runs " + h.parent() + "() first (\"" + p + "\"), then " + h.child() + "() (\"" + c + "\").",
                rng);
    }

    private static StoredQuestion outStringEquals(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        String w = d.val1();
        boolean useNew = rng.nextBoolean();
        List<String> lines = useNew
                ? List.of(
                        "String a = new String(\"" + w + "\");",
                        "String b = new String(\"" + w + "\");",
                        "System.out.println((a == b) + \" \" + a.equals(b));")
                : List.of(
                        "String a = \"" + w + "\";",
                        "String b = \"" + w + "\";",
                        "System.out.println((a == b) + \" \" + a.equals(b));");
        String correct = useNew ? "false true" : "true true";
        return OopBlitzDomains.choiceQuestion(
                "out-string-equals", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does this code print?",
                lines,
                correct,
                List.of(useNew ? "true true" : "false true", "false false", "true false"),
                useNew
                        ? "new String(...) creates two distinct heap objects (== is false), while .equals() compares text (true)."
                        : "String literals intern to the same String pool object (== is true and .equals() is true).",
                rng);
    }

    private static StoredQuestion outSuperMethodChain(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int mult = rng.nextBoolean() ? 2 : 3;
        int input = rng.nextInt(2, 8);
        int bonus = rng.nextInt(1, 7);
        int baseVal = input * mult;
        int ans = baseVal + bonus;
        return OopBlitzDomains.choiceQuestion(
                "out-super-method-chain", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does ref.calc(" + input + ") print?",
                List.of(
                        "class " + h.parent() + " { int calc(int x) { return x * " + mult + "; } }",
                        "class " + h.child() + " extends " + h.parent() + " {",
                        "    int calc(int x) { return super.calc(x) + " + bonus + "; }",
                        "}",
                        h.parent() + " ref = new " + h.child() + "();",
                        "System.out.println(ref.calc(" + input + "));"),
                String.valueOf(ans),
                List.of(String.valueOf(baseVal), String.valueOf((input + bonus) * mult), String.valueOf(input + bonus)),
                h.child() + ".calc calls super.calc(" + input + ") = " + baseVal + ", then adds " + bonus + " = " + ans + ".",
                rng);
    }

    private static StoredQuestion outPolyArraySum(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int pScore = rng.nextInt(1, 5);
        int cScore = rng.nextInt(6, 12);
        int sScore = rng.nextInt(13, 19);
        int total = cScore + sScore;
        return OopBlitzDomains.choiceQuestion(
                "out-poly-array-sum", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does this polymorphic loop print?",
                List.of(
                        "class " + h.parent() + " { int pts() { return " + pScore + "; } }",
                        "class " + h.child() + " extends " + h.parent() + " { int pts() { return " + cScore + "; } }",
                        "class " + h.sibling() + " extends " + h.parent() + " { int pts() { return " + sScore + "; } }",
                        h.parent() + "[] list = { new " + h.child() + "(), new " + h.sibling() + "() };",
                        "int sum = 0; for (" + h.parent() + " x : list) sum += x.pts();",
                        "System.out.println(sum);"),
                String.valueOf(total),
                List.of(String.valueOf(pScore * 2), String.valueOf(pScore + cScore), String.valueOf(cScore)),
                "Each element dispatches to its runtime type (" + cScore + " + " + sScore + " = " + total + ").",
                rng);
    }

    private static StoredQuestion outReferenceAliasing(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int v1 = rng.nextInt(3, 9);
        int delta = rng.nextInt(2, 7);
        int v2 = v1 + delta + rng.nextInt(4, 10);
        int ans = v1 + delta;
        return OopBlitzDomains.choiceQuestion(
                "out-reference-aliasing", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does this code print?",
                List.of(
                        d.cls() + " a = new " + d.cls() + "(" + v1 + ");",
                        d.cls() + " b = a;",
                        "b." + d.intField() + " += " + delta + ";",
                        "a = new " + d.cls() + "(" + v2 + ");",
                        "System.out.println(b." + d.intField() + ");"),
                String.valueOf(ans),
                List.of(String.valueOf(v2), String.valueOf(v1), String.valueOf(v2 + delta)),
                "b still points to the original object (" + v1 + " + " + delta + " = " + ans + ") after a is reassigned.",
                rng);
    }

    private static StoredQuestion outStringBuilderMutate(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        String s1 = d.val1();
        String s2 = d.val2();
        boolean reverse = rng.nextBoolean();
        String code = reverse
                ? "sb.append(\"-\").append(\"" + s2 + "\");"
                : "sb.append(\":\").append(\"" + s2 + "\");";
        String ans = reverse ? s1 + "-" + s2 : s1 + ":" + s2;
        return OopBlitzDomains.choiceQuestion(
                "out-stringbuilder-mutate", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What does main() print?",
                List.of(
                        "static void edit(StringBuilder sb) {",
                        "    " + code,
                        "}",
                        "StringBuilder buf = new StringBuilder(\"" + s1 + "\");",
                        "edit(buf);",
                        "System.out.println(buf.toString());"),
                ans,
                List.of(s1, s2, "Compile error"),
                "StringBuilder is mutable; edit(buf) mutates the shared buffer in place to \"" + ans + "\".",
                rng);
    }

    private static StoredQuestion outTryCatchFinally(RandomGenerator rng) {
        int init = rng.nextInt(1, 5);
        int tryAdd = rng.nextInt(2, 6);
        int catchAdd = rng.nextInt(7, 12);
        int finAdd = rng.nextInt(1, 4);
        boolean throwsEx = rng.nextBoolean();

        int ans = throwsEx ? (init + catchAdd + finAdd) : (init + tryAdd + finAdd);
        String throwLine = throwsEx
                ? "    if (true) throw new IllegalArgumentException();"
                : "    if (false) throw new IllegalArgumentException();";
        return OopBlitzDomains.choiceQuestion(
                "out-try-catch-finally", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_MEDIUM, 15,
                "What is printed at the end?",
                List.of(
                        "int r = " + init + ";",
                        "try {",
                        throwLine,
                        "    r += " + tryAdd + ";",
                        "} catch (RuntimeException e) {",
                        "    r += " + catchAdd + ";",
                        "} finally {",
                        "    r += " + finAdd + ";",
                        "}",
                        "System.out.println(r);"),
                String.valueOf(ans),
                List.of(
                        String.valueOf(throwsEx ? init + tryAdd + finAdd : init + catchAdd + finAdd),
                        String.valueOf(throwsEx ? init + catchAdd : init + tryAdd),
                        String.valueOf(init + finAdd)),
                throwsEx
                        ? "IllegalArgumentException IS-A RuntimeException, so catch adds " + catchAdd + " and finally adds " + finAdd + " (= " + ans + ")."
                        : "No exception is thrown: try adds " + tryAdd + " and finally still runs (+" + finAdd + ") = " + ans + ".",
                rng);
    }

    // ==================== HARD (8) ====================

    private static StoredQuestion outPassByValueVsReassign(RandomGenerator rng) {
        ClassDomain d = OopBlitzDomains.pickDomain(rng);
        int init = rng.nextInt(3, 10);
        int add = rng.nextInt(2, 7);
        int other = init + rng.nextInt(10, 20);
        int prim = rng.nextInt(11, 25);
        boolean reassignParam = rng.nextBoolean();

        List<String> lines = reassignParam
                ? List.of(
                        "static void bump(" + d.cls() + " b, int n) {",
                        "    b = new " + d.cls() + "(" + other + ");",
                        "    b." + d.intField() + " += " + add + ";",
                        "    n += " + add + ";",
                        "}",
                        d.cls() + " box = new " + d.cls() + "(" + init + "); int x = " + prim + ";",
                        "bump(box, x);",
                        "System.out.println(box." + d.intField() + " + \",\" + x);")
                : List.of(
                        "static void bump(" + d.cls() + " b, int n) {",
                        "    b." + d.intField() + " += " + add + ";",
                        "    n += " + add + ";",
                        "}",
                        d.cls() + " box = new " + d.cls() + "(" + init + "); int x = " + prim + ";",
                        "bump(box, x);",
                        "System.out.println(box." + d.intField() + " + \",\" + x);");

        String correct = reassignParam ? (init + "," + prim) : ((init + add) + "," + prim);
        return OopBlitzDomains.choiceQuestion(
                "out-pass-by-value-vs-reassign", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What does main() print?",
                lines,
                correct,
                List.of(
                        (init + add) + "," + (prim + add),
                        (other + add) + "," + prim,
                        reassignParam ? ((init + add) + "," + prim) : (init + "," + prim)),
                reassignParam
                        ? "Java passes references by value: reassigning b inside bump does not change caller's box (" + init + ")."
                        : "Mutating b." + d.intField() + " changes box (" + (init + add) + "), while primitive x remains " + prim + ".",
                rng);
    }

    private static StoredQuestion outStaticMethodHiding(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        return OopBlitzDomains.choiceQuestion(
                "out-static-method-hiding", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What does obj.tag() print?",
                List.of(
                        "class " + h.parent() + " { static String tag() { return \"" + h.parentTag() + "\"; } }",
                        "class " + h.child() + " extends " + h.parent() + " { static String tag() { return \"" + h.childTag() + "\"; } }",
                        h.parent() + " obj = new " + h.child() + "();",
                        "System.out.println(obj.tag());"),
                h.parentTag(),
                List.of(h.childTag(), "null", "Compile error"),
                "Static methods are bound at compile time by declared reference type (" + h.parent() + "), not runtime type.",
                rng);
    }

    private static StoredQuestion outFieldShadowing(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        int pVal = rng.nextInt(1, 7);
        int cVal = pVal + rng.nextInt(3, 12);
        return OopBlitzDomains.choiceQuestion(
                "out-field-shadowing", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What does this snippet print?",
                List.of(
                        "class " + h.parent() + " { int x = " + pVal + "; int getX() { return x; } }",
                        "class " + h.child() + " extends " + h.parent() + " { int x = " + cVal + "; int getX() { return x; } }",
                        h.parent() + " ref = new " + h.child() + "();",
                        "System.out.println(ref.x + \",\" + ref.getX());"),
                pVal + "," + cVal,
                List.of(cVal + "," + cVal, pVal + "," + pVal, cVal + "," + pVal),
                "Fields are resolved by declared type (ref.x = " + pVal + "), whereas overridden getX() uses runtime type (" + cVal + ").",
                rng);
    }

    private static StoredQuestion outOverloadCompileTime(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        boolean castToString = rng.nextBoolean();
        String callLine = castToString
                ? "System.out.println(describe((String) item));"
                : "System.out.println(describe(item));";
        String correct = castToString ? h.childTag() : h.parentTag();
        String other = castToString ? h.parentTag() : h.childTag();
        return OopBlitzDomains.choiceQuestion(
                "out-overload-compile-time", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "Which overloaded describe(...) method executes?",
                List.of(
                        "static String describe(Object o) { return \"" + h.parentTag() + "\"; }",
                        "static String describe(String s) { return \"" + h.childTag() + "\"; }",
                        "Object item = \"EIU\";",
                        callLine),
                correct,
                List.of(other, "null", "Compile error"),
                castToString
                        ? "Casting (String) item makes the static argument type String, selecting describe(String)."
                        : "Overload resolution uses declared compile-time type Object, selecting describe(Object).",
                rng);
    }

    private static StoredQuestion outCtor3LevelInit(RandomGenerator rng) {
        TripleHierarchy t = OopBlitzDomains.pickTriple(rng);
        String ans = t.gTag() + t.pTag() + t.cTag();
        return OopBlitzDomains.choiceQuestion(
                "out-ctor-3level-init", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What does new " + t.child() + "() print?",
                List.of(
                        "class " + t.grand() + " { " + t.grand() + "() { System.out.print(\"" + t.gTag() + "\"); } }",
                        "class " + t.parent() + " extends " + t.grand() + " { " + t.parent() + "() { System.out.print(\"" + t.pTag() + "\"); } }",
                        "class " + t.child() + " extends " + t.parent() + " { " + t.child() + "() { System.out.print(\"" + t.cTag() + "\"); } }",
                        "new " + t.child() + "();"),
                ans,
                List.of(t.cTag() + t.pTag() + t.gTag(), t.pTag() + t.cTag(), t.cTag()),
                "Constructor chaining runs top-down from root superclass: " + t.grand() + " -> " + t.parent() + " -> " + t.child() + " (" + ans + ").",
                rng);
    }

    private static StoredQuestion outInterfaceDefaultClassWins(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        String iface = h.parent() + "Spec";
        return OopBlitzDomains.choiceQuestion(
                "out-interface-default-class-wins", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "Class and interface share the same method. What prints?",
                List.of(
                        "interface " + iface + " { default String " + h.method() + "() { return \"" + h.childOut() + "\"; } }",
                        "class " + h.parent() + " { public String " + h.method() + "() { return \"" + h.parentOut() + "\"; } }",
                        "class " + h.child() + " extends " + h.parent() + " implements " + iface + " { }",
                        "System.out.println(new " + h.child() + "()." + h.method() + "());"),
                h.parentOut(),
                List.of(h.childOut(), "Compile error", "null"),
                "JLS 'class wins' rule: concrete method in superclass " + h.parent() + " overrides the interface default method.",
                rng);
    }

    private static StoredQuestion outDowncastRuntime(RandomGenerator rng) {
        HierarchyDomain h = OopBlitzDomains.pickHierarchy(rng);
        boolean validCast = rng.nextBoolean();
        String runtime = validCast ? h.child() : h.sibling();
        String correct = validCast ? h.childOut() : "ClassCastException";
        return OopBlitzDomains.choiceQuestion(
                "out-downcast-runtime", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What happens when running this code?",
                List.of(
                        "class " + h.parent() + " { String " + h.method() + "() { return \"" + h.parentOut() + "\"; } }",
                        "class " + h.child() + " extends " + h.parent() + " { String " + h.method() + "() { return \"" + h.childOut() + "\"; } }",
                        "class " + h.sibling() + " extends " + h.parent() + " { }",
                        h.parent() + " ref = new " + runtime + "();",
                        h.child() + " sub = (" + h.child() + ") ref;",
                        "System.out.println(sub." + h.method() + "());"),
                correct,
                List.of(
                        validCast ? "ClassCastException" : h.childOut(),
                        h.parentOut(),
                        "Compile error"),
                validCast
                        ? "ref holds a " + h.child() + " at runtime, so the downcast succeeds and prints \"" + h.childOut() + "\"."
                        : "ref holds a " + h.sibling() + " at runtime, so casting to " + h.child() + " throws ClassCastException.",
                rng);
    }

    private static StoredQuestion outIntegerCache(RandomGenerator rng) {
        boolean inCache = rng.nextBoolean();
        int val = inCache ? rng.nextInt(1, 127) : rng.nextInt(200, 999);
        String correct = inCache ? "true true" : "false true";
        return OopBlitzDomains.choiceQuestion(
                "out-integer-cache", OopBlitzQuestionBank.KIND_OUTPUT, OopBlitzQuestionBank.DIFF_HARD, 20,
                "What does autoboxed Integer comparison print?",
                List.of(
                        "Integer a = " + val + ";",
                        "Integer b = " + val + ";",
                        "System.out.println((a == b) + \" \" + a.equals(b));"),
                correct,
                List.of(inCache ? "false true" : "true true", "false false", "true false"),
                inCache
                        ? val + " is inside Java's IntegerCache (-128..127), so a and b share the cached object (== is true)."
                        : val + " is outside IntegerCache (-128..127), creating two Integer instances (== is false, .equals() is true).",
                rng);
    }
}
