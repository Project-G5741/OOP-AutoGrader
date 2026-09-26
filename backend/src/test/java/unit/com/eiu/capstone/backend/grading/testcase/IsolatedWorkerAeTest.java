package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

import support.com.eiu.capstone.backend.grading.testcase.WorkerTestSupport;

/**
 * Named AE1–AE9 coverage for the isolated worker. AE3/AE6 live in
 * {@code WorkerProcessClientTest}; AE7 lives in {@code WorkerInvokeEngineTest}.
 */
class IsolatedWorkerAeTest {

    @TempDir
    Path tempDir;

    @Test
    void ae1SystemExitInMethodDoesNotKillApiJvm() throws Exception {
        Path classesDir = compile(Map.of(
                "ExitBoom.java", """
                        public class ExitBoom {
                            public void boom() { System.exit(1); }
                        }
                        """,
                "Ok.java", """
                        public class Ok {
                            public int value() { return 7; }
                        }
                        """));
        try (WorkerSessionHandle handle = startWorker()) {
            InvocationRunner runner = new InvocationRunner(new JsonValueCoercer());
            InvocationOutcome first = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("ExitBoom", "boom"), List.of());
            assertEquals(InvocationOutcomeKind.ERROR, first.kind());
            InvocationOutcome later = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Ok", "value"), List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, later.kind(), later.errorMessage());
            assertEquals(7, later.returnValue());
        }
    }

    @Test
    void ae2ClassTabStaticExitDoesNotKillApiJvm() throws Exception {
        Path classesDir = compile("StaticExit.java", """
                public class StaticExit {
                    static { System.exit(1); }
                    public int value() { return 1; }
                }
                """);
        List<ParsedClass> parsed = new ReflectionClassParser().parseClasses(classesDir);
        assertEquals(1, parsed.size());
        assertEquals("StaticExit", parsed.get(0).simpleName);
    }

    @Test
    void ae4GradeSingleSystemExitIsPreviewError() throws Exception {
        Path classesDir = compile("ExitBoom.java", """
                public class ExitBoom {
                    public void boom() { System.exit(1); }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            JsonValueCoercer coercer = new JsonValueCoercer();
            TestcaseGrader grader = new TestcaseGrader(
                    new InvocationRunner(coercer),
                    new AssertionEvaluator(coercer),
                    new PrimaryAssertionSelector(),
                    new TestcaseDisplayFormatter(coercer));
            TestcaseGrader.PendingTestcaseResult result = grader.gradeSingle(
                    singleInvoke("ExitBoom", "boom"),
                    context(classesDir, handle));
            assertEquals(TestcaseResultStatus.ERROR, result.status());
        }
    }

    @Test
    void utf8ReturnValueAndStdoutRoundTripThroughIpc() throws Exception {
        Path classesDir = compile("Accent.java", """
                public class Accent {
                    public String greet() {
                        System.out.print("Xin ch\\u00e0o");
                        return "caf\\u00e9";
                    }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            InvocationRunner runner = new InvocationRunner(new JsonValueCoercer());
            InvocationOutcome outcome = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Accent", "greet"), List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind(), outcome.errorMessage());
            assertEquals("caf\u00e9", outcome.returnValue(), () -> codepoints(outcome.returnValue()));
            assertEquals("Xin ch\u00e0o", outcome.stdout(), () -> codepoints(outcome.stdout()));
        }
    }

    @Test
    void hangingMethodTimesOutThenLaterInvokeStillRuns() throws Exception {
        Path classesDir = compile(Map.of(
                "Hang.java", """
                        public class Hang {
                            public int spin() {
                                while (true) {
                                    Thread.onSpinWait();
                                }
                            }
                        }
                        """,
                "Ok.java", """
                        public class Ok {
                            public int value() { return 7; }
                        }
                        """));
        try (WorkerSessionHandle handle = startWorker(1)) {
            InvocationRunner runner = new InvocationRunner(new JsonValueCoercer());
            InvocationOutcome hung = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Hang", "spin"), List.of());
            assertEquals(InvocationOutcomeKind.TIMED_OUT, hung.kind(), hung.errorMessage());
            InvocationOutcome later = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Ok", "value"), List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, later.kind(), later.errorMessage());
            assertEquals(7, later.returnValue());
        }
    }

    @Test
    void ae5OrdinaryInvokeStillPasses() throws Exception {
        Path classesDir = compile("Ok.java", """
                public class Ok {
                    public int value() { return 7; }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            InvocationRunner runner = new InvocationRunner(new JsonValueCoercer());
            InvocationOutcome outcome = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Ok", "value"), List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind(), outcome.errorMessage());
            assertEquals(7, outcome.returnValue());
        }
    }

    @Test
    void ae8StdoutTruncationDoesNotFailOtherwiseCorrectInvoke() throws Exception {
        Path classesDir = compile("Flood.java", """
                public class Flood {
                    public void flood() {
                        for (int i = 0; i < 70000; i++) {
                            System.out.print('x');
                        }
                    }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            InvocationRunner runner = new InvocationRunner(new JsonValueCoercer());
            InvocationOutcome outcome = runner.invokeSingle(
                    context(classesDir, handle), methodInvoke("Flood", "flood"), List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, outcome.kind(), outcome.errorMessage());
            assertTrue(outcome.stdoutTruncated());
            assertEquals(WorkerIpc.DEFAULT_STDOUT_CAP, outcome.stdout().length());
        }
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.STDOUT,
                null,
                null,
                null,
                null,
                "\"x\"",
                ComparisonMode.EXACT,
                0);
        AssertionEvaluation evaluation = new AssertionEvaluation(
                assertion.id(), TestcaseResultStatus.PASSED, "\"xxxxx\"", "ok");
        String actual = new TestcaseDisplayFormatter(new JsonValueCoercer()).formatActual(
                assertion,
                evaluation,
                InvocationOutcome.normal(null, "xxxxx", true, Map.of()),
                null);
        assertTrue(actual.endsWith(" [truncated]"), actual);
    }

    @Test
    void ae1UnitDepositFieldStateOnHiddenReceiver() throws Exception {
        Path classesDir = compile("BankAccount.java", """
                public class BankAccount {
                    private int balance;
                    public BankAccount() { this.balance = 0; }
                    public void deposit(int amount) { this.balance += amount; }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            JsonValueCoercer coercer = new JsonValueCoercer();
            TestcaseGrader grader = new TestcaseGrader(
                    new InvocationRunner(coercer),
                    new AssertionEvaluator(coercer),
                    new PrimaryAssertionSelector(),
                    new TestcaseDisplayFormatter(coercer));
            UUID invocationId = UUID.randomUUID();
            InvocationRubric deposit = new InvocationRubric(
                    invocationId,
                    InvocationKind.METHOD,
                    null,
                    UUID.randomUUID(),
                    "BankAccount",
                    "deposit",
                    List.of("int"),
                    "[100]",
                    null,
                    null,
                    List.of(),
                    null);
            TestcaseRubric testcase = new TestcaseRubric(
                    UUID.randomUUID(),
                    "deposit",
                    TestcaseType.UNIT,
                    null,
                    1,
                    0,
                    false,
                    deposit,
                    List.of(),
                    List.of(new AssertionRubric(
                            UUID.randomUUID(),
                            AssertionKind.FIELD_STATE,
                            invocationId,
                            null,
                            "balance",
                            "int",
                            "100",
                            ComparisonMode.EXACT,
                            0)));
            TestcaseGrader.PendingTestcaseResult result = grader.gradeSingle(
                    testcase, context(classesDir, handle));
            assertEquals(TestcaseResultStatus.PASSED, result.status(), result.feedback());
        }
    }

    @Test
    void ae6UnacceptedMethodThrowFailsLaterAssertions() throws Exception {
        Path classesDir = compile("Actor.java", """
                public class Actor {
                    public void boom() { throw new IllegalStateException("boom"); }
                    public int ok() { return 7; }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            JsonValueCoercer coercer = new JsonValueCoercer();
            TestcaseGrader grader = new TestcaseGrader(
                    new InvocationRunner(coercer),
                    new AssertionEvaluator(coercer),
                    new PrimaryAssertionSelector(),
                    new TestcaseDisplayFormatter(coercer));
            UUID constructId = UUID.randomUUID();
            UUID boomId = UUID.randomUUID();
            UUID laterId = UUID.randomUUID();
            TestcaseRubric testcase = new TestcaseRubric(
                    UUID.randomUUID(),
                    "mid throw",
                    TestcaseType.COMPOSITION,
                    null,
                    1,
                    0,
                    false,
                    null,
                    List.of(),
                    List.of(new AssertionRubric(
                            UUID.randomUUID(),
                            AssertionKind.RETURN_VALUE,
                            laterId,
                            null,
                            null,
                            null,
                            "7",
                            ComparisonMode.EXACT,
                            0)),
                    List.of(
                            new InvocationRubric(
                                    constructId,
                                    InvocationKind.CONSTRUCTOR,
                                    UUID.randomUUID(),
                                    null,
                                    "Actor",
                                    null,
                                    List.of(),
                                    "[]",
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    "actor",
                                    null,
                                    null,
                                    null),
                            new InvocationRubric(
                                    boomId,
                                    InvocationKind.METHOD,
                                    null,
                                    UUID.randomUUID(),
                                    "Actor",
                                    "boom",
                                    List.of(),
                                    "[]",
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    "actor",
                                    null,
                                    null,
                                    null),
                            new InvocationRubric(
                                    laterId,
                                    InvocationKind.METHOD,
                                    null,
                                    UUID.randomUUID(),
                                    "Actor",
                                    "ok",
                                    List.of(),
                                    "[]",
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    "actor",
                                    null,
                                    null,
                                    null)),
                    null);
            TestcaseGrader.PendingTestcaseResult result = grader.gradeSingle(
                    testcase, context(classesDir, handle));
            assertEquals(TestcaseResultStatus.FAILED, result.status());
            assertEquals(TestcaseResultStatus.FAILED, result.assertions().get(0).status());
            assertTrue(result.assertions().get(0).feedback().toLowerCase().contains("not executed"),
                    result.assertions().get(0).feedback());
        }
    }

    @Test
    void ae7ExceptionStdoutAndFieldStateThroughWorker() throws Exception {
        Path classesDir = compile("Probe.java", """
                public class Probe {
                    private int flag = 1;
                    public void shout() {
                        System.out.print("hi");
                        throw new IllegalArgumentException("nope");
                    }
                }
                """);
        try (WorkerSessionHandle handle = startWorker()) {
            JsonValueCoercer coercer = new JsonValueCoercer();
            TestcaseGrader grader = new TestcaseGrader(
                    new InvocationRunner(coercer),
                    new AssertionEvaluator(coercer),
                    new PrimaryAssertionSelector(),
                    new TestcaseDisplayFormatter(coercer));
            UUID constructId = UUID.randomUUID();
            UUID shoutId = UUID.randomUUID();
            TestcaseRubric testcase = new TestcaseRubric(
                    UUID.randomUUID(),
                    "mixed",
                    TestcaseType.COMPOSITION,
                    null,
                    1,
                    0,
                    false,
                    null,
                    List.of(),
                    List.of(
                            new AssertionRubric(
                                    UUID.randomUUID(), AssertionKind.EXCEPTION, shoutId, null, null, null,
                                    "\"IllegalArgumentException\"", ComparisonMode.EXACT, 0),
                            new AssertionRubric(
                                    UUID.randomUUID(), AssertionKind.STDOUT, shoutId, null, null, null,
                                    "\"hi\"", ComparisonMode.EXACT, 1),
                            new AssertionRubric(
                                    UUID.randomUUID(), AssertionKind.FIELD_STATE, shoutId, null, "flag", "int",
                                    "1", ComparisonMode.EXACT, 2)),
                    List.of(
                            new InvocationRubric(
                                    constructId,
                                    InvocationKind.CONSTRUCTOR,
                                    UUID.randomUUID(),
                                    null,
                                    "Probe",
                                    null,
                                    List.of(),
                                    "[]",
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    "probe",
                                    null,
                                    null,
                                    null),
                            new InvocationRubric(
                                    shoutId,
                                    InvocationKind.METHOD,
                                    null,
                                    UUID.randomUUID(),
                                    "Probe",
                                    "shout",
                                    List.of(),
                                    "[]",
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    "probe",
                                    null,
                                    null,
                                    null)),
                    null);
            TestcaseGrader.PendingTestcaseResult result = grader.gradeSingle(
                    testcase, context(classesDir, handle));
            assertEquals(TestcaseResultStatus.PASSED, result.status(), result.feedback());
        }
    }

    private Path compile(String fileName, String source) throws Exception {
        return compile(Map.of(fileName, source));
    }

    private Path compile(Map<String, String> sources) throws Exception {
        Path classesDir = tempDir.resolve("classes-" + UUID.randomUUID());
        Path sourceDir = tempDir.resolve("src-" + UUID.randomUUID());
        Files.createDirectories(classesDir);
        Files.createDirectories(sourceDir);
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-d");
        command.add(classesDir.toString());
        for (Map.Entry<String, String> source : new LinkedHashMap<>(sources).entrySet()) {
            Path file = sourceDir.resolve(source.getKey());
            Files.writeString(file, source.getValue());
            command.add(file.toString());
        }
        Process compile = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        int exit = compile.waitFor();
        String out = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + out);
        return classesDir;
    }

    private static WorkerSessionHandle startWorker() {
        return startWorker(5);
    }

    private static WorkerSessionHandle startWorker(int timeoutSeconds) {
        return WorkerSessionHandle.startCommand(
                new WorkerProcessClient("java", java.nio.file.Path.of("missing-worker.jar")),
                timeoutSeconds,
                WorkerTestSupport.javaCommand());
    }

    private static ChallengeGradingContext context(Path classesDir, WorkerSessionHandle handle) {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "c1", List.of(), List.of(), List.of());
        return ChallengeGradingContext.of(rubric, classesDir, null, List.of(), Set.of(), Map.of(), handle);
    }

    private static TestcaseRubric singleInvoke(String className, String methodName) {
        InvocationRubric invocation = methodInvoke(className, methodName);
        return new TestcaseRubric(
                UUID.randomUUID(),
                "hostile",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                invocation,
                List.of(),
                List.of(new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        invocation.id(),
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
    }

    private static String codepoints(Object value) {
        if (!(value instanceof String text)) {
            return String.valueOf(value);
        }
        StringBuilder hex = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (hex.length() > 0) {
                hex.append(' ');
            }
            hex.append(Integer.toHexString(cp));
        });
        return hex.toString();
    }

    private static InvocationRubric methodInvoke(String className, String methodName) {
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
}
