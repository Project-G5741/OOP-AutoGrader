package unit.com.eiu.capstone.backend.grading;

import com.eiu.capstone.backend.grading.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void classWithNoExtendsOrImplements_hasNoHeritageNames(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir, "Plain", """
                public class Plain {
                }
                """);

        ParsedClass plain = named(parser.parseClasses(classesDir), "Plain");
        assertNull(plain.superclassSimpleName);
        assertTrue(plain.interfaceSimpleNames.isEmpty());
    }

    @Test
    void implementsObserver_capturesDeclaredInterface(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir,
                "Observer", """
                        public interface Observer {
                            void update(String message);
                        }
                        """,
                "EmailSubscriber", """
                        public class EmailSubscriber implements Observer {
                            public void update(String message) {}
                        }
                        """);

        ParsedClass subscriber = named(parser.parseClasses(classesDir), "EmailSubscriber");
        assertNull(subscriber.superclassSimpleName);
        assertEquals(List.of("Observer"), subscriber.interfaceSimpleNames);
    }

    @Test
    void extendsAnimal_capturesImmediateSuperclass(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir,
                "Animal", """
                        public class Animal {
                        }
                        """,
                "Dog", """
                        public class Dog extends Animal {
                        }
                        """);

        ParsedClass dog = named(parser.parseClasses(classesDir), "Dog");
        assertEquals("Animal", dog.superclassSimpleName);
        assertTrue(dog.interfaceSimpleNames.isEmpty());
    }

    @Test
    void extraImplements_capturesEveryDeclaredInterface(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir,
                "Observer", """
                        public interface Observer {
                        }
                        """,
                "EmailSubscriber", """
                        public class EmailSubscriber implements Observer, java.io.Serializable {
                        }
                        """);

        ParsedClass subscriber = named(parser.parseClasses(classesDir), "EmailSubscriber");
        assertTrue(subscriber.interfaceSimpleNames.contains("Observer"));
        assertTrue(subscriber.interfaceSimpleNames.contains("Serializable"));
        assertEquals(2, subscriber.interfaceSimpleNames.size());
    }

    @Test
    void nestedType_capturesDeclaredHeritage(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir,
                "Animal", """
                        public class Animal {
                        }
                        """,
                "Pen", """
                        public class Pen {
                            public static class PenBuilder extends Animal implements java.io.Serializable {
                            }
                        }
                        """);

        ParsedClass builder = named(parser.parseClasses(classesDir), "PenBuilder");
        assertEquals("Pen", builder.outerSimpleName);
        assertEquals("Animal", builder.superclassSimpleName);
        assertEquals(List.of("Serializable"), builder.interfaceSimpleNames);
    }

    private ParsedClass named(List<ParsedClass> parsed, String simpleName) {
        return parsed.stream()
                .filter(pc -> simpleName.equals(pc.simpleName))
                .findFirst()
                .orElseThrow();
    }

    private void compileSources(Path outputDir, String className, String source) throws Exception {
        compileSources(outputDir, new String[] { className, source });
    }

    private void compileSources(Path outputDir, String... namesAndSources) throws Exception {
        if (namesAndSources.length == 0 || namesAndSources.length % 2 != 0) {
            throw new IllegalArgumentException("Expected className/source pairs");
        }
        Path parent = outputDir.getParent();
        List<String> compilerArgs = new java.util.ArrayList<>();
        compilerArgs.add("-d");
        compilerArgs.add(outputDir.toString());
        for (int i = 0; i < namesAndSources.length; i += 2) {
            Path sourceFile = parent.resolve(namesAndSources[i] + ".java");
            Files.writeString(sourceFile, namesAndSources[i + 1]);
            compilerArgs.add(sourceFile.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, compilerArgs.toArray(String[]::new));
        assertEquals(0, status, "Compilation failed");
    }
}
