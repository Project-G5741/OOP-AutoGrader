package com.eiu.capstone.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.utility.TimeUtil;

class StatsRepositoryTest {

    @Test
    void statsRowResolvesJdbcTimestamp() {
        Instant instant = Instant.parse("2026-08-10T10:00:00Z");
        var row = new StatsRepository.StatsRow(1, BigDecimal.TEN, Timestamp.from(instant), 5);

        OffsetDateTime resolved = row.latestSubmittedAtOffset();
        assertNotNull(resolved);
        assertEquals(instant.atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime(), resolved);
    }

    @Test
    void statsRowResolvesLocalDateTime() {
        LocalDateTime local = LocalDateTime.of(2026, 8, 10, 17, 0);
        var row = new StatsRepository.StatsRow(1, BigDecimal.TEN, local, 5);

        OffsetDateTime resolved = row.latestSubmittedAtOffset();
        assertNotNull(resolved);
        assertEquals(local.atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime(), resolved);
    }
}
