package com.eiu.capstone.backend.desktop.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads public prebuilt desktop zips into the deploy filename next to {@code app.jar}.
 */
public class DesktopBundleDownloader {

    private static final Logger log = LoggerFactory.getLogger(DesktopBundleDownloader.class);

    private final long minBytes;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final HttpClient httpClient;

    public DesktopBundleDownloader(long minBytes, Duration connectTimeout, Duration readTimeout) {
        this.minBytes = minBytes;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

  /** For tests: inject client (e.g. test-only redirect rules). */
    DesktopBundleDownloader(
            long minBytes,
            Duration connectTimeout,
            Duration readTimeout,
            HttpClient httpClient) {
        this.minBytes = minBytes;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.httpClient = httpClient;
    }

    /**
     * When {@code url} is non-blank, download to {@code deployTarget} (atomic replace).
     * Skips when the URL is blank.
     */
    public void downloadIfConfigured(String url, Path deployTarget) throws IOException {
        if (url == null || url.isBlank()) {
            return;
        }
        Path dest = deployTarget.toAbsolutePath().normalize();
        Path parent = dest.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path part = parent != null
                ? parent.resolve(dest.getFileName().toString() + ".part")
                : Path.of(dest.getFileName() + ".part").toAbsolutePath().normalize();

        log.info("Prefetching desktop bundle from {} into {}", url, dest);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .timeout(readTimeout)
                .GET()
                .build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + url, e);
        }
        int status = response.statusCode();
        if (status < 200 || status > 299) {
            response.body().close();
            throw new IOException("HTTP " + status + " while downloading " + url);
        }
        try (InputStream body = response.body()) {
            Files.copy(body, part, StandardCopyOption.REPLACE_EXISTING);
        }
        validateZip(part);
        try {
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Desktop bundle ready at {} ({} bytes)", dest, Files.size(dest));
    }

    void validateZip(Path file) throws IOException {
        long size = Files.size(file);
        if (size <= minBytes) {
            throw new IOException("Downloaded file is too small (" + size + " bytes; minimum " + minBytes + ")");
        }
        try (InputStream in = Files.newInputStream(file)) {
            int b0 = in.read();
            int b1 = in.read();
            if (b0 != 'P' || b1 != 'K') {
                throw new IOException("Downloaded file is not a zip (missing PK header)");
            }
        }
    }
}