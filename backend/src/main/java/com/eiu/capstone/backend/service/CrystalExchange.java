package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.StudentStreak;

/**
 * Turns stored crystals into reward points. One crystal pays {@value #POINTS_PER_CRYSTAL}.
 */
public final class CrystalExchange {

    public static final int POINTS_PER_CRYSTAL = 4000;
    public static final int MAX_COUNT = 99;

    private CrystalExchange() {
    }

    public static void exchange(StudentStreak row, int count) {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("count");
        }
        if (row.getCrystals() < count) {
            throw new IllegalStateException("crystals");
        }
        long nextPoints = (long) row.getPoints() + (long) count * POINTS_PER_CRYSTAL;
        if (nextPoints > Integer.MAX_VALUE) {
            throw new IllegalStateException("points");
        }
        row.setCrystals(row.getCrystals() - count);
        row.setPoints((int) nextPoints);
    }
}
