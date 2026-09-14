package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.model.InvocationKind;

import support.com.eiu.capstone.backend.grading.testcase.WorkerTestSupport;

class InvocationRunnerTest {

    @TempDir
    Path tempDir;

    private Path classesDir;
    private InvocationRunner runner;
    private WorkerSessionHandle handle;

    @BeforeEach
    void setUp() throws Exception {
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
                }
                """);

        Process compile = new ProcessBuilder(
                "javac",
                "-d", classesDir.toString(),
                sourceDir.resolve("Car.java").toString())
                .redirectErrorStream(true)
                .start();
        int exitCode = compile.waitFor();
        String compileOutput = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exitCode, () -> "javac failed: " + compileOutput);

        runner = new InvocationRunner(new JsonValueCoercer());
        WorkerProcessClient client = new WorkerProcessClient("java", "missing-worker.jar");
        handle = WorkerSessionHandle.startCommand(client, 5, WorkerTestSupport.javaCommand());
    }

    @AfterEach
    void tearDown() {
        if (handle != null) {
            handle.close();
        }
    }

    @Test
    void methodInvocationUsesReceiverConstructor() {
        InvocationRubric rubric = methodWithReceiver("getSpeed", List.of(), "[]");

        InvocationOutcome outcome = runner.invokeSingle(context(), rubric, List.of("speed"));

        assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind());
        assertEquals(0, outcome.returnValue());
    }

    @Test
    void methodInvocationWithoutReceiverRequiresNoArgConstructor() {
        InvocationRubric rubric = new InvocationRubric(
                UUID.randomUUID(),
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Car",
                "getSpeed",
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null);

        InvocationOutcome outcome = runner.invokeSingle(context(), rubric, List.of());

        assertEquals(InvocationOutcomeKind.ERROR, outcome.kind());
        assertTrue(outcome.errorMessage().contains("no-argument constructor"));
    }

    @Test
    void voidMethodInvocationSucceedsWithReceiver() {
        InvocationRubric rubric = methodWithReceiver("accelerate", List.of(), "[]");

        InvocationOutcome outcome = runner.invokeSingle(context(), rubric, List.of("speed"));

        assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind());
        assertEquals(5, outcome.fieldSnapshots().get("speed"));
    }

    private ChallengeGradingContext context() {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "c1", List.of(), List.of(), List.of());
        return ChallengeGradingContext.of(rubric, classesDir, null, List.of(), Set.of(), Map.of(), handle);
    }

    private static InvocationRubric methodWithReceiver(String methodName,
                                                       List<String> parameterTypes,
                                                       String paramsJson) {
        return new InvocationRubric(
                UUID.randomUUID(),
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Car",
                methodName,
                parameterTypes,
                paramsJson,
                UUID.randomUUID(),
                "Car",
                List.of("int", "String"),
                "[2020, \"Toyota\"]");
    }
}
