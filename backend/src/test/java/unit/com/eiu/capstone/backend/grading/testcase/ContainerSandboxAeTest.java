package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcomeKind;
import com.eiu.capstone.backend.grading.testcase.InvocationRunner;
import com.eiu.capstone.backend.grading.testcase.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.RemoteWorkerSessionClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.model.InvocationKind;

/**
 * AE1–AE3 inside container sandbox when {@code SANDBOX_INTEGRATION=true} and runner env vars are set.
 */
@EnabledIfEnvironmentVariable(named = "SANDBOX_INTEGRATION", matches = "true")
class ContainerSandboxAeTest {

    @TempDir
    Path tempDir;

    private RemoteWorkerSessionClient remoteClient;
    private final int timeoutSeconds = 10;

    @BeforeEach
    void requireRunnerConfig() {
        String url = System.getenv("SANDBOX_RUNNER_URL");
        String token = System.getenv("SANDBOX_RUNNER_TOKEN");
        assumeTrue(url != null && !url.isBlank(), "SANDBOX_RUNNER_URL required");
        assumeTrue(token != null && !token.isBlank(), "SANDBOX_RUNNER_TOKEN required");
        remoteClient = new RemoteWorkerSessionClient(url, token);
    }

    @Test
    void ae1NetworkEgressFailsContained() throws Exception {
        Path root = compileRoot(Map.of(
                "NetBoom.java", """
                        public class NetBoom {
                            public void boom() throws Exception {
                                new java.net.URL("http://example.com").openConnection().connect();
                            }
                        }
                        """));
        try (WorkerSessionHandle handle = remoteClient.open(root, timeoutSeconds)) {
            InvocationOutcome outcome = new InvocationRunner(new JsonValueCoercer()).invokeSingle(
                    context(root.resolve("classes"), handle),
                    methodInvoke("NetBoom", "boom"),
                    List.of());
            assertEquals(InvocationOutcomeKind.ERROR, outcome.kind());
        }
    }

    @Test
    void ae2HostFileReadDoesNotLeakIntoReturn() throws Exception {
        Path root = compileRoot(Map.of(
                "SecretPeek.java", """
                        public class SecretPeek {
                            public String peek() throws Exception {
                                return java.nio.file.Files.readString(java.nio.file.Path.of("/etc/passwd"));
                            }
                        }
                        """));
        try (WorkerSessionHandle handle = remoteClient.open(root, timeoutSeconds)) {
            InvocationOutcome outcome = new InvocationRunner(new JsonValueCoercer()).invokeSingle(
                    context(root.resolve("classes"), handle),
                    methodInvoke("SecretPeek", "peek"),
                    List.of());
            assertEquals(InvocationOutcomeKind.ERROR, outcome.kind());
        }
    }

    @Test
    void ae3OomKillsSessionOnlySubsequentSessionWorks() throws Exception {
        Path root = compileRoot(Map.of(
                "OomBoom.java", """
                        public class OomBoom {
                            public void boom() {
                                java.util.List<byte[]> leak = new java.util.ArrayList<>();
                                while (true) {
                                    leak.add(new byte[1024 * 1024]);
                                }
                            }
                        }
                        """,
                "Ok.java", """
                        public class Ok {
                            public int value() { return 3; }
                        }
                        """));
        try (WorkerSessionHandle first = remoteClient.open(root, timeoutSeconds)) {
            InvocationOutcome oom = new InvocationRunner(new JsonValueCoercer()).invokeSingle(
                    context(root.resolve("classes"), first),
                    methodInvoke("OomBoom", "boom"),
                    List.of());
            assertEquals(InvocationOutcomeKind.ERROR, oom.kind());
        }
        try (WorkerSessionHandle second = remoteClient.open(root, timeoutSeconds)) {
            InvocationOutcome ok = new InvocationRunner(new JsonValueCoercer()).invokeSingle(
                    context(root.resolve("classes"), second),
                    methodInvoke("Ok", "value"),
                    List.of());
            assertEquals(InvocationOutcomeKind.NORMAL, ok.kind());
            assertEquals(3, ok.returnValue());
        }
    }

    private Path compileRoot(Map<String, String> sources) throws Exception {
        Path root = tempDir.resolve("submission-" + UUID.randomUUID());
        Path classesDir = root.resolve("classes");
        Path sourceDir = root.resolve("src");
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
        Process compile = new ProcessBuilder(command).redirectErrorStream(true).start();
        int exit = compile.waitFor();
        String out = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + out);
        return root;
    }

    private static ChallengeGradingContext context(Path classesDir, WorkerSessionHandle handle) {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "sandbox-ae", List.of(), List.of(), List.of());
        return ChallengeGradingContext.of(rubric, classesDir, null, List.of(), Set.of(), Map.of(), handle);
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
