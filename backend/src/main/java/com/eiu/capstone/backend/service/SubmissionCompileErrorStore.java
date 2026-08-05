package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Persists per-challenge compile diagnostics outside the ephemeral upload folder
 * so the Class tab can show them after temp sources are deleted.
 */
@Service
public class SubmissionCompileErrorStore {

    private final Path storeDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionCompileErrorStore(@Value("${app.storage.submission-base-dir}") String baseDir) {
        this.storeDir = Path.of(baseDir, "_compile_errors");
    }

    public void save(UUID submissionId, Map<UUID, String> errorsByChallengeId) {
        if (submissionId == null) {
            return;
        }
        try {
            Files.createDirectories(storeDir);
            Path file = storeDir.resolve(submissionId + ".json");
            if (errorsByChallengeId == null || errorsByChallengeId.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            objectMapper.writeValue(file.toFile(), errorsByChallengeId);
        } catch (IOException e) {
            System.out.printf("compile_error_store write failed submission=%s%n", submissionId);
        }
    }

    public String get(UUID submissionId, UUID challengeId) {
        if (submissionId == null || challengeId == null) {
            return null;
        }
        return readAll(submissionId).get(challengeId);
    }

    public Map<UUID, String> readAll(UUID submissionId) {
        if (submissionId == null) {
            return Map.of();
        }
        Path file = storeDir.resolve(submissionId + ".json");
        if (!Files.exists(file)) {
            return Map.of();
        }
        try {
            Map<String, String> raw = objectMapper.readValue(
                    file.toFile(),
                    new TypeReference<Map<String, String>>() {});
            Map<UUID, String> parsed = new HashMap<>();
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                parsed.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
            return parsed;
        } catch (IOException e) {
            return Map.of();
        }
    }
}
