package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.WeeklyRankingRules;

class WeeklyRankingRulesTest {

    @Test
    void giftBandsMatchPublishedPrizes() {
        assertEquals(0, WeeklyRankingRules.giftPointsForRank(0));
        assertEquals(1000, WeeklyRankingRules.giftPointsForRank(1));
        assertEquals(800, WeeklyRankingRules.giftPointsForRank(2));
        assertEquals(500, WeeklyRankingRules.giftPointsForRank(3));
        assertEquals(300, WeeklyRankingRules.giftPointsForRank(4));
        assertEquals(300, WeeklyRankingRules.giftPointsForRank(10));
        assertEquals(200, WeeklyRankingRules.giftPointsForRank(11));
        assertEquals(200, WeeklyRankingRules.giftPointsForRank(20));
        assertEquals(0, WeeklyRankingRules.giftPointsForRank(21));
        assertEquals(0, WeeklyRankingRules.giftTicketsForRank(0));
        assertEquals(50, WeeklyRankingRules.giftTicketsForRank(1));
        assertEquals(40, WeeklyRankingRules.giftTicketsForRank(2));
        assertEquals(30, WeeklyRankingRules.giftTicketsForRank(3));
        assertEquals(20, WeeklyRankingRules.giftTicketsForRank(4));
        assertEquals(20, WeeklyRankingRules.giftTicketsForRank(10));
        assertEquals(10, WeeklyRankingRules.giftTicketsForRank(11));
        assertEquals(10, WeeklyRankingRules.giftTicketsForRank(20));
        assertEquals(0, WeeklyRankingRules.giftTicketsForRank(21));
    }
}
