package com.eiu.capstone.backend.grading.testcase;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.eiu.capstone.backend.grading.testcase.transport.ProcessWorkerTransport;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerProcessClient {

    public static final int STDERR_CAP_BYTES = WorkerIpc.DEFAULT_STDOUT_CAP;
    public static final int IPC_LINE_CAP_BYTES = WorkerIpc.MAX_LINE_BYTES;
    public static final String HEAP_FLAG = "-Xmx64m";
    public static final String METASPACE_FLAG = "-XX:MaxMetaspaceSize=48m";
    public static final String EXIT_ON_OOM_FLAG = "-XX:+ExitOnOutOfMemoryError";

    /** Docker image layout (see backend/Dockerfile). Tried when configured path is missing. */
    public static final String DOCKER_WORKER_JAR = "/app/worker.jar";
    /** Local Maven package output relative to backend/. Tried after the Docker path. */
    public static final String LOCAL_WORKER_JAR = "target/backend-1.0.0-worker.jar";

    private final String javaBinary;
    private final Path workerJar;

    @Autowired
    public WorkerProcessClient(
            @Value("${app.grading.worker-java:java}") String javaBinary,
            @Value("${app.grading.worker-jar:/app/worker.jar}") String workerJar) {
        this(javaBinary, resolveWorkerJar(workerJar));
    }

    /** Explicit resolved path (tests); skips configured/fallback resolution. */
    public WorkerProcessClient(String javaBinary, Path workerJar) {
        this.javaBinary = javaBinary;
        this.workerJar = workerJar;
    }

    /**
     * Prefer the configured path when it exists. Otherwise fall back to the Docker image
     * layout, then the local Maven artifact — so a Render env copied from local
     * {@code WORKER_JAR=target/...} still finds {@code /app/worker.jar}.
     */
    static Path resolveWorkerJar(String configured) {
        return resolveWorkerJar(configured, List.of(DOCKER_WORKER_JAR, LOCAL_WORKER_JAR));
    }

    /** Tests inject fallback candidates via this overload. */
    public static Path resolveWorkerJar(String configured, List<String> fallbacks) {
        Path configuredPath = Path.of(configured == null || configured.isBlank()
                ? DOCKER_WORKER_JAR
                : configured.trim());
        if (Files.isRegularFile(configuredPath)) {
            return configuredPath.toAbsolutePath().normalize();
        }
        if (fallbacks != null) {
            for (String candidate : fallbacks) {
                Path path = Path.of(candidate);
                if (Files.isRegularFile(path)) {
                    return path.toAbsolutePath().normalize();
                }
            }
        }
        return configuredPath;
    }

    public List<String> productionCommand(String... workerArgs) {
        if (!Files.isRegularFile(workerJar)) {
            throw new WorkerSpawnException("Missing worker jar: " + workerJar.toAbsolutePath());
        }
        List<String> command = new ArrayList<>();
        command.add(javaBinary);
        command.add(HEAP_FLAG);
        command.add(METASPACE_FLAG);
        command.add(EXIT_ON_OOM_FLAG);
        command.add("-jar");
        command.add(workerJar.toAbsolutePath().toString());
        if (workerArgs != null) {
            command.addAll(List.of(workerArgs));
        }
        return command;
    }

    public WorkerSession start(String... workerArgs) {
        return startCommand(productionCommand(workerArgs));
    }

    public WorkerSession startCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new WorkerSpawnException("Worker command is empty");
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        WorkerEnvironment.apply(builder);
        builder.redirectErrorStream(false);
        Process process;
        try {
            process = builder.start();
        } catch (Exception e) {
            throw new WorkerSpawnException("Failed to start worker process", e);
        }
        AtomicInteger stderrKept = new AtomicInteger();
        Thread drain = new Thread(() -> drainStderr(process, stderrKept), "worker-stderr-drain");
        drain.setDaemon(true);
        drain.start();
        return new WorkerSession(new ProcessWorkerTransport(process, drain, stderrKept));
    }

    public WorkerSession killAndRespawnCommand(WorkerSession session, List<String> command) {
        if (session != null) {
            session.close();
        }
        WorkerSession next = startCommand(command);
        next.incrementRespawnCount();
        if (session != null) {
            for (int i = 0; i < session.respawnCount(); i++) {
                next.incrementRespawnCount();
            }
        }
        return next;
    }

    public boolean waitThenKillOnTimeout(WorkerSession session, Duration timeout) {
        if (session == null) {
            return false;
        }
        try {
            boolean exited = session.process().waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (exited) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ProcessTreeKiller.kill(session.process());
        return true;
    }

    private static void drainStderr(Process process, AtomicInteger stderrKept) {
        byte[] buffer = new byte[4096];
        try (InputStream err = process.getErrorStream()) {
            int read;
            while ((read = err.read(buffer)) != -1) {
                int already = stderrKept.get();
                int room = Math.max(0, STDERR_CAP_BYTES - already);
                if (room > 0) {
                    stderrKept.addAndGet(Math.min(room, read));
                }
            }
        } catch (Exception ignored) {
            // process closed
        }
    }
}
