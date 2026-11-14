package com.eiu.capstone.backend.desktop;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("desktop")
public class DesktopMascotSnapshotService {

    private final Path snapshotFile;
    private final String cloudApiUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DesktopMascotSnapshotService(
            @Value("${app.desktop.install-dir:.}") String installDir,
            @Value("${app.desktop.cloud-api-url:}") String cloudApiUrl,
            ObjectMapper objectMapper) {
        this.snapshotFile = Path.of(installDir).resolve("data").resolve("mascot-snapshot.json");
        this.cloudApiUrl = cloudApiUrl == null ? "" : cloudApiUrl.trim();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public MascotSnapshotView read() {
        if (!Files.isRegularFile(snapshotFile)) {
            return new MascotSnapshotView("fox", List.of());
        }
        try {
            return objectMapper.readValue(snapshotFile.toFile(), MascotSnapshotView.class);
        } catch (IOException ex) {
            return new MascotSnapshotView("fox", List.of());
        }
    }

    public MascotSnapshotView sync(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Practice token is required");
        }
        if (cloudApiUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud API URL is not configured");
        }
        String base = cloudApiUrl.endsWith("/") ? cloudApiUrl.substring(0, cloudApiUrl.length() - 1) : cloudApiUrl;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/students/mascot-snapshot"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token.trim())
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach the web mascot service");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not pull mascot from the web");
        }
        try {
            MascotSnapshotView view = objectMapper.readValue(response.body(), MascotSnapshotView.class);
            Files.createDirectories(snapshotFile.getParent());
            objectMapper.writeValue(snapshotFile.toFile(), view);
            return view;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save mascot snapshot");
        }
    }
}
