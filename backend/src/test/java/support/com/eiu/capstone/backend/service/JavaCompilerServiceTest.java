package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCompilerServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void compileSources_jdkUtilTypesStillCompile() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("jdk");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "Bag.java",
                        "public class Bag { java.util.List<String> items = new java.util.ArrayList<>(); }"
                                .getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = service.compileSources(sources, outputDir);

        assertTrue(outcome.succeeded());
        assertTrue(Files.exists(outputDir.resolve("Bag.class")));
    }

    @Test
    void compileSources_doesNotResolveApplicationClasspathTypes() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("spring");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "Hook.java",
                        """
                        import org.springframework.stereotype.Service;
                        @Service
                        public class Hook {}
                        """.getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = service.compileSources(sources, outputDir);

        assertFalse(outcome.succeeded());
        assertFalse(Files.exists(outputDir.resolve("Hook.class")));
        assertTrue(outcome.messages().stream().anyMatch(message ->
                message.toLowerCase().contains("cannot find symbol")
                        || message.toLowerCase().contains("package org.springframework")
                        || message.toLowerCase().contains("service")));
    }

    @Test
    void compileSources_writesClassFilesForValidSources() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject("A.java", "public class A {}".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject("B.java", "public class B {}".getBytes(StandardCharsets.UTF_8)));

        service.compileSources(sources, outputDir);

        assertTrue(Files.exists(outputDir.resolve("A.class")));
        assertTrue(Files.exists(outputDir.resolve("B.class")));
    }

    @Test
    void compileSources_emptyListIsNoOp() {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        CompileOutcome outcome = service.compileSources(List.of(), tempDir);
        assertTrue(outcome.succeeded());
        assertTrue(outcome.messages().isEmpty());
        assertEquals(0, outcome.classFileCount());
    }

    @Test
    void compileSources_mixedGoodAndBad_emitsOnlyGoodClass() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("mixed");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "Good.java", "public class Good {}".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject(
                        "Bad.java", "public class Bad {".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = assertDoesNotThrow(() -> service.compileSources(sources, outputDir));

        assertFalse(outcome.succeeded());
        assertTrue(Files.exists(outputDir.resolve("Good.class")));
        assertFalse(Files.exists(outputDir.resolve("Bad.class")));
        assertEquals(1, outcome.classFileCount());
    }

    @Test
    void compileSources_mutualRefsSurviveBrokenSibling() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("mutual");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "A.java", "public class A { B peer; }".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject(
                        "B.java", "public class B { A peer; }".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject(
                        "Bad.java", "public class Bad {".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = assertDoesNotThrow(() -> service.compileSources(sources, outputDir));

        assertFalse(outcome.succeeded());
        assertTrue(Files.exists(outputDir.resolve("A.class")));
        assertTrue(Files.exists(outputDir.resolve("B.class")));
        assertFalse(Files.exists(outputDir.resolve("Bad.class")));
        assertEquals(2, outcome.classFileCount());
    }

    @Test
    void compileSources_independentSurvivesBrokenTypeAndItsDependent() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("cascade");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "Good.java", "public class Good {}".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject(
                        "Student.java", "public class Student {".getBytes(StandardCharsets.UTF_8)),
                new MemorySourceJavaFileObject(
                        "BankAccount.java",
                        "public class BankAccount { Student owner; }".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = assertDoesNotThrow(() -> service.compileSources(sources, outputDir));

        assertFalse(outcome.succeeded());
        assertTrue(Files.exists(outputDir.resolve("Good.class")));
        assertFalse(Files.exists(outputDir.resolve("Student.class")));
        assertFalse(Files.exists(outputDir.resolve("BankAccount.class")));
        assertEquals(1, outcome.classFileCount());
    }

    @Test
    void compileSources_onlyBrokenSources_doesNotThrow() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("broken");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject(
                        "Broken.java", "public class Broken {".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = assertDoesNotThrow(() -> service.compileSources(sources, outputDir));

        assertFalse(outcome.succeeded());
        assertFalse(Files.exists(outputDir.resolve("Broken.class")));
        assertEquals(0, outcome.classFileCount());
        assertTrue(outcome.messages().stream().anyMatch(message -> message.contains("line")));
    }

    @Test
    void compileSources_syntaxErrorIncludesDiagnostics() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject("Broken.java", "public class Broken {".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome outcome = service.compileSources(sources, outputDir);

        assertFalse(outcome.succeeded());
        assertTrue(outcome.messages().stream().anyMatch(message -> message.contains("line")));
    }

    @Test
    void compileSources_recoversOnSameThreadAfterSyntaxError() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);

        List<JavaFileObject> broken = List.of(
                new MemorySourceJavaFileObject("Broken.java", "public class Broken {".getBytes(StandardCharsets.UTF_8)));

        CompileOutcome brokenOutcome = service.compileSources(broken, outputDir);
        assertFalse(brokenOutcome.succeeded());

        List<JavaFileObject> valid = List.of(
                new MemorySourceJavaFileObject("Fixed.java", "public class Fixed {}".getBytes(StandardCharsets.UTF_8)));
        service.compileSources(valid, outputDir);

        assertTrue(Files.exists(outputDir.resolve("Fixed.class")));
    }

    @Test
    void compileSources_parallelThreadsBothSucceed() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        try {
            pool.submit(() -> {
                try {
                    Path out = tempDir.resolve("t1");
                    Files.createDirectories(out);
                    service.compileSources(
                            List.of(new MemorySourceJavaFileObject("One.java", "public class One {}".getBytes(StandardCharsets.UTF_8))),
                            out);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
            pool.submit(() -> {
                try {
                    Path out = tempDir.resolve("t2");
                    Files.createDirectories(out);
                    service.compileSources(
                            List.of(new MemorySourceJavaFileObject("Two.java", "public class Two {}".getBytes(StandardCharsets.UTF_8))),
                            out);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertTrue(Files.exists(tempDir.resolve("t1/One.class")));
            assertTrue(Files.exists(tempDir.resolve("t2/Two.class")));
        } finally {
            pool.shutdownNow();
        }
    }
}
