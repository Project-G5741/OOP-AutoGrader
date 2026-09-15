package com.eiu.capstone.sandboxrunner.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eiu.capstone.sandboxrunner.config.PoolProperties;
import com.eiu.capstone.sandboxrunner.config.SessionProperties;
import com.eiu.capstone.sandboxrunner.docker.ContainerFactory;
import com.eiu.capstone.sandboxrunner.model.SessionRecord;
import com.eiu.capstone.sandboxrunner.pool.WarmPoolManager;
import org.junit.jupiter.api.Test;

class SessionServiceLifecycleTest {

    @Test
    void shutdownAllSessionsReleasesContainers() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        SessionService service = newSessionService(factory);
        Process worker = new ProcessBuilder("java", "-version").start();
        injectSession(service, "s1", new SessionRecord("s1", "container-1", worker));

        service.shutdownAllSessions();
        assertTrue(factory.destroyed.contains("container-1"));
    }

    @Test
    void sweepExpiredSessionsDeletesStaleEntries() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        SessionProperties sessionProperties = new SessionProperties();
        sessionProperties.setTtlSeconds(1);
        SessionService service = new SessionService(
                new WarmPoolManager(factory, poolProperties()),
                sessionProperties,
                poolProperties());

        Process worker = new ProcessBuilder("java", "-version").start();
        SessionRecord record = new SessionRecord("old", "container-old", worker);
        injectSession(service, "old", record);
        forceCreatedAt(record, Instant.now().minusSeconds(120));

        service.sweepExpiredSessions();
        assertTrue(factory.destroyed.contains("container-old"));
    }

    private static SessionService newSessionService(RecordingFactory factory) {
        return new SessionService(
                new WarmPoolManager(factory, poolProperties()),
                sessionProperties(),
                poolProperties());
    }

    private static SessionProperties sessionProperties() {
        SessionProperties props = new SessionProperties();
        props.setTtlSeconds(600);
        return props;
    }

    private static PoolProperties poolProperties() {
        PoolProperties props = new PoolProperties();
        props.setWarmOnStart(false);
        props.setMinWarm(0);
        return props;
    }

    private static void injectSession(SessionService service, String id, SessionRecord record) throws Exception {
        var field = SessionService.class.getDeclaredField("sessions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, SessionRecord> sessions = (Map<String, SessionRecord>) field.get(service);
        sessions.put(id, record);
    }

    private static void forceCreatedAt(SessionRecord record, Instant instant) throws Exception {
        var field = SessionRecord.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(record, instant);
    }

    private static final class RecordingFactory implements ContainerFactory {
        private final List<String> destroyed = new ArrayList<>();

        @Override
        public String createIdleContainer() {
            return "container-" + destroyed.size();
        }

        @Override
        public void destroyContainer(String containerId) {
            destroyed.add(containerId);
        }
    }
}
