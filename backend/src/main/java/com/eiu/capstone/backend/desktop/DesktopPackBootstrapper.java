package com.eiu.capstone.backend.desktop;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.repository.TermRepository;

@Component
@Lazy(false)
@Profile("desktop")
public class DesktopPackBootstrapper {

    private final DesktopPackBootstrapService bootstrapService;
    private final DesktopLocalUserService localUserService;
    private final DesktopRuntimeState runtimeState;
    private final TermRepository termRepository;

    public DesktopPackBootstrapper(
            DesktopPackBootstrapService bootstrapService,
            DesktopLocalUserService localUserService,
            DesktopRuntimeState runtimeState,
            TermRepository termRepository) {
        this.bootstrapService = bootstrapService;
        this.localUserService = localUserService;
        this.runtimeState = runtimeState;
        this.termRepository = termRepository;
    }

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        runtimeState.setBootstrapComplete(false);
        runtimeState.setBootstrapError(null);
        runtimeState.setPackMissing(false);
        try {
            DesktopPackBootstrapService.BootstrapResult result = bootstrapService.bootstrap();
            runtimeState.setPackMissing(result.packMissing());
            if (result.ready()) {
                runtimeState.setBootstrapError(null);
                runtimeState.setLoadedPackVersion(result.packVersion());
            } else {
                runtimeState.setBootstrapError(result.error());
                runtimeState.setLoadedPackVersion(null);
                localUserService.ensureLocalStudent(termRepository.findCurrent().orElse(null));
            }
        } finally {
            runtimeState.setBootstrapComplete(true);
        }
    }
}
