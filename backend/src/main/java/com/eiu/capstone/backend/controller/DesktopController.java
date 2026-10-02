package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.desktop.DesktopRuntimeState;

@RestController
@RequestMapping("/api/desktop")
@Profile("desktop")
public class DesktopController {

    public record DesktopStatusDTO(
            boolean ready,
            String packVersion,
            String error,
            boolean packMissing,
            boolean bootstrapComplete) {}

    private final DesktopRuntimeState runtimeState;

    public DesktopController(DesktopRuntimeState runtimeState) {
        this.runtimeState = runtimeState;
    }

    @GetMapping("/status")
    public DesktopStatusDTO status() {
        return new DesktopStatusDTO(
                runtimeState.isReady(),
                runtimeState.loadedPackVersion(),
                runtimeState.bootstrapError(),
                runtimeState.isPackMissing(),
                runtimeState.isBootstrapComplete());
    }
}
