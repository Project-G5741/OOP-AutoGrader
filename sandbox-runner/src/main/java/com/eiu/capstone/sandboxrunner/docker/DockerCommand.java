package com.eiu.capstone.sandboxrunner.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DockerCommand {

    private DockerCommand() {}

    public static void copyToContainer(Path hostSource, String containerId, String containerPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("cp");
        command.add(hostSource.toString() + "/.");
        command.add(containerId + ":" + containerPath);
        run(command, 120);
    }

    public static Process startWorkerExec(String containerId) throws IOException {
        List<String> command = List.of(
                "docker", "exec", "-i", containerId,
                "java",
                "-Xmx64m",
                "-XX:MaxMetaspaceSize=48m",
                "-XX:+ExitOnOutOfMemoryError",
                "-jar", "/opt/worker/worker.jar");
        return new ProcessBuilder(command).start();
    }

    private static void run(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        Thread drain = new Thread(() -> drainQuietly(process.getInputStream()), "docker-cmd-drain");
        drain.setDaemon(true);
        drain.start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("docker command timed out: " + command);
        }
        drain.join(1000);
        if (process.exitValue() != 0) {
            throw new IOException("docker command failed (" + process.exitValue() + "): " + command);
        }
    }

    private static void drainQuietly(InputStream stream) {
        byte[] buffer = new byte[4096];
        try {
            while (stream.read(buffer) != -1) {
                // discard
            }
        } catch (IOException ignored) {
            // process closed
        }
    }
}
