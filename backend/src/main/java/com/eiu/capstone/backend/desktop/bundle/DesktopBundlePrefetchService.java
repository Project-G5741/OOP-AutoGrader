package com.eiu.capstone.backend.desktop.bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Downloads configured public Release URLs into deploy paths ({@code OOP-AutoGrader-*.zip}).
 * Used on startup and via lecturer admin refresh.
 */
@Service
@Profile("!desktop & !desktop-lecturer")
public class DesktopBundlePrefetchService {

    private static final Logger log = LoggerFactory.getLogger(DesktopBundlePrefetchService.class);

    private final DesktopBundleDownloader downloader;
    private final String practiceUrl;
    private final String lecturerBulkUrl;

    @Autowired
    public DesktopBundlePrefetchService(
            @Value("${app.desktop.bundle.min-bytes:1000000}") long minBytes,
            @Value("${app.desktop.bundle.connect-timeout-seconds:30}") int connectTimeoutSeconds,
            @Value("${app.desktop.bundle.read-timeout-seconds:600}") int readTimeoutSeconds,
            @Value("${PRACTICE_BUNDLE_URL:}") String practiceUrl,
            @Value("${LECTURER_BULK_BUNDLE_URL:}") String lecturerBulkUrl) {
        this.downloader = new DesktopBundleDownloader(
                minBytes,
                Duration.ofSeconds(connectTimeoutSeconds),
                Duration.ofSeconds(readTimeoutSeconds));
        this.practiceUrl = practiceUrl;
        this.lecturerBulkUrl = lecturerBulkUrl;
    }

    public DesktopBundleRefreshReport refreshAll() {
        List<DesktopBundleChannelResult> channels = new ArrayList<>();
        channels.add(prefetchOne("practice", practiceUrl, DesktopBundlePaths.PRACTICE_DEPLOY));
        channels.add(prefetchOne("lecturerBulk", lecturerBulkUrl, DesktopBundlePaths.LECTURER_BULK_DEPLOY));
        return new DesktopBundleRefreshReport(channels);
    }

    private DesktopBundleChannelResult prefetchOne(String channel, String url, Path deployTarget) {
        if (url == null || url.isBlank()) {
            return new DesktopBundleChannelResult(channel, ChannelRefreshOutcome.SKIPPED, null);
        }
        Path dest = deployTarget.toAbsolutePath().normalize();
        try {
            downloader.downloadIfConfigured(url, deployTarget);
            return new DesktopBundleChannelResult(channel, ChannelRefreshOutcome.REFRESHED, null);
        } catch (IOException e) {
            if (Files.isRegularFile(dest)) {
                log.warn(
                        "Could not refresh {} desktop bundle from {}; keeping existing {}",
                        channel,
                        url,
                        dest,
                        e);
                return new DesktopBundleChannelResult(
                        channel, ChannelRefreshOutcome.KEPT_EXISTING, e.getMessage());
            }
            log.error("Could not download {} desktop bundle from {}", channel, url, e);
            return new DesktopBundleChannelResult(channel, ChannelRefreshOutcome.FAILED, e.getMessage());
        }
    }

    public enum ChannelRefreshOutcome {
        SKIPPED,
        REFRESHED,
        KEPT_EXISTING,
        FAILED
    }

    public record DesktopBundleChannelResult(
            String channel, ChannelRefreshOutcome outcome, String message) {}

    public record DesktopBundleRefreshReport(List<DesktopBundleChannelResult> channels) {}
}