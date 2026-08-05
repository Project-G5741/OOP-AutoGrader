package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Persists per-challenge MMD metadata (file presence, class presence in diagram, relation errors)
 * outside the ephemeral upload folder for the MMD read API.
 */
@Service
public class SubmissionMmdMetaStore {

    private final Path storeDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionMmdMetaStore(@Value("${app.storage.submission-base-dir}") String baseDir) {
        this.storeDir = Path.of(baseDir, "_mmd_meta");
    }

    public void save(UUID submissionId, Map<UUID, ChallengeMmdMeta> metaByChallengeId) {
        if (submissionId == null) {
            return;
        }
        try {
            Files.createDirectories(storeDir);
            Path file = storeDir.resolve(submissionId + ".json");
            if (metaByChallengeId == null || metaByChallengeId.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Map<String, ChallengeMmdMeta> serialized = new HashMap<>();
            metaByChallengeId.forEach((id, meta) -> serialized.put(id.toString(), meta));
            objectMapper.writeValue(file.toFile(), serialized);
        } catch (IOException e) {
            System.out.printf("mmd_meta_store write failed submission=%s%n", submissionId);
        }
    }

    public ChallengeMmdMeta get(UUID submissionId, UUID challengeId) {
        if (submissionId == null || challengeId == null) {
            return ChallengeMmdMeta.empty();
        }
        return readAll(submissionId).getOrDefault(challengeId, ChallengeMmdMeta.empty());
    }

    public Map<UUID, ChallengeMmdMeta> readAll(UUID submissionId) {
        if (submissionId == null) {
            return Map.of();
        }
        Path file = storeDir.resolve(submissionId + ".json");
        if (!Files.exists(file)) {
            return Map.of();
        }
        try {
            Map<String, ChallengeMmdMeta> raw = objectMapper.readValue(
                    file.toFile(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, ChallengeMmdMeta.class));
            Map<UUID, ChallengeMmdMeta> parsed = new HashMap<>();
            for (Map.Entry<String, ChallengeMmdMeta> entry : raw.entrySet()) {
                parsed.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
            return parsed;
        } catch (IOException e) {
            return Map.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChallengeMmdMeta {
        public boolean mmdSubmitted;
        public Map<String, Boolean> classStereotypeCorrect = Map.of();
        public Map<String, String> relationErrors = Map.of();

        public static ChallengeMmdMeta empty() {
            return new ChallengeMmdMeta();
        }
    }
}
