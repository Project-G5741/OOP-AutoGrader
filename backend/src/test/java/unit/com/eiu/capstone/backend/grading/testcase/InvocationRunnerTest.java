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
        WorkerProcessClient client = new WorkerProcessClient("java", java.nio.file.Path.of("missing-worker.jar"));
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
    void methodInvocationWithoutReceiverUsesHiddenDefaultConstructor() {
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

        assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind());
        assertEquals(0, outcome.returnValue());
    }

    @Test
    void voidMethodInvocationSucceedsWithReceiver() {
        InvocationRubric rubric = methodWithReceiver("accelerate", List.of(), "[]");

        InvocationOutcome outcome = runner.invokeSingle(context(), rubric, List.of("speed"));

        assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind());
        assertEquals(5, outcome.fieldSnapshots().get("speed"));
    }

    @Test
    void invokeScenarioMapsWorkerStepsAndOmitsLaterAfterConstructThrow() throws Exception {
        Path sourceDir = tempDir.resolve("boom-src");
        Path boomClasses = tempDir.resolve("boom-classes");
        Files.createDirectories(sourceDir);
        Files.createDirectories(boomClasses);
        Files.writeString(sourceDir.resolve("Boom.java"), """
                public class Boom {
                    public Boom() { throw new IllegalStateException("boom"); }
                    public int ok() { return 7; }
                }
                """);
        Process compile = new ProcessBuilder(
                "javac", "-d", boomClasses.toString(), sourceDir.resolve("Boom.java").toString())
                .redirectErrorStream(true)
                .start();
        int exit = compile.waitFor();
        String compileOutput = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + compileOutput);

        List<InvocationOutcome> outcomes = runner.invokeScenario(
                contextAt(boomClasses),
                List.of(
                        noArgConstructor("Boom"),
                        noArgMethod("Boom", "ok")),
                List.of());

        assertEquals(1, outcomes.size());
        assertEquals(InvocationOutcomeKind.THREW, outcomes.get(0).kind());
        assertEquals("IllegalStateException", outcomes.get(0).exceptionSimpleName());
    }

    @Test
    void invokeScenarioOneStepWithoutDispatchStillWorks() {
        InvocationRubric rubric = methodWithReceiver("accelerate", List.of(), "[]");
        List<InvocationOutcome> outcomes = runner.invokeScenario(context(), List.of(rubric), List.of("speed"));
        assertEquals(1, outcomes.size());
        assertEquals(InvocationOutcomeKind.NORMAL, outcomes.get(0).kind());
        assertEquals(5, outcomes.get(0).fieldSnapshots().get("speed"));
    }

    private ChallengeGradingContext context() {
        return contextAt(classesDir);
    }

    private ChallengeGradingContext contextAt(Path dir) {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "c1", List.of(), List.of(), List.of());
        return ChallengeGradingContext.of(rubric, dir, null, List.of(), Set.of(), Map.of(), handle);
    }

    private static InvocationRubric noArgConstructor(String className) {
        return new InvocationRubric(
                UUID.randomUUID(),
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                className,
                null,
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null);
    }

    private static InvocationRubric noArgMethod(String className, String methodName) {
        return new InvocationRubric(
                UUID.randomUUID(),
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                className,
                methodName,
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null);
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
