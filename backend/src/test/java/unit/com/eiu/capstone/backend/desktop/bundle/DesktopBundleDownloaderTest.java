package com.eiu.capstone.backend.desktop.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

class DesktopBundleDownloaderTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadIfConfigured_writesValidZip() throws Exception {
        byte[] body = minimalZipPayload(64);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/practice.zip", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/practice.zip";
            Path dest = tempDir.resolve(DesktopBundlePaths.PRACTICE_FILENAME);
            DesktopBundleDownloader downloader = new DesktopBundleDownloader(
                    32, Duration.ofSeconds(5), Duration.ofSeconds(30));
            downloader.downloadIfConfigured(url, dest);
            assertTrue(Files.isRegularFile(dest));
            assertEquals(body.length, Files.size(dest));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadIfConfigured_skipsBlankUrl() throws IOException {
        Path dest = tempDir.resolve("out.zip");
        DesktopBundleDownloader downloader = new DesktopBundleDownloader(
                1, Duration.ofSeconds(1), Duration.ofSeconds(5));
        downloader.downloadIfConfigured("  ", dest);
        assertTrue(Files.notExists(dest));
    }

    @Test
    void validateZip_rejectsNonZip() throws IOException {
        Path bad = tempDir.resolve("bad.bin");
        Files.write(bad, new byte[] {'X', 'Y', 1, 2, 3});
        DesktopBundleDownloader downloader = new DesktopBundleDownloader(
                1, Duration.ofSeconds(1), Duration.ofSeconds(5));
        assertThrows(IOException.class, () -> downloader.validateZip(bad));
    }

    private static byte[] minimalZipPayload(int size) {
        byte[] data = new byte[size];
        data[0] = 'P';
        data[1] = 'K';
        return data;
    }
}