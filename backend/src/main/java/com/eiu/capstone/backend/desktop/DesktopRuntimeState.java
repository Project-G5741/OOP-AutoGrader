package com.eiu.capstone.backend.desktop;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopRuntimeState {

    private volatile String loadedPackVersion;
    private volatile String bootstrapError;
    private volatile boolean packMissing;
    private volatile boolean bootstrapComplete;

    public String loadedPackVersion() {
        return loadedPackVersion;
    }

    public void setLoadedPackVersion(String loadedPackVersion) {
        this.loadedPackVersion = loadedPackVersion;
    }

    public String bootstrapError() {
        return bootstrapError;
    }

    public void setBootstrapError(String bootstrapError) {
        this.bootstrapError = bootstrapError;
    }

    public boolean isPackMissing() {
        return packMissing;
    }

    public void setPackMissing(boolean packMissing) {
        this.packMissing = packMissing;
    }

    public boolean isBootstrapComplete() {
        return bootstrapComplete;
    }

    public void setBootstrapComplete(boolean bootstrapComplete) {
        this.bootstrapComplete = bootstrapComplete;
    }

    public boolean isReady() {
        return bootstrapError == null && loadedPackVersion != null && !loadedPackVersion.isBlank();
    }
}
