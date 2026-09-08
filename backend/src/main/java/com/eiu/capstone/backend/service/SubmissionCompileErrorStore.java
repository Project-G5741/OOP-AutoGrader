package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
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

    public void save(UUID submissionId, Map<UUID, ChallengeCompileErrors> errorsByChallengeId) {
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
            Map<String, ChallengeCompileErrors> payload = new HashMap<>();
            boolean any = false;
            for (Map.Entry<UUID, ChallengeCompileErrors> entry : errorsByChallengeId.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                payload.put(entry.getKey().toString(), entry.getValue());
                any = true;
            }
            if (!any) {
                Files.deleteIfExists(file);
                return;
            }
            objectMapper.writeValue(file.toFile(), payload);
        } catch (IOException e) {
            System.out.printf("compile_error_store write failed submission=%s%n", submissionId);
        }
    }

    public ChallengeCompileErrors get(UUID submissionId, UUID challengeId) {
        if (submissionId == null || challengeId == null) {
            return ChallengeCompileErrors.none();
        }
        ChallengeCompileErrors stored = readAll(submissionId).get(challengeId);
        return stored == null ? ChallengeCompileErrors.none() : stored;
    }

    public Map<UUID, ChallengeCompileErrors> readAll(UUID submissionId) {
        if (submissionId == null) {
            return Map.of();
        }
        Path file = storeDir.resolve(submissionId + ".json");
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            Map<UUID, ChallengeCompileErrors> parsed = new HashMap<>();
            var fields = root.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                parsed.put(UUID.fromString(entry.getKey()), parseValue(entry.getValue()));
            }
            return parsed;
        } catch (IOException | IllegalArgumentException e) {
            return Map.of();
        }
    }

    private ChallengeCompileErrors parseValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return ChallengeCompileErrors.none();
        }
        if (value.isTextual()) {
            return ChallengeCompileErrors.catastrophic(value.asText());
        }
        if (value.isObject()) {
            return objectMapper.convertValue(value, ChallengeCompileErrors.class);
        }
        return ChallengeCompileErrors.none();
    }
}
