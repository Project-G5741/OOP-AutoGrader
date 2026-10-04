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
 * Persists per-challenge package-normalization notices outside the ephemeral upload folder.
 */
@Service
public class SubmissionPackageNormalizationStore {

    private final Path storeDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionPackageNormalizationStore(@Value("${app.storage.submission-base-dir}") String baseDir) {
        this.storeDir = Path.of(baseDir, "_package_normalization");
    }

    public void save(UUID submissionId, Map<UUID, String> noticesByChallengeId) {
        if (submissionId == null) {
            return;
        }
        try {
            Files.createDirectories(storeDir);
            Path file = storeDir.resolve(submissionId + ".json");
            if (noticesByChallengeId == null || noticesByChallengeId.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            objectMapper.writeValue(file.toFile(), noticesByChallengeId);
        } catch (IOException e) {
            System.out.printf("package_normalization_store write failed submission=%s%n", submissionId);
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
