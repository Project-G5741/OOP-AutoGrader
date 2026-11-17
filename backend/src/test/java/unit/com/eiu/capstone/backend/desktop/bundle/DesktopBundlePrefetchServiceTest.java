package com.eiu.capstone.backend.desktop.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService.ChannelRefreshOutcome;

class DesktopBundlePrefetchServiceTest {

    @Test
    void refreshAll_skipsBlankUrls() {
        DesktopBundlePrefetchService service = new DesktopBundlePrefetchService(
                new DesktopBundleDownloader(1, Duration.ofSeconds(1), Duration.ofSeconds(5)), "", "");
        var report = service.refreshAll();
        assertEquals(2, report.channels().size());
        assertEquals(ChannelRefreshOutcome.SKIPPED, report.channels().get(0).outcome());
        assertEquals(ChannelRefreshOutcome.SKIPPED, report.channels().get(1).outcome());
    }
}