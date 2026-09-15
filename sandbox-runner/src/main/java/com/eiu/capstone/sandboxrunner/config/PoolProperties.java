package com.eiu.capstone.sandboxrunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sandbox.pool")
public class PoolProperties {

    private int minWarm = 1;
    private int maxWarm = 4;
    private long scaleUpWaitMs = 2000;
    private long scaleDownIdleMs = 60000;
    private boolean warmOnStart = true;

    public int getMinWarm() {
        return minWarm;
    }

    public void setMinWarm(int minWarm) {
        this.minWarm = minWarm;
    }

    public int getMaxWarm() {
        return maxWarm;
    }

    public void setMaxWarm(int maxWarm) {
        this.maxWarm = maxWarm;
    }

    public long getScaleUpWaitMs() {
        return scaleUpWaitMs;
    }

    public void setScaleUpWaitMs(long scaleUpWaitMs) {
        this.scaleUpWaitMs = scaleUpWaitMs;
    }

    public long getScaleDownIdleMs() {
        return scaleDownIdleMs;
    }

    public void setScaleDownIdleMs(long scaleDownIdleMs) {
        this.scaleDownIdleMs = scaleDownIdleMs;
    }

    public boolean isWarmOnStart() {
        return warmOnStart;
    }

    public void setWarmOnStart(boolean warmOnStart) {
        this.warmOnStart = warmOnStart;
    }
}
