package com.eiu.capstone.backend.desktop.bundle;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Lazy(false)
@Profile("!desktop & !desktop-lecturer")
public class DesktopBundlePrefetchRunner {

    private final DesktopBundlePrefetchService prefetchService;

    public DesktopBundlePrefetchRunner(DesktopBundlePrefetchService prefetchService) {
        this.prefetchService = prefetchService;
    }

    @Order(25)
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        prefetchService.refreshAll();
    }
}