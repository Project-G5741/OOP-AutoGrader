package com.eiu.capstone.backend.grading.testcase.transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;
import com.eiu.capstone.backend.grading.testcase.WorkerTimeoutException;

public final class HttpWorkerTransport implements WorkerTransport {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String sessionId;
    private final String bearerToken;
    private final Object invokeMutex = new Object();
    private String pendingWrite;
    private volatile boolean closed;

    public HttpWorkerTransport(HttpClient httpClient,
                               String baseUrl,
                               String sessionId,
                               String bearerToken) {
        this.httpClient = httpClient;
        this.baseUrl = WorkerTransportUrls.stripTrailingSlash(baseUrl);
        this.sessionId = sessionId;
        this.bearerToken = bearerToken;
    }

    @Override
    public void writeLine(String line) {
        synchronized (invokeMutex) {
            if (closed) {
                throw new WorkerSpawnException("Remote worker session closed");
            }
            pendingWrite = line;
        }
    }

    @Override
    public String readLine(Duration timeout, int byteCap) {
        synchronized (invokeMutex) {
            if (closed) {
                throw new WorkerSpawnException("Remote worker session closed");
            }
            if (pendingWrite == null) {
                throw new WorkerSpawnException("No pending IPC write");
            }
            try {
                int timeoutSeconds = (int) Math.max(1, timeout.toSeconds());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/sessions/" + sessionId + "/invoke?timeoutSeconds=" + timeoutSeconds))
                        .timeout(timeout)
                        .header("Authorization", "Bearer " + bearerToken)
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString(pendingWrite))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                pendingWrite = null;
                int status = response.statusCode();
                if (status == 504) {
                    throw new WorkerTimeoutException("Remote worker invoke timed out");
                }
                if (status < 200 || status >= 300) {
                    throw new WorkerSpawnException("Remote worker invoke failed with status " + status);
                }
                String body = response.body();
                if (body == null || body.isBlank()) {
                    return null;
                }
                if (body.getBytes(StandardCharsets.UTF_8).length > byteCap) {
                    throw new WorkerSpawnException("Remote worker IPC line exceeded " + byteCap + " bytes");
                }
                return body;
            } catch (WorkerTimeoutException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WorkerSpawnException("Interrupted remote worker invoke", e);
            } catch (Exception e) {
                throw new WorkerSpawnException("Remote worker invoke failed", e);
            }
        }
    }

    @Override
    public boolean isAlive() {
        return !closed;
    }

    @Override
    public void closeTransport() {
        synchronized (invokeMutex) {
            closed = true;
            pendingWrite = null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/sessions/" + sessionId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + bearerToken)
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // best effort
        }
    }
}
