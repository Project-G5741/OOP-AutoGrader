package unit.com.eiu.capstone.backend.grading.testcase.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.kernel.ValueComparator;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerMain;

class WorkerJarIsolationTest {

    private static final String SPRING_BINARY = "org/springframework";

    @Test
    void workerMainIsLaunchableWithoutSpringType() throws Exception {
        Method main = WorkerMain.class.getMethod("main", String[].class);
        assertNotNull(main);
        assertNull(WorkerMain.class.getAnnotation(Component.class));
        assertFalse(classBytesContainSpring(WorkerMain.class));
    }

    @Test
    void kernelTypesAreSpringFree() throws Exception {
        assertNull(JsonValueCoercer.class.getAnnotation(Component.class));
        assertFalse(classBytesContainSpring(JsonValueCoercer.class));
        assertFalse(classBytesContainSpring(ValueComparator.class));
    }

    @Test
    void workerAndKernelClassFilesDoNotEmbedSpringDescriptors() throws Exception {
        Path classes = Path.of("target/classes/com/eiu/capstone/backend/grading/testcase");
        assertTrue(Files.isDirectory(classes), "compiled testcase package missing");
        try (Stream<Path> stream = Files.walk(classes)) {
            List<Path> classFiles = stream
                    .filter(path -> path.toString().replace('\\', '/').contains("/kernel/")
                            || path.toString().replace('\\', '/').contains("/worker/"))
                    .filter(path -> path.toString().endsWith(".class"))
                    .toList();
            assertFalse(classFiles.isEmpty(), "kernel/worker class files missing");
            for (Path classFile : classFiles) {
                String bytes = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
                assertFalse(bytes.contains(SPRING_BINARY), "Spring descriptor in " + classFile);
            }
        }
    }

    @Test
    @EnabledIf("workerJarExists")
    void packagedWorkerJarExcludesSpringAndContainsMain() throws Exception {
        Path workerJar = workerJarPath();
        try (JarFile jar = new JarFile(workerJar.toFile())) {
            assertNotNull(jar.getJarEntry("com/eiu/capstone/backend/grading/testcase/worker/WorkerMain.class"));
            assertNotNull(jar.getJarEntry("com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngine.class"));
            assertNotNull(jar.getJarEntry("com/eiu/capstone/backend/grading/testcase/worker/WorkerIpc.class"));
            assertNotNull(jar.getJarEntry("com/eiu/capstone/backend/grading/testcase/kernel/JsonValueCoercer.class"));
            assertNotNull(jar.getJarEntry("com/eiu/capstone/backend/grading/testcase/SerializedInvocationOutcome.class"));
            assertTrue(jar.stream().noneMatch(entry -> entry.getName().startsWith("org/springframework/")),
                    "worker JAR must not contain Spring types");
            String mainClass = jar.getManifest().getMainAttributes().getValue("Main-Class");
            assertTrue("com.eiu.capstone.backend.grading.testcase.worker.WorkerMain".equals(mainClass),
                    "worker JAR Main-Class");
        }
    }

    static boolean workerJarExists() {
        Path jar = workerJarPath();
        if (!Files.isRegularFile(jar)) {
            return false;
        }
        Path engine = Path.of("target/classes/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngine.class");
        if (!Files.isRegularFile(engine)) {
            return true;
        }
        try {
            return Files.getLastModifiedTime(jar).toMillis() >= Files.getLastModifiedTime(engine).toMillis();
        } catch (IOException e) {
            return false;
        }
    }

    private static Path workerJarPath() {
        return Path.of("target/backend-1.0.0-worker.jar");
    }

    private static boolean classBytesContainSpring(Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing class bytes for " + type.getName());
            String bytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            return bytes.contains(SPRING_BINARY);
        }
    }
}
