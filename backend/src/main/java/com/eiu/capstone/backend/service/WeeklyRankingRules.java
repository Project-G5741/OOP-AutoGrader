package com.eiu.capstone.backend.service;

/**
 * Unique-rank gift bands for weekly ranking (R7–R8).
 */
public final class WeeklyRankingRules {

    private WeeklyRankingRules() {
    }

    public static int giftPointsForRank(int rank) {
        if (rank <= 0) {
            return 0;
        }
        if (rank == 1) {
            return 500;
        }
        if (rank == 2) {
            return 300;
        }
        if (rank == 3) {
            return 200;
        }
        if (rank <= 10) {
            return 100;
        }
        if (rank <= 20) {
            return 50;
        }
        return 0;
    }
}
