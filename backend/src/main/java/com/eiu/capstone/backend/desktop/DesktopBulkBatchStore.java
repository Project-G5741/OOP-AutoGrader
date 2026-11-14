package com.eiu.capstone.backend.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Persists the last finished offline Bulk Grading batch under the lecturer install home.
 */
@Service
@Profile("desktop-lecturer")
public class DesktopBulkBatchStore {

    private static final String FILE_NAME = "bulk-last-batch.json";

    private final Path batchFile;
    private final ObjectMapper objectMapper;

    public DesktopBulkBatchStore(
            @Value("${app.desktop.install-dir}") String installDir,
            ObjectMapper objectMapper) {
        this.batchFile = Path.of(installDir, "data", FILE_NAME);
        this.objectMapper = objectMapper;
    }

    public JsonNode load() {
        if (!Files.isRegularFile(batchFile)) {
            return null;
        }
        try {
            return objectMapper.readTree(Files.readString(batchFile, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read retained bulk batch",
                    ex);
        }
    }

    public void save(JsonNode batch) {
        if (batch == null || batch.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batch body is required");
        }
        try {
            Files.createDirectories(batchFile.getParent());
            Path temp = batchFile.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temp, objectMapper.writeValueAsString(batch), StandardCharsets.UTF_8);
            try {
                Files.move(temp, batchFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, batchFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save retained bulk batch",
                    ex);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(batchFile);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not clear retained bulk batch",
                    ex);
        }
    }

    public boolean hasBatch() {
        return Files.isRegularFile(batchFile);
    }
}
