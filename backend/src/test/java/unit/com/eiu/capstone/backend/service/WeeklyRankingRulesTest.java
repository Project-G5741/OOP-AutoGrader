package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.WeeklyRankingRules;

class WeeklyRankingRulesTest {

    @Test
    void giftBandsMatchPublishedPrizes() {
        assertEquals(0, WeeklyRankingRules.giftPointsForRank(0));
        assertEquals(100, WeeklyRankingRules.giftPointsForRank(1));
        assertEquals(80, WeeklyRankingRules.giftPointsForRank(2));
        assertEquals(60, WeeklyRankingRules.giftPointsForRank(3));
        assertEquals(40, WeeklyRankingRules.giftPointsForRank(4));
        assertEquals(40, WeeklyRankingRules.giftPointsForRank(10));
        assertEquals(20, WeeklyRankingRules.giftPointsForRank(11));
        assertEquals(20, WeeklyRankingRules.giftPointsForRank(20));
        assertEquals(0, WeeklyRankingRules.giftPointsForRank(21));
    }
}
