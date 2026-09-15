package com.eiu.capstone.sandboxrunner.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.eiu.capstone.sandboxrunner.config.PoolProperties;
import com.eiu.capstone.sandboxrunner.docker.ContainerFactory;
import org.junit.jupiter.api.Test;

class WarmPoolManagerTest {

    @Test
    void acquireCreatesContainerWhenPoolEmpty() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        PoolProperties props = new PoolProperties();
        props.setMinWarm(0);
        props.setMaxWarm(2);
        props.setWarmOnStart(false);

        WarmPoolManager pool = new WarmPoolManager(factory, props);
        String id = pool.acquire(1000);
        assertEquals("c1", id);
    }

    @Test
    void releaseDestroysWhenRequested() {
        RecordingFactory factory = new RecordingFactory();
        PoolProperties props = new PoolProperties();
        props.setWarmOnStart(false);

        WarmPoolManager pool = new WarmPoolManager(factory, props);
        pool.release("c1", true);
        assertEquals(List.of("c1"), factory.destroyed);
    }

    @Test
    void acquireReturnsNullWhenPoolExhausted() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        PoolProperties props = new PoolProperties();
        props.setMinWarm(0);
        props.setMaxWarm(1);
        props.setWarmOnStart(false);

        WarmPoolManager pool = new WarmPoolManager(factory, props);
        assertEquals("c1", pool.acquire(1000));
        assertNull(pool.acquire(50));
    }

    private static final class RecordingFactory implements ContainerFactory {
        private final AtomicInteger counter = new AtomicInteger();
        private final List<String> destroyed = new ArrayList<>();

        @Override
        public String createIdleContainer() {
            return "c" + counter.incrementAndGet();
        }

        @Override
        public void destroyContainer(String containerId) {
            destroyed.add(containerId);
        }
    }
}
