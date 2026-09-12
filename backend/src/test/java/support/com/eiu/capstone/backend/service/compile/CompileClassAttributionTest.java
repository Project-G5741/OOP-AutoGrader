package support.com.eiu.capstone.backend.service.compile;

import com.eiu.capstone.backend.service.compile.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.service.JavaCompilerService;
import com.eiu.capstone.backend.service.compile.CompileClassAttribution.Result;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileClassAttributionTest {

    @TempDir
    Path tempDir;

    @Test
    void independentSiblingKeepsOnlyBrokenTypeFailed() throws Exception {
        Result result = attribute(
                List.of("Good", "Bad"),
                entry("Good.java", "public class Good {}"),
                entry("Bad.java", "public class Bad {"));

        assertTrue(result.failedClassNames().contains("Bad"));
        assertFalse(result.failedClassNames().contains("Good"));
        assertEquals("Unclosed class on line 1", result.compileErrorsByClassName().get("Bad"));
        assertFalse(result.compileErrorsByClassName().containsKey("Good"));
    }

    @Test
    void dependentGetsPointerNotRootSyntaxDump() throws Exception {
        Result result = attribute(
                List.of("Student", "BankAccount"),
                entry("Student.java", "public class Student {"),
                entry("BankAccount.java", "public class BankAccount { Student owner; }"));

        assertTrue(result.failedClassNames().contains("Student"));
        assertTrue(result.failedClassNames().contains("BankAccount"));
        assertEquals("See Student",
                result.compileErrorsByClassName().get("BankAccount"));
        assertEquals("Unclosed class on line 1",
                result.compileErrorsByClassName().get("Student"));
        assertFalse(result.compileErrorsByClassName().get("BankAccount").contains("reached end of file"));
    }

    @Test
    void transitiveDependentIsFailed() throws Exception {
        Result result = attribute(
                List.of("A", "B", "C"),
                entry("A.java", "public class A {"),
                entry("B.java", "public class B { A a; }"),
                entry("C.java", "public class C { B b; }"));

        assertTrue(result.failedClassNames().containsAll(List.of("A", "B", "C")));
        assertEquals("See A", result.compileErrorsByClassName().get("B"));
        assertEquals("See A", result.compileErrorsByClassName().get("C"));
    }

    @Test
    void typesInTheSameBrokenFileFailTogether() throws Exception {
        Result result = attribute(
                List.of("Student", "Helper"),
                entry("Student.java", """
                        public class Student {
                        class Helper {}
                        """));

        assertTrue(result.failedClassNames().contains("Student"));
        assertTrue(result.failedClassNames().contains("Helper"));
        assertEquals(
                result.compileErrorsByClassName().get("Student"),
                result.compileErrorsByClassName().get("Helper"));
    }

    @Test
    void cannotFindSymbolNamesTheMissingType() throws Exception {
        Result result = attribute(
                List.of("SMSSubscriber", "Observer"),
                entry("SMSSubscriber.java", "public class SMSSubscriber implements Observer {}"));

        assertEquals("Observer not found", result.compileErrorsByClassName().get("SMSSubscriber"));
    }

    @Test
    void publicClassFilenameMismatchUsesFileStem() throws Exception {
        Result result = attribute(
                List.of("Observer", "EmailSubscriber"),
                entry("Observer.java", "public class EmailSubscriber {}"));

        assertEquals("Wrong class in this file", result.compileErrorsByClassName().get("Observer"));
        assertEquals("Declared in Observer.java", result.compileErrorsByClassName().get("EmailSubscriber"));
    }

    @Test
    void nestedStaticClassSharesOuterSyntaxError() throws Exception {
        Result result = attribute(
                List.of("Outer", "Outer.Inner"),
                entry("Outer.java", """
                        public class Outer {
                          public static class Inner {}
                        """));

        String outerError = result.compileErrorsByClassName().get("Outer");
        assertEquals("Unclosed class on line 2", outerError);
        assertEquals(outerError, result.compileErrorsByClassName().get("Outer.Inner"));
        assertFalse(result.compileErrorsByClassName().containsValue("Declared in Outer.java"));
        assertFalse(result.compileErrorsByClassName().containsValue("Wrong class in this file"));
    }

    private Result attribute(List<String> preferredOrder, SourceEntry... sources) throws Exception {
        JavaCompilerService compiler = new JavaCompilerService();
        compiler.initCompiler();
        Path outputDir = Files.createDirectories(tempDir.resolve("classes-" + preferredOrder.hashCode()));
        List<SourceEntry> sourceList = List.of(sources);
        List<JavaFileObject> files = sourceList.stream()
                .map(entry -> new MemorySourceJavaFileObject(
                        entry.logicalPath(),
                        entry.source().getBytes(StandardCharsets.UTF_8)))
                .map(JavaFileObject.class::cast)
                .toList();
        CompileOutcome outcome = compiler.compileSources(files, outputDir);
        return CompileClassAttribution.attribute(outcome, sourceList, preferredOrder);
    }

    private static SourceEntry entry(String path, String source) {
        return new SourceEntry(path, source);
    }
}
