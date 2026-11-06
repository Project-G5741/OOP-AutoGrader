package unit.com.eiu.capstone.backend.grading.testcase.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerClassReflector;

/**
 * Parity: worker {@code --reflect} DTO shape must match in-process {@link ReflectionClassParser}.
 */
class WorkerClassReflectorTest {

    @Test
    void parseClasses_matchesInProcessParserOnNestedAndHeritage(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileSources(classesDir, "Animal", "public class Animal {}",
                "Pen", """
                        public class Pen {
                            public static class PenBuilder extends Animal implements java.io.Serializable {
                                private String brand;
                                public PenBuilder setBrand(String brand) { this.brand = brand; return this; }
                                public Pen build() { return new Pen(); }
                            }
                        }
                        """);

        List<WorkerClassReflector.ReflectClass> worker = WorkerClassReflector.parseClasses(classesDir);
        List<ParsedClass> api = new ReflectionClassParser(null).parseInProcess(classesDir);

        WorkerClassReflector.ReflectClass workerBuilder = worker.stream()
                .filter(c -> "PenBuilder".equals(c.simpleName))
                .findFirst()
                .orElseThrow();
        ParsedClass apiBuilder = api.stream()
                .filter(c -> "PenBuilder".equals(c.simpleName))
                .findFirst()
                .orElseThrow();

        assertEquals(apiBuilder.outerSimpleName, workerBuilder.outerSimpleName);
        assertEquals(apiBuilder.superclassSimpleName, workerBuilder.superclassSimpleName);
        assertEquals(apiBuilder.interfaceSimpleNames, workerBuilder.interfaceSimpleNames);
        assertTrue(workerBuilder.isStatic);
        assertEquals(apiBuilder.isStatic, workerBuilder.isStatic);
        assertEquals(apiBuilder.methods.size(), workerBuilder.methods.size());
        assertEquals(apiBuilder.fields.size(), workerBuilder.fields.size());
    }

    private void compileSources(Path outputDir, String... namesAndSources) throws Exception {
        if (namesAndSources.length == 0 || namesAndSources.length % 2 != 0) {
            throw new IllegalArgumentException("Expected className/source pairs");
        }
        Path parent = outputDir.getParent();
        java.util.List<String> compilerArgs = new java.util.ArrayList<>();
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
