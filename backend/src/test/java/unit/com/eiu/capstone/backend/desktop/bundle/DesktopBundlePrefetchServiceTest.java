package com.eiu.capstone.backend.desktop.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService.ChannelRefreshOutcome;

class DesktopBundlePrefetchServiceTest {

    @Test
    void refreshAll_skipsBlankUrls() {
        DesktopBundlePrefetchService service =
                new DesktopBundlePrefetchService(1, 1, 5, "", "");
        var report = service.refreshAll();
        assertEquals(2, report.channels().size());
        assertEquals(ChannelRefreshOutcome.SKIPPED, report.channels().get(0).outcome());
        assertEquals(ChannelRefreshOutcome.SKIPPED, report.channels().get(1).outcome());
    }
}