package com.eiu.capstone.backend.desktop;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotCodeBoundView;
import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("desktop")
public class DesktopMascotSnapshotService {

    private final Path snapshotFile;
    private final Path cloudApiUrlFile;
    private final String configuredCloudApiUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DesktopMascotSnapshotService(
            @Value("${app.desktop.install-dir:.}") String installDir,
            @Value("${app.desktop.cloud-api-url:}") String cloudApiUrl,
            ObjectMapper objectMapper) {
        Path home = Path.of(installDir);
        this.snapshotFile = home.resolve("data").resolve("mascot-snapshot.json");
        this.cloudApiUrlFile = home.resolve("cloud-api-url.txt");
        this.configuredCloudApiUrl = cloudApiUrl == null ? "" : cloudApiUrl.trim();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public MascotSnapshotView read() {
        if (!Files.isRegularFile(snapshotFile)) {
            return new MascotSnapshotView("fox", List.of());
        }
        try {
            MascotSnapshotView view = objectMapper.readValue(snapshotFile.toFile(), MascotSnapshotView.class);
            return revertIfCodeUnbound(view);
        } catch (IOException ex) {
            return new MascotSnapshotView("fox", List.of());
        }
    }

    public MascotSnapshotView sync(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Practice token is required");
        }
        String base = resolveCloudApiUrl();
        if (base.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud API URL is not configured");
        }
        URI uri;
        try {
            uri = URI.create(base + "/api/students/mascot-snapshot");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cloud API URL is invalid");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
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

    private MascotSnapshotView revertIfCodeUnbound(MascotSnapshotView view) {
        if (view == null || view.jti() == null || view.jti().isBlank()) {
            return view == null ? new MascotSnapshotView("fox", List.of()) : view;
        }
        String base = resolveCloudApiUrl();
        if (base.isEmpty()) {
            return view;
        }
        try {
            URI uri = URI.create(base + "/api/students/mascot-code/" + view.jti().trim() + "/bound");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return view;
            }
            MascotCodeBoundView bound = objectMapper.readValue(response.body(), MascotCodeBoundView.class);
            if (bound != null && bound.bound()) {
                return view;
            }
            MascotSnapshotView fox = new MascotSnapshotView("fox", List.of());
            Files.createDirectories(snapshotFile.getParent());
            objectMapper.writeValue(snapshotFile.toFile(), fox);
            return fox;
        } catch (IOException | InterruptedException | IllegalArgumentException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return view;
        }
    }

    /** Live file wins over the URL baked at assemble / process start. */
    public String resolveCloudApiUrl() {
        if (Files.isRegularFile(cloudApiUrlFile)) {
            try {
                String fromFile = Files.readString(cloudApiUrlFile, StandardCharsets.UTF_8)
                        .replace("\uFEFF", "")
                        .trim();
                if (!fromFile.isEmpty()) {
                    return stripTrailingSlash(fromFile);
                }
            } catch (IOException ignored) {
                // fall through to the startup value
            }
        }
        return stripTrailingSlash(configuredCloudApiUrl);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
