package unit.com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingRow;
import com.eiu.capstone.backend.analytics.repository.WeeklyRankingRepository;
import com.eiu.capstone.backend.analytics.service.WeeklyRankingService;

class WeeklyRankingServiceTest {

    @Test
    void toRowsAssignsDenseUniqueRanksAndGifts() {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");
        List<WeeklyRankingRow> rows = WeeklyRankingService.toRows(List.of(
                new WeeklyRankingRepository.RankSource(first, "A", "IRN1", new BigDecimal("90"), null),
                new WeeklyRankingRepository.RankSource(second, "B", "IRN2", new BigDecimal("90"), null)));
        assertEquals(1, rows.get(0).rank());
        assertEquals(100, rows.get(0).giftPoints());
        assertEquals(2, rows.get(1).rank());
        assertEquals(80, rows.get(1).giftPoints());
    }
}
