package unit.com.eiu.capstone.backend.analytics.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.analytics.cache.AnalyticsDashboardCache;
import com.eiu.capstone.backend.analytics.cache.InProcessTtlCache;
import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.LabDeadlineHelper;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.TermService;

/**
 * Permanent regression for bounded in-process TTL caches (memory-leak audit F-1/F-2/F-3).
 * Offline only — no Spring context, DB, or network.
 */
@ExtendWith(MockitoExtension.class)
class InProcessTtlCacheBoundTest {

    private static final AnalyticsDashboardCache.Loader DUMMY_LOADER = () ->
            new AnalyticsDashboardResponse(
                    (java.math.BigDecimal) null,
                    (String) null,
                    (java.math.BigDecimal) null,
                    (String) null,
                    (java.util.List<AnalyticsDashboardResponse.LabTrendItem>) null,
                    (java.util.List<AnalyticsDashboardResponse.StudentOverviewItem>) null,
                    (java.util.List<AnalyticsDashboardResponse.AtRiskLabItem>) null,
                    (java.util.List<AnalyticsDashboardResponse.AtRiskStudentItem>) null,
                    (AnalyticsDashboardResponse.AiSummary) null);

    @Mock private TermService termService;
    @Mock private LabDeadlineHelper labDeadlineHelper;
    @Mock private UserAccountRepository userAccountRepository;

    @Test
    void getWithinTtl_reusesCachedValueWithoutSecondLoad() {
        InProcessTtlCache<String, String> cache = new InProcessTtlCache<>(30, 10);
        AtomicInteger loads = new AtomicInteger();
        String first = cache.get("k", () -> {
            loads.incrementAndGet();
            return "v";
        });
        String second = cache.get("k", () -> {
            loads.incrementAndGet();
            return "other";
        });
        assertSame(first, second);
        assertEquals(1, loads.get());
    }

    @Test
    void afterTtlSweep_sizeCollapsesWithoutRequeryingKeys() throws Exception {
        AnalyticsDashboardCache cache = new AnalyticsDashboardCache(1L, 20_000);
        int iterations = 500;
        for (int i = 0; i < iterations; i++) {
            AnalyticsDashboardCache.CacheKey key = new AnalyticsDashboardCache.CacheKey(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "course-" + i);
            cache.get(key, DUMMY_LOADER);
        }
        assertEquals(iterations, cache.sizeForTests());

        Thread.sleep(1200);
        cache.sweepExpired();

        assertEquals(0, cache.sizeForTests(),
                "Expired entries must leave without a same-key re-hit (AE1)");
    }

    @Test
    void distinctKeyFlood_staysAtOrUnderMaxSize() {
        int maxSize = 50;
        AnalyticsDashboardCache cache = new AnalyticsDashboardCache(180L, maxSize);
        AnalyticsDashboardCache.CacheKey lastKey = null;
        for (int i = 0; i < maxSize + 200; i++) {
            lastKey = new AnalyticsDashboardCache.CacheKey(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "course-" + i);
            cache.get(lastKey, DUMMY_LOADER);
        }
        assertTrue(cache.sizeForTests() <= maxSize,
                "Distinct-key flood must stay <= maxSize (AE2); was " + cache.sizeForTests());
        // Most-recent insert must remain (eviction must not drop the key just written).
        AtomicInteger loads = new AtomicInteger();
        AnalyticsDashboardCache.CacheKey retained = lastKey;
        cache.get(retained, () -> {
            loads.incrementAndGet();
            return DUMMY_LOADER.load();
        });
        assertEquals(0, loads.get(), "Just-inserted key must survive size eviction");
    }

    @Test
    void labStatisticsInvalidate_forcesReload() {
        LabStatisticsCache cache = new LabStatisticsCache(120L, 64);
        UUID labId = UUID.randomUUID();
        AtomicInteger loads = new AtomicInteger();
        LabStatisticsResponse first = cache.get(labId, () -> {
            loads.incrementAndGet();
            return emptyLabStats();
        });
        cache.invalidate(labId);
        LabStatisticsResponse second = cache.get(labId, () -> {
            loads.incrementAndGet();
            return emptyLabStats();
        });
        assertEquals(2, loads.get());
        assertEquals(first.getClass(), second.getClass());
    }

    @Test
    void accessCache_retainsPairsUntilSweepOrCap() {
        StudentTermAccessService service = new StudentTermAccessService(
                termService, labDeadlineHelper, userAccountRepository, 30L, 100, null);

        int pairs = 150;
        for (int i = 0; i < pairs; i++) {
            UserAccount user = new UserAccount();
            user.setEmail("student" + i + "@eiu.edu.vn");
            user.setIsActive(true);
            Lab lab = new Lab();
            lab.setId(UUID.randomUUID());
            service.rememberSuccessfulAccess(user, lab);
        }

        assertTrue(service.accessCacheSizeForTests() <= 100,
                "Upload access cache must honor max size; was " + service.accessCacheSizeForTests());
    }

    @Test
    void getIfPresent_returnsNullForMissingAndRemovesExpired() throws Exception {
        InProcessTtlCache<String, String> cache = new InProcessTtlCache<>(1, 10);
        assertNull(cache.getIfPresent("missing"));
        cache.put("k", "v");
        assertEquals("v", cache.getIfPresent("k"));
        Thread.sleep(1200);
        assertNull(cache.getIfPresent("k"));
        assertEquals(0, cache.size());
    }

    private static LabStatisticsResponse emptyLabStats() {
        return new LabStatisticsResponse(
                UUID.randomUUID(),
                "lab",
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                0L,
                0L,
                0L,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.util.List.of());
    }
}
