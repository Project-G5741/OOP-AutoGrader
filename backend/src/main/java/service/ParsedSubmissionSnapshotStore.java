package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Persists parsed submission display snapshots for Class/MMD tabs outside the ephemeral upload folder.
 */
@Service
public class ParsedSubmissionSnapshotStore {

    private final Path storeDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedSubmissionSnapshotStore(@Value("${app.storage.submission-base-dir}") String baseDir) {
        this.storeDir = Path.of(baseDir, "_parsed_snapshot");
    }

    public void save(UUID submissionId, Map<UUID, ChallengeSnapshot> snapshotsByChallengeId) {
        if (submissionId == null) {
            return;
        }
        try {
            Files.createDirectories(storeDir);
            Path file = storeDir.resolve(submissionId + ".json");
            if (snapshotsByChallengeId == null || snapshotsByChallengeId.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            ParsedSubmissionSnapshot root = new ParsedSubmissionSnapshot();
            snapshotsByChallengeId.forEach((challengeId, snapshot) ->
                    root.challenges.put(challengeId.toString(), snapshot));
            objectMapper.writeValue(file.toFile(), root);
        } catch (IOException e) {
            System.out.printf("parsed_snapshot_store write failed submission=%s%n", submissionId);
        }
    }

    public ChallengeSnapshot get(UUID submissionId, UUID challengeId) {
        if (submissionId == null || challengeId == null) {
            return null;
        }
        ParsedSubmissionSnapshot root = readAll(submissionId);
        if (root == null || root.challenges == null) {
            return null;
        }
        return root.challenges.get(challengeId.toString());
    }

    public ParsedSubmissionSnapshot readAll(UUID submissionId) {
        if (submissionId == null) {
            return null;
        }
        Path file = storeDir.resolve(submissionId + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return objectMapper.readValue(file.toFile(), ParsedSubmissionSnapshot.class);
        } catch (IOException e) {
            return null;
        }
    }

    public Map<UUID, ChallengeSnapshot> readChallengeMap(UUID submissionId) {
        ParsedSubmissionSnapshot root = readAll(submissionId);
        if (root == null || root.challenges == null || root.challenges.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ChallengeSnapshot> parsed = new HashMap<>();
        for (Map.Entry<String, ChallengeSnapshot> entry : root.challenges.entrySet()) {
            parsed.put(UUID.fromString(entry.getKey()), entry.getValue());
        }
        return parsed;
    }
}
