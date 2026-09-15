package com.eiu.capstone.sandboxrunner.pool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.eiu.capstone.sandboxrunner.config.PoolProperties;
import com.eiu.capstone.sandboxrunner.docker.ContainerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class WarmPoolManager {

    private final ContainerFactory containerFactory;
    private final PoolProperties poolProperties;
    private final Deque<String> idle = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();
    private int liveContainers;
    private volatile boolean shuttingDown;

    public WarmPoolManager(ContainerFactory containerFactory, PoolProperties poolProperties) {
        this.containerFactory = containerFactory;
        this.poolProperties = poolProperties;
    }

    @PostConstruct
    void warmOnStart() {
        if (!poolProperties.isWarmOnStart()) {
            return;
        }
        while (idleCount() < poolProperties.getMinWarm()) {
            addIdleContainer(createContainer());
        }
    }

    public String acquire(long maxWaitMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        lock.lock();
        try {
            while (!shuttingDown) {
                if (!idle.isEmpty()) {
                    return idle.removeFirst();
                }
                if (liveContainers < poolProperties.getMaxWarm()) {
                    lock.unlock();
                    try {
                        return trackLiveContainer(createContainer());
                    } finally {
                        lock.lock();
                    }
                }
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return null;
                }
                available.awaitNanos(remaining * 1_000_000L);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void release(String containerId, boolean destroy) {
        lock.lock();
        try {
            if (destroy || shuttingDown) {
                containerFactory.destroyContainer(containerId);
                liveContainers = Math.max(0, liveContainers - 1);
            } else {
                idle.addLast(containerId);
            }
            available.signalAll();
            maybeReplenishLocked();
        } finally {
            lock.unlock();
        }
    }

    private void maybeReplenishLocked() {
        while (!shuttingDown && idle.size() < poolProperties.getMinWarm()
                && liveContainers < poolProperties.getMaxWarm()) {
            lock.unlock();
            try {
                addIdleContainer(createContainer());
            } finally {
                lock.lock();
            }
        }
    }

    private String createContainer() {
        return containerFactory.createIdleContainer();
    }

    private String trackLiveContainer(String containerId) {
        lock.lock();
        try {
            liveContainers++;
            return containerId;
        } finally {
            lock.unlock();
        }
    }

    private void addIdleContainer(String containerId) {
        lock.lock();
        try {
            idle.addLast(containerId);
            liveContainers++;
            available.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int idleCount() {
        lock.lock();
        try {
            return idle.size();
        } finally {
            lock.unlock();
        }
    }

    @PreDestroy
    void shutdown() {
        shuttingDown = true;
        lock.lock();
        try {
            while (!idle.isEmpty()) {
                containerFactory.destroyContainer(idle.removeFirst());
            }
            liveContainers = 0;
        } finally {
            lock.unlock();
        }
    }
}
