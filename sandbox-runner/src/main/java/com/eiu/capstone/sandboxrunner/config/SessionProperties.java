package com.eiu.capstone.sandboxrunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sandbox.session")
public class SessionProperties {

    private long maxTarballBytes = 10_485_760;
    private long ttlSeconds = 600;
    private int maxSessions = 64;

    public long getMaxTarballBytes() {
        return maxTarballBytes;
    }

    public void setMaxTarballBytes(long maxTarballBytes) {
        this.maxTarballBytes = maxTarballBytes;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
    }
}
