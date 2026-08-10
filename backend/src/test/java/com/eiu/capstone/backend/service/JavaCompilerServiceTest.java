package com.eiu.capstone.backend.service;

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

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCompilerServiceTest {

    @TempDir
    Path tempDir;

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

        List<String> messages = service.compileSources(List.of(), tempDir);
        assertTrue(messages.isEmpty());
    }

    @Test
    void compileSources_syntaxErrorIncludesDiagnostics() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);

        List<JavaFileObject> sources = List.of(
                new MemorySourceJavaFileObject("Broken.java", "public class Broken {".getBytes(StandardCharsets.UTF_8)));

        SubmissionProcessingException ex = assertThrows(
                SubmissionProcessingException.class,
                () -> service.compileSources(sources, outputDir));

        assertTrue(ex.getMessage().contains("Compilation failed"));
        assertTrue(ex.getMessage().contains("line"));
    }

    @Test
    void compileSources_recoversOnSameThreadAfterSyntaxError() throws Exception {
        JavaCompilerService service = new JavaCompilerService();
        service.initCompiler();

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);

        List<JavaFileObject> broken = List.of(
                new MemorySourceJavaFileObject("Broken.java", "public class Broken {".getBytes(StandardCharsets.UTF_8)));

        assertThrows(SubmissionProcessingException.class, () -> service.compileSources(broken, outputDir));

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
