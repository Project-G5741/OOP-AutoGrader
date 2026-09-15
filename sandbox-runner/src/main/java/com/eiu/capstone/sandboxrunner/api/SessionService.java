package com.eiu.capstone.sandboxrunner.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.eiu.capstone.sandboxrunner.config.PoolProperties;
import com.eiu.capstone.sandboxrunner.config.SessionProperties;
import com.eiu.capstone.sandboxrunner.docker.DockerCommand;
import com.eiu.capstone.sandboxrunner.model.SessionRecord;
import com.eiu.capstone.sandboxrunner.pool.WarmPoolManager;
import com.eiu.capstone.sandboxrunner.util.TarGz;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    static final String CONTAINER_SUBMISSION_ROOT = "/work/submission";

    private final WarmPoolManager warmPoolManager;
    private final SessionProperties sessionProperties;
    private final PoolProperties poolProperties;
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    public SessionService(WarmPoolManager warmPoolManager,
                          SessionProperties sessionProperties,
                          PoolProperties poolProperties) {
        this.warmPoolManager = warmPoolManager;
        this.sessionProperties = sessionProperties;
        this.poolProperties = poolProperties;
    }

    public Map<String, String> createSession(InputStream classesTarGz) throws Exception {
        if (sessions.size() >= sessionProperties.getMaxSessions()) {
            throw new IllegalStateException("Sandbox session limit reached");
        }
        byte[] payload = classesTarGz.readAllBytes();
        if (payload.length > sessionProperties.getMaxTarballBytes()) {
            throw new IllegalArgumentException("Tarball exceeds max size");
        }
        if (payload.length == 0) {
            throw new IllegalArgumentException("Tarball is empty");
        }

        Path tempRoot = Files.createTempDirectory("sandbox-session-");
        Path submissionDir = tempRoot.resolve("submission");
        Files.createDirectories(submissionDir);
        try {
            TarGz.extractGzipTar(new java.io.ByteArrayInputStream(payload), submissionDir);

            String containerId = warmPoolManager.acquire(poolProperties.getScaleUpWaitMs());
            if (containerId == null) {
                throw new IllegalStateException("Sandbox pool exhausted");
            }

            try {
                DockerCommand.copyToContainer(submissionDir, containerId, CONTAINER_SUBMISSION_ROOT);
                Process worker = DockerCommand.startWorkerExec(containerId);
                String sessionId = UUID.randomUUID().toString();
                SessionRecord record = new SessionRecord(sessionId, containerId, worker);
                sessions.put(sessionId, record);
                return Map.of("sessionId", sessionId, "containerRoot", CONTAINER_SUBMISSION_ROOT);
            } catch (Exception e) {
                warmPoolManager.release(containerId, true);
                throw e;
            }
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    public String invoke(String sessionId, String requestLine, int timeoutSeconds) throws Exception {
        SessionRecord record = sessions.get(sessionId);
        if (record == null) {
            throw new IllegalArgumentException("Unknown session");
        }
        return record.invokeLine(requestLine, Duration.ofSeconds(Math.max(1, timeoutSeconds)));
    }

    public void deleteSession(String sessionId) {
        SessionRecord record = sessions.remove(sessionId);
        if (record != null) {
            try {
                record.close();
            } finally {
                warmPoolManager.release(record.containerId(), true);
            }
        }
    }

    @Scheduled(fixedDelayString = "${sandbox.session.sweep-interval-ms:60000}")
    void sweepExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds(sessionProperties.getTtlSeconds());
        List<String> expired = new ArrayList<>();
        sessions.forEach((id, record) -> {
            if (record.createdAt().isBefore(cutoff)) {
                expired.add(id);
            }
        });
        for (String sessionId : expired) {
            log.info("Sweeping expired sandbox session {}", sessionId);
            deleteSession(sessionId);
        }
    }

    @PreDestroy
    void shutdownAllSessions() {
        List<String> active = new ArrayList<>(sessions.keySet());
        for (String sessionId : active) {
            log.info("Closing sandbox session {} on runner shutdown", sessionId);
            deleteSession(sessionId);
        }
    }

    private static void deleteRecursively(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // best effort
                }
            });
        } catch (Exception ignored) {
            // best effort
        }
    }
}
