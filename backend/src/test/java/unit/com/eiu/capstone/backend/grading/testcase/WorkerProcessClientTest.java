package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.eiu.capstone.backend.grading.testcase.ProcessTreeKiller;
import com.eiu.capstone.backend.grading.testcase.WorkerEnvironment;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSession;
import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;

import support.com.eiu.capstone.backend.grading.testcase.WorkerTestSupport;

class WorkerProcessClientTest {

    private final WorkerProcessClient client = new WorkerProcessClient("java", "missing-worker.jar");

    @Test
    void missingWorkerJarIsSpawnFailure() {
        WorkerSpawnException ex = assertThrows(WorkerSpawnException.class, client::start);
        assertTrue(ex.getMessage().contains("Missing worker jar"));
    }

    @Test
    void productionCommandContainsHeapFlags() {
        Path jar = Path.of("target/backend-1.0.0-worker.jar");
        WorkerProcessClient packaged = new WorkerProcessClient("java", jar.toString());
        if (!jar.toFile().isFile()) {
            WorkerSpawnException ex = assertThrows(WorkerSpawnException.class, packaged::productionCommand);
            assertTrue(ex.getMessage().contains("Missing worker jar"));
            return;
        }
        List<String> command = packaged.productionCommand();
        assertTrue(command.contains(WorkerProcessClient.HEAP_FLAG));
        assertTrue(command.contains(WorkerProcessClient.METASPACE_FLAG));
        assertTrue(command.contains(WorkerProcessClient.EXIT_ON_OOM_FLAG));
        assertTrue(command.contains("-jar"));
    }

    @Test
    void environmentAllowlistDropsApiSecrets() {
        ProcessBuilder builder = new ProcessBuilder("java");
        builder.environment().put("JWT_SECRET", "dummy-secret");
        builder.environment().put("DB_PASSWORD", "dummy-db");
        WorkerEnvironment.apply(builder);
        assertFalse(builder.environment().containsKey("JWT_SECRET"));
        assertFalse(builder.environment().containsKey("DB_PASSWORD"));
        assertTrue(WorkerEnvironment.allowed("PATH") || builder.environment().keySet().stream()
                .anyMatch(WorkerEnvironment::allowed));
    }

    @Test
    void ae6WorkerDoesNotInheritNamedSecrets() throws Exception {
        List<String> command = WorkerTestSupport.javaCommand("--dump-env", "JWT_SECRET", "DB_PASSWORD");
        try (WorkerSession session = client.startCommand(command)) {
            String line = session.readLine(Duration.ofSeconds(15), WorkerProcessClient.IPC_LINE_CAP_BYTES);
            assertTrue(line.contains("\"JWT_SECRET\":false"), line);
            assertTrue(line.contains("\"DB_PASSWORD\":false"), line);
        }
    }

    @Test
    void stderrFloodIsCapped() throws Exception {
        List<String> command = WorkerTestSupport.javaCommand("--stderr-flood");
        try (WorkerSession session = client.startCommand(command)) {
            long deadline = System.currentTimeMillis() + 10_000;
            while (session.stderrBytesKept() < WorkerProcessClient.STDERR_CAP_BYTES
                    && session.isAlive()
                    && System.currentTimeMillis() < deadline) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            assertTrue(session.stderrBytesKept() <= WorkerProcessClient.STDERR_CAP_BYTES);
            assertEquals(WorkerProcessClient.STDERR_CAP_BYTES, session.stderrBytesKept());
        }
    }

    @Test
    void hangTimeoutKillsWorkerAndLaterCommandStillRuns() throws Exception {
        Semaphore slot = new Semaphore(1);
        assertTrue(slot.tryAcquire());
        WorkerSession hung = client.startCommand(WorkerTestSupport.javaCommand("--hang"));
        try {
            assertTrue(hung.isAlive());
            assertFalse(slot.tryAcquire());
            assertTrue(client.waitThenKillOnTimeout(hung, Duration.ofSeconds(2)));
            assertTrue(slot.availablePermits() == 0, "slot stays held across kill");
            WorkerSession next = client.killAndRespawnCommand(hung, WorkerTestSupport.javaCommand("--self-check"));
            try {
                String line = next.readLine(Duration.ofSeconds(15), WorkerProcessClient.IPC_LINE_CAP_BYTES);
                assertEquals("{\"ok\":true}", line);
                assertTrue(next.respawnCount() >= 1);
            } finally {
                next.close();
            }
        } finally {
            hung.close();
            slot.release();
        }
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void ae3TimeoutKillsChildOsProcess() throws Exception {
        WorkerSession session = client.startCommand(WorkerTestSupport.javaCommand("--child-hang"));
        try {
            String line = session.readLine(Duration.ofSeconds(15), WorkerProcessClient.IPC_LINE_CAP_BYTES);
            assertTrue(line.contains("childPid"), line);
            long childPid = Long.parseLong(line.replaceAll("\\D+", ""));
            assertTrue(client.waitThenKillOnTimeout(session, Duration.ofSeconds(2)));
            TimeUnit.MILLISECONDS.sleep(200);
            Optional<ProcessHandle> child = ProcessHandle.of(childPid);
            assertTrue(child.isEmpty() || !child.get().isAlive(), "child OS process still alive");
        } finally {
            session.close();
        }
    }
}

class ProcessTreeKillerTest {

    @Test
    void killReturnsPromptlyWhenProcessAlreadyExited() throws Exception {
        Process process = new ProcessBuilder(
                ProcessHandle.current().info().command().orElse("java"), "-version")
                .redirectErrorStream(true)
                .start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        long started = System.nanoTime();
        ProcessTreeKiller.kill(process);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(elapsedMs < 400, "kill waited " + elapsedMs + "ms on an exited process");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void killsSpawnedSleepProcess() throws Exception {
        Process sleep = new ProcessBuilder("sleep", "60").start();
        assertTrue(sleep.isAlive());
        ProcessTreeKiller.kill(sleep);
        assertFalse(sleep.isAlive());
    }
}
