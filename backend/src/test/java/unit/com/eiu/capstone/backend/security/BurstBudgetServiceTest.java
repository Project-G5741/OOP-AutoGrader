package unit.com.eiu.capstone.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.security.BurstBudgetService;
import com.eiu.capstone.backend.security.BurstBudgetService.Lane;

class BurstBudgetServiceTest {

    @Test
    void twentyAllowsThenDeny() {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 20, 10, 5, 100);
        for (int i = 0; i < 20; i++) {
            assertTrue(budget.tryConsume("user:a").allowed());
        }
        BurstBudgetService.Decision denied = budget.tryConsume("user:a");
        assertFalse(denied.allowed());
        assertTrue(denied.retryAfterSeconds() >= 5);
    }

    @Test
    void windowSlideAllowsAgain() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-09T12:00:00Z"));
        BurstBudgetService budget = BurstBudgetService.forTest(clock, 2, 10, 5, 100);
        assertTrue(budget.tryConsume("k").allowed());
        assertTrue(budget.tryConsume("k").allowed());
        assertFalse(budget.tryConsume("k").allowed());
        clock.advance(Duration.ofSeconds(11));
        assertTrue(budget.tryConsume("k").allowed());
    }

    @Test
    void readsDoNotSpendTheWriteBudget() {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 2, 60, 10, 5, 100);
        for (int i = 0; i < 13; i++) {
            assertTrue(budget.tryConsume("user:a", true).allowed());
        }
        assertTrue(budget.tryConsume("user:a").allowed());
        assertTrue(budget.tryConsume("user:a").allowed());
        assertFalse(budget.tryConsume("user:a").allowed());
        assertTrue(budget.tryConsume("user:a", true).allowed());
        assertEquals(2, budget.currentCount("user:a"));
        assertEquals(14, budget.currentCount("user:a", true));
    }

    @Test
    void authLaneDoesNotSpendWriteBudgetAndAllowsClassroomBurst() {
        // Tight writes (5), generous auth (60) — shared Wi‑Fi login burst.
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 5, 30, 60, 10, 5, 100);
        for (int i = 0; i < 60; i++) {
            assertTrue(budget.tryConsume("ip:10.0.0.1", Lane.AUTH).allowed());
        }
        assertFalse(budget.tryConsume("ip:10.0.0.1", Lane.AUTH).allowed());
        // Write lane for the same IP key is still open.
        for (int i = 0; i < 5; i++) {
            assertTrue(budget.tryConsume("ip:10.0.0.1", Lane.WRITE).allowed());
        }
        assertFalse(budget.tryConsume("ip:10.0.0.1", Lane.WRITE).allowed());
        assertEquals(60, budget.currentCount("ip:10.0.0.1", Lane.AUTH));
        assertEquals(5, budget.currentCount("ip:10.0.0.1", Lane.WRITE));
    }

    @Test
    void independentKeysDoNotShareBudget() {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 1, 10, 5, 100);
        assertTrue(budget.tryConsume("a").allowed());
        assertTrue(budget.tryConsume("b").allowed());
        assertFalse(budget.tryConsume("a").allowed());
    }

    @Test
    void idleKeysEvictUnderMaxKeys() {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 1, 10, 5, 16);
        for (int i = 0; i < 40; i++) {
            assertTrue(budget.tryConsume("key-" + i).allowed());
        }
        assertTrue(budget.keyCount() <= 16);
    }

    @Test
    void concurrentSameKeyRespectsMax() throws Exception {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 20, 10, 5, 100);
        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allows = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                if (budget.tryConsume("hot").allowed()) {
                    allows.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
        assertEquals(20, allows.get());
        assertEquals(20, budget.currentCount("hot"));
    }

    @Test
    void defaultsMatchProductNumbers() {
        BurstBudgetService budget = BurstBudgetService.forTest(Clock.systemUTC(), 20, 10, 5, 10000);
        assertEquals(20, budget.maxRequests());
        assertEquals(10, budget.windowSeconds());
        assertEquals(5, budget.defaultRetryAfterSeconds());
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
