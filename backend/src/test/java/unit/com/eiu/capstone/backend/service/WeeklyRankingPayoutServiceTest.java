package unit.com.eiu.capstone.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingBoardResponse;
import com.eiu.capstone.backend.analytics.dto.WeeklyRankingRow;
import com.eiu.capstone.backend.analytics.service.WeeklyRankingService;
import com.eiu.capstone.backend.service.StudentStreakService;
import com.eiu.capstone.backend.service.WeeklyRankingPayoutService;

@ExtendWith(MockitoExtension.class)
class WeeklyRankingPayoutServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private WeeklyRankingService weeklyRankingService;

    @Mock
    private StudentStreakService studentStreakService;

    @Test
    void alreadyPaidWeekDoesNotCreditAgain() {
        Instant weekEnd = Instant.parse("2026-10-18T16:59:59Z");
        when(jdbcTemplate.update(contains("weekly_ranking_week"), any(Timestamp.class))).thenReturn(0);
        when(jdbcTemplate.query(contains("FOR UPDATE"), any(ResultSetExtractor.class), any(Timestamp.class)))
                .thenAnswer(invocation -> Instant.parse("2026-10-18T17:00:00Z"));

        new WeeklyRankingPayoutService(jdbcTemplate, weeklyRankingService, studentStreakService)
                .payWeekEnding(weekEnd);

        verify(weeklyRankingService, never()).boardAsOf(any(), any());
        verify(studentStreakService, never()).addPoints(any(), any(int.class));
    }

    @Test
    void conflictOnEntryDoesNotDoubleCredit() throws Exception {
        Instant weekEnd = Instant.parse("2026-10-18T16:59:59Z");
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(jdbcTemplate.update(contains("weekly_ranking_week"), any(Timestamp.class))).thenReturn(1);
        when(jdbcTemplate.query(contains("FOR UPDATE"), any(ResultSetExtractor.class), any(Timestamp.class)))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<Instant> extractor = invocation.getArgument(1);
                    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
                    when(rs.next()).thenReturn(true);
                    when(rs.getTimestamp(1)).thenReturn(null);
                    return extractor.extractData(rs);
                });
        when(weeklyRankingService.boardAsOf(weekEnd, weekEnd)).thenReturn(new WeeklyRankingBoardResponse(
                weekEnd,
                weekEnd,
                List.of(new WeeklyRankingRow(1, userId, "A", "IRN1", new BigDecimal("100"), 100))));
        when(jdbcTemplate.update(contains("weekly_ranking_entry"), any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(jdbcTemplate.update(contains("paid_at"), any(Timestamp.class), any(Timestamp.class))).thenReturn(1);

        new WeeklyRankingPayoutService(jdbcTemplate, weeklyRankingService, studentStreakService)
                .payWeekEnding(weekEnd);

        verify(studentStreakService, never()).addPoints(any(), any(int.class));
    }

    @Test
    void newEntryCreditsGiftPointsOnce() throws Exception {
        Instant weekEnd = Instant.parse("2026-10-18T16:59:59Z");
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(jdbcTemplate.update(contains("weekly_ranking_week"), any(Timestamp.class))).thenReturn(1);
        when(jdbcTemplate.query(contains("FOR UPDATE"), any(ResultSetExtractor.class), any(Timestamp.class)))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<Instant> extractor = invocation.getArgument(1);
                    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
                    when(rs.next()).thenReturn(true);
                    when(rs.getTimestamp(1)).thenReturn(null);
                    return extractor.extractData(rs);
                });
        when(weeklyRankingService.boardAsOf(weekEnd, weekEnd)).thenReturn(new WeeklyRankingBoardResponse(
                weekEnd,
                weekEnd,
                List.of(new WeeklyRankingRow(1, userId, "A", "IRN1", new BigDecimal("100"), 100))));
        when(jdbcTemplate.update(contains("weekly_ranking_entry"), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(jdbcTemplate.update(contains("paid_at"), any(Timestamp.class), any(Timestamp.class))).thenReturn(1);

        new WeeklyRankingPayoutService(jdbcTemplate, weeklyRankingService, studentStreakService)
                .payWeekEnding(weekEnd);

        verify(studentStreakService).addPoints(userId, 100);
    }
}
