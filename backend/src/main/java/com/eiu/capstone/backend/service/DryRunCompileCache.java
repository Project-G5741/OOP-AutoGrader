package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

import jakarta.annotation.PreDestroy;

/**
 * Reuses a compiled classes dir across dry-runs when reference sources are unchanged.
 */
@Component
public class DryRunCompileCache {

    private static final long IDLE_TTL_MS = 60_000L;

    private final Object lock = new Object();
    private String cachedKey;
    private Path cachedClassesDir;
    private CompileOutcome cachedOutcome;
    private List<SourceEntry> cachedNormalizedSources;
    private long deadlineMs;

    public record Hit(Path classesDir, CompileOutcome outcome, List<SourceEntry> normalizedSources) {}

    public Hit getIfFresh(String key) {
        synchronized (lock) {
            if (cachedKey != null
                    && cachedKey.equals(key)
                    && cachedClassesDir != null
                    && Files.isDirectory(cachedClassesDir)
                    && System.currentTimeMillis() < deadlineMs) {
                deadlineMs = System.currentTimeMillis() + IDLE_TTL_MS;
                return new Hit(cachedClassesDir, cachedOutcome, cachedNormalizedSources);
            }
            return null;
        }
    }

    public void put(String key,
                    Path classesDir,
                    CompileOutcome outcome,
                    List<SourceEntry> normalizedSources) {
        synchronized (lock) {
            if (cachedClassesDir != null && !cachedClassesDir.equals(classesDir)) {
                deleteQuietly(cachedClassesDir.getParent());
            }
            cachedKey = key;
            cachedClassesDir = classesDir;
            cachedOutcome = outcome;
            cachedNormalizedSources = normalizedSources;
            deadlineMs = System.currentTimeMillis() + IDLE_TTL_MS;
        }
    }

    public static String fingerprint(List<SourceEntry> sources) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (SourceEntry entry : sources) {
                digest.update(entry.logicalPath().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.source().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return String.valueOf(sources.hashCode());
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (lock) {
            if (cachedClassesDir != null) {
                deleteQuietly(cachedClassesDir.getParent());
            }
            cachedKey = null;
            cachedClassesDir = null;
            cachedOutcome = null;
            cachedNormalizedSources = null;
        }
    }

    private static void deleteQuietly(Path root) {
        if (root == null) {
            return;
        }
        try {
            if (Files.isDirectory(root)) {
                try (var stream = Files.list(root)) {
                    for (Path child : stream.toList()) {
                        deleteQuietly(child);
                    }
                }
            }
            Files.deleteIfExists(root);
        } catch (Exception ignored) {
            // best effort
        }
    }
}
