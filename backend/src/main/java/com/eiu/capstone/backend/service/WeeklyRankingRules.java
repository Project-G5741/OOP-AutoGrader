package com.eiu.capstone.backend.service;

/**
 * Unique-rank gift bands for weekly ranking.
 * Rank 1: 1000 points + 50 tickets. Rank 2: 800 + 40. Rank 3: 500 + 30.
 * Ranks 4–10: 300 + 20. Ranks 11–20: 200 + 10. Rank 21 and below: nothing.
 */
public final class WeeklyRankingRules {

    private WeeklyRankingRules() {
    }

    public static int giftPointsForRank(int rank) {
        if (rank <= 0) {
            return 0;
        }
        if (rank == 1) {
            return 1000;
        }
        if (rank == 2) {
            return 800;
        }
        if (rank == 3) {
            return 500;
        }
        if (rank <= 10) {
            return 300;
        }
        if (rank <= 20) {
            return 200;
        }
        return 0;
    }

    public static int giftTicketsForRank(int rank) {
        if (rank <= 0) {
            return 0;
        }
        if (rank == 1) {
            return 50;
        }
        if (rank == 2) {
            return 40;
        }
        if (rank == 3) {
            return 30;
        }
        if (rank <= 10) {
            return 20;
        }
        if (rank <= 20) {
            return 10;
        }
        return 0;
    }
}
