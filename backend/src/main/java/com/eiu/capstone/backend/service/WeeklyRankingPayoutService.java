package com.eiu.capstone.backend.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingBoardResponse;
import com.eiu.capstone.backend.analytics.dto.WeeklyRankingRow;
import com.eiu.capstone.backend.analytics.service.WeeklyRankingService;

@Service
@Profile("!desktop")
public class WeeklyRankingPayoutService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyRankingPayoutService.class);

    private final JdbcTemplate jdbcTemplate;
    private final WeeklyRankingService weeklyRankingService;
    private final StudentStreakService studentStreakService;

    public WeeklyRankingPayoutService(
            JdbcTemplate jdbcTemplate,
            WeeklyRankingService weeklyRankingService,
            StudentStreakService studentStreakService) {
        this.jdbcTemplate = jdbcTemplate;
        this.weeklyRankingService = weeklyRankingService;
        this.studentStreakService = studentStreakService;
    }

    public void catchUpUnpaidWeeks(Instant now) {
        Instant earliest = earliestUnpaidOrDefault(now);
        for (Instant weekEnd : WeeklyRankingCalendar.completedWeekEndsThrough(earliest, now)) {
            payWeekEnding(weekEnd);
        }
    }

    @Transactional
    public void payWeekEnding(Instant weekEnd) {
        Timestamp weekTs = Timestamp.from(weekEnd);
        jdbcTemplate.update(
                "INSERT INTO weekly_ranking_week (week_end, paid_at) VALUES (?, NULL) ON CONFLICT (week_end) DO NOTHING",
                weekTs);
        Instant paidAt = jdbcTemplate.query(
                "SELECT paid_at FROM weekly_ranking_week WHERE week_end = ? FOR UPDATE",
                rs -> rs.next() ? (rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant()) : null,
                weekTs);
        if (paidAt != null) {
            return;
        }
        WeeklyRankingBoardResponse board = weeklyRankingService.boardAsOf(weekEnd, weekEnd);
        for (WeeklyRankingRow row : board.rows()) {
            creditIfNew(weekTs, row);
        }
        jdbcTemplate.update(
                "UPDATE weekly_ranking_week SET paid_at = ? WHERE week_end = ? AND paid_at IS NULL",
                Timestamp.from(Instant.now()),
                weekTs);
        log.info("Weekly ranking paid for week ending {}", weekEnd);
    }

    private void creditIfNew(Timestamp weekTs, WeeklyRankingRow row) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO weekly_ranking_entry (week_end, user_id, rank, total, gift_points)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (week_end, user_id) DO NOTHING
                """,
                weekTs,
                row.userId(),
                row.rank(),
                row.total(),
                row.giftPoints());
        if (inserted == 1 && row.giftPoints() > 0) {
            studentStreakService.addPoints(row.userId(), row.giftPoints());
        }
    }

    private Instant earliestUnpaidOrDefault(Instant now) {
        List<Timestamp> unpaid = jdbcTemplate.query(
                "SELECT week_end FROM weekly_ranking_week WHERE paid_at IS NULL ORDER BY week_end LIMIT 1",
                (rs, i) -> rs.getTimestamp(1));
        if (!unpaid.isEmpty() && unpaid.get(0) != null) {
            return unpaid.get(0).toInstant();
        }
        List<Timestamp> latestPaid = jdbcTemplate.query(
                "SELECT MAX(week_end) FROM weekly_ranking_week WHERE paid_at IS NOT NULL",
                (rs, i) -> rs.getTimestamp(1));
        if (!latestPaid.isEmpty() && latestPaid.get(0) != null) {
            return latestPaid.get(0).toInstant().plusSeconds(1);
        }
        return WeeklyRankingCalendar.weekStartOf(now);
    }
}
