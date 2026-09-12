package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.PresenceService;

class PresenceServiceTest {

    @Test
    void uniqueEmailsCountOnce() {
        PresenceService presence = PresenceService.forClock(Clock.systemUTC());
        presence.heartbeat("a@eiu.edu.vn");
        presence.heartbeat("A@eiu.edu.vn");
        presence.heartbeat("b@eiu.edu.vn");
        assertEquals(2, presence.countActive());
    }

    @Test
    void heartbeatInsideWindowStaysActive() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T13:00:00Z"));
        PresenceService presence = PresenceService.forClock(clock);
        presence.heartbeat("a@eiu.edu.vn");
        clock.advance(PresenceService.ACTIVE_WINDOW.minusSeconds(1));
        assertEquals(1, presence.countActive());
    }

    @Test
    void expiredHeartbeatsDropOut() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T13:00:00Z"));
        PresenceService presence = PresenceService.forClock(clock);
        presence.heartbeat("a@eiu.edu.vn");
        clock.advance(PresenceService.ACTIVE_WINDOW.plusSeconds(1));
        assertEquals(0, presence.countActive());
    }

    @Test
    void blankEmailsAreIgnored() {
        PresenceService presence = PresenceService.forClock(Clock.systemUTC());
        presence.heartbeat(" ");
        presence.heartbeat(null);
        assertEquals(0, presence.countActive());
    }

    @Test
    void leaveRemovesTheUserImmediately() {
        PresenceService presence = PresenceService.forClock(Clock.systemUTC());
        presence.heartbeat("a@eiu.edu.vn");
        presence.heartbeat("b@eiu.edu.vn");
        presence.leave("A@eiu.edu.vn");
        assertEquals(1, presence.countActive());
        presence.leave("b@eiu.edu.vn");
        assertEquals(0, presence.countActive());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
