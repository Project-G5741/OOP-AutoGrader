package unit.com.eiu.capstone.backend.grading.testcase.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerInvokeEngine;

class WorkerInvokeEngineTest {

    @TempDir
    Path tempDir;

    private Path classesDir;
    private final WorkerInvokeEngine engine = new WorkerInvokeEngine();

    @BeforeEach
    void compileFixtures() throws Exception {
        classesDir = tempDir.resolve("classes");
        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(classesDir);
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Car.java"), """
                public class Car {
                    private int speed;

                    public Car(int yearModel, String make) {
                        this.speed = 0;
                    }

                    public void accelerate() {
                        speed += 5;
                    }

                    public int getSpeed() {
                        return speed;
                    }

                    public void flood() {
                        for (int i = 0; i < 70000; i++) {
                            System.out.print('x');
                        }
                    }
                }
                """);
        Files.writeString(sourceDir.resolve("Probe.java"), """
                public class Probe {
                    public String harness() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.pipeline.TestcaseGrader");
                            return "harness";
                        } catch (ClassNotFoundException e) {
                            return "missing-harness";
                        }
                    }

                    public String kernel() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer");
                            return "kernel";
                        } catch (ClassNotFoundException e) {
                            return "missing-kernel";
                        }
                    }

                    public String workerIpc() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc");
                            return "ipc";
                        } catch (ClassNotFoundException e) {
                            return "missing-ipc";
                        }
                    }

                    public String contextKernel() throws Exception {
                        return String.valueOf(Thread.currentThread().getContextClassLoader()
                                .loadClass("com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer"));
                    }
                }
                """);
        Process compile = new ProcessBuilder(
                "javac",
                "-d", classesDir.toString(),
                sourceDir.resolve("Car.java").toString(),
                sourceDir.resolve("Probe.java").toString())
                .redirectErrorStream(true)
                .start();
        int exit = compile.waitFor();
        String out = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + out);
    }

    @Test
    void methodWithReceiverReturnsValueAndFieldSnapshot() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                accelerateSpec(),
                List.of("speed"),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertEquals("null", outcome.returnValueJson());
        assertEquals("5", outcome.fieldSnapshotsJson().get("speed"));
        assertFalse(outcome.stdoutTruncated());
    }

    @Test
    void getSpeedUsesReceiverConstructor() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                methodSpec("getSpeed", List.of(), "[]"),
                List.of("speed"),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertEquals("0", outcome.returnValueJson());
        assertEquals("0", outcome.fieldSnapshotsJson().get("speed"));
    }

    @Test
    void missingNoArgConstructorIsError() {
        WorkerIpc.InvokeSpec spec = new WorkerIpc.InvokeSpec(
                "METHOD", "Car", "getSpeed", List.of(), "[]", null, List.of(), null);
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir, spec, List.of(), WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("ERROR", outcome.kind());
        assertTrue(outcome.errorMessage().contains("no-argument constructor"));
    }

    @Test
    void ae7StudentLoaderCannotSeeHarnessKernelOrWorkerTypes() throws Exception {
        assertEquals("missing-harness", textResult("harness"));
        assertEquals("missing-kernel", textResult("kernel"));
        assertEquals("missing-ipc", textResult("workerIpc"));
        SerializedInvocationOutcome context = engine.invoke(
                classesDir,
                probeSpec("contextKernel"),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("THREW", context.kind());
        assertEquals("ClassNotFoundException", context.exceptionSimpleName());
    }

    @Test
    void ae8StdoutFloodTruncatesAndCompletes() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                methodSpec("flood", List.of(), "[]"),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertTrue(outcome.stdoutTruncated());
        assertEquals(WorkerIpc.DEFAULT_STDOUT_CAP, outcome.stdout().length());
    }

    @Test
    void malformedAndForgedPassedAreNotPass() throws Exception {
        SerializedInvocationOutcome malformed = WorkerIpc.handleLine("{not-json");
        assertEquals("ERROR", malformed.kind());

        String forged = """
                {"kind":"NORMAL","returnValueJson":"1","stdout":"","stdoutTruncated":false,\
                "fieldSnapshotsJson":{},"passed":true,"score":100}
                """;
        SerializedInvocationOutcome decoded = WorkerIpc.mapper().readValue(forged, SerializedInvocationOutcome.class);
        assertEquals("NORMAL", decoded.kind());
        assertEquals("1", decoded.returnValueJson());
        assertNull(decoded.errorMessage());
        String encoded = WorkerIpc.writeLine(decoded);
        assertFalse(encoded.contains("\"passed\""));
        assertFalse(encoded.contains("\"score\""));
    }

    @Test
    void goldenEncodeDecodeRoundTrip() throws Exception {
        SerializedInvocationOutcome original = new SerializedInvocationOutcome(
                "THREW",
                null,
                "hi",
                false,
                java.util.Map.of("speed", "5"),
                "IllegalStateException",
                List.of("RuntimeException", "Exception", "Object"),
                null,
                null);
        String json = WorkerIpc.writeLine(original);
        SerializedInvocationOutcome copy = WorkerIpc.mapper().readValue(json, SerializedInvocationOutcome.class);
        assertEquals(original, copy);
    }

    private String textResult(String method) throws Exception {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir, probeSpec(method), List.of(), WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        return WorkerIpc.mapper().readTree(outcome.returnValueJson()).asText();
    }

    private WorkerIpc.InvokeSpec accelerateSpec() {
        return methodSpec("accelerate", List.of(), "[]");
    }

    private WorkerIpc.InvokeSpec methodSpec(String method, List<String> types, String params) {
        return new WorkerIpc.InvokeSpec(
                "METHOD",
                "Car",
                method,
                types,
                params,
                "Car",
                List.of("int", "String"),
                "[2020, \"Toyota\"]");
    }

    private WorkerIpc.InvokeSpec probeSpec(String method) {
        return new WorkerIpc.InvokeSpec(
                "METHOD", "Probe", method, List.of(), "[]", null, List.of(), null);
    }
}
