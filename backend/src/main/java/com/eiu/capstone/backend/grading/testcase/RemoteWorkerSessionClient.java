package com.eiu.capstone.backend.grading.testcase;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import com.eiu.capstone.backend.grading.testcase.transport.HttpWorkerTransport;
import com.eiu.capstone.backend.grading.testcase.transport.WorkerTransportUrls;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RemoteWorkerSessionClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteWorkerSessionClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String runnerUrl;
    private final String runnerToken;

    public RemoteWorkerSessionClient(
            @Value("${app.grading.sandbox.runner-url:}") String runnerUrl,
            @Value("${app.grading.sandbox.runner-token:}") String runnerToken) {
        this.runnerUrl = runnerUrl == null ? "" : runnerUrl.trim();
        this.runnerToken = runnerToken == null ? "" : runnerToken.trim();
    }

    public WorkerSessionHandle open(Path rootToTar, int timeoutSeconds) {
        long started = System.currentTimeMillis();
        if (runnerUrl.isBlank() || runnerToken.isBlank()) {
            log.warn("Sandbox runner URL or token not configured");
            return WorkerSessionHandle.failedRemote(SandboxInfraErrors.STUDENT_MESSAGE,
                    System.currentTimeMillis() - started);
        }
        try {
            WorkerSessionHandle.RemoteSessionParts parts = openSessionParts(rootToTar);
            Supplier<WorkerSessionHandle.RemoteSessionParts> reopen = () -> openSessionParts(rootToTar);
            return WorkerSessionHandle.startTransport(
                    parts.transport(),
                    timeoutSeconds,
                    parts.classesDirMapper(),
                    System.currentTimeMillis() - started,
                    true,
                    reopen);
        } catch (Exception e) {
            log.warn("Sandbox session create failed", e);
            return WorkerSessionHandle.failedRemote(SandboxInfraErrors.STUDENT_MESSAGE,
                    System.currentTimeMillis() - started);
        }
    }

    private WorkerSessionHandle.RemoteSessionParts openSessionParts(Path rootToTar) {
        if (runnerUrl.isBlank() || runnerToken.isBlank()) {
            throw new WorkerSpawnException(SandboxInfraErrors.STUDENT_MESSAGE);
        }
        try {
            byte[] tarball = directoryToGzipTar(rootToTar.toAbsolutePath().normalize());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WorkerTransportUrls.stripTrailingSlash(runnerUrl) + "/sessions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + runnerToken)
                    .header("Content-Type", "multipart/form-data; boundary=sandbox")
                    .POST(multipartBody(tarball))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WorkerSpawnException(SandboxInfraErrors.STUDENT_MESSAGE);
            }
            SandboxSessionResponse body = WorkerIpc.mapper()
                    .readValue(response.body(), SandboxSessionResponse.class);
            if (body == null || body.sessionId() == null || body.sessionId().isBlank()
                    || body.containerRoot() == null || body.containerRoot().isBlank()) {
                throw new WorkerSpawnException(SandboxInfraErrors.STUDENT_MESSAGE);
            }
            Path normalizedRoot = rootToTar.toAbsolutePath().normalize();
            UnaryOperator<String> mapper = classesDir ->
                    SandboxClassesDirMapper.map(classesDir, normalizedRoot, body.containerRoot());
            String baseUrl = WorkerTransportUrls.stripTrailingSlash(runnerUrl);
            HttpWorkerTransport transport = new HttpWorkerTransport(
                    httpClient, baseUrl, body.sessionId(), runnerToken);
            return new WorkerSessionHandle.RemoteSessionParts(transport, mapper);
        } catch (WorkerSpawnException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkerSpawnException(SandboxInfraErrors.STUDENT_MESSAGE, e);
        }
    }

    private static HttpRequest.BodyPublisher multipartBody(byte[] tarball) {
        String boundary = "sandbox";
        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"classes\"; filename=\"classes.tar.gz\"\r\n"
                + "Content-Type: application/gzip\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return HttpRequest.BodyPublishers.ofByteArray(concat(header, tarball, footer));
    }

    private static byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] out = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        System.arraycopy(c, 0, out, a.length + b.length, c.length);
        return out;
    }

    private static byte[] directoryToGzipTar(Path root) throws java.io.IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String entryName = root.relativize(path).toString().replace('\\', '/');
                        TarArchiveEntry entry = new TarArchiveEntry(path, entryName);
                        tar.putArchiveEntry(entry);
                        Files.copy(path, tar);
                        tar.closeArchiveEntry();
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException("Failed to tar " + path, e);
                    }
                });
            }
            tar.finish();
        }
        return bytes.toByteArray();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SandboxSessionResponse(String sessionId, String containerRoot) {
    }
}

