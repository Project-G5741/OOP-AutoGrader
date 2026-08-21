package com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReflectionClassParserTest {

    private final ReflectionClassParser parser = new ReflectionClassParser();

    @Test
    void loadsStaticNestedClassWithOuterMetadata(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir, "Pen", """
                public class Pen {
                    public static class PenBuilder {
                        private String brand;
                        public PenBuilder setBrand(String brand) { this.brand = brand; return this; }
                        public Pen build() { return new Pen(); }
                    }
                }
                """);

        List<ParsedClass> parsed = parser.parseClasses(classesDir);

        ParsedClass penBuilder = parsed.stream()
                .filter(pc -> "PenBuilder".equals(pc.simpleName))
                .findFirst()
                .orElseThrow();
        assertEquals("Pen", penBuilder.outerSimpleName);
        assertTrue(penBuilder.isStatic);
        assertTrue(penBuilder.methods.stream().anyMatch(m -> "setBrand".equals(m.name)));
    }

    @Test
    void skipsMultiDollarClassFiles(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir, "Outer", """
                public class Outer {
                    void run() {
                        Runnable local = new Runnable() { public void run() {} };
                    }
                }
                """);

        List<ParsedClass> parsed = parser.parseClasses(classesDir);
        assertTrue(parsed.stream().noneMatch(pc -> pc.simpleName.contains("$")));
    }

    private void compileSources(Path outputDir, String className, String source) throws Exception {
        Path sourceFile = outputDir.getParent().resolve(className + ".java");
        Files.writeString(sourceFile, source);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, "-d", outputDir.toString(), sourceFile.toString());
        assertEquals(0, status, "Compilation failed");
    }
}
