package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;

import com.eiu.capstone.backend.DTO.StudentStreakView;
import com.eiu.capstone.backend.model.StudentStreak;

/**
 * Daily streak: log in, submit {@value #SUBMIT_GOAL} times, pet {@value #PET_GOAL} times.
 * Completing all three adds one streak day and {@value #BASE_POINTS} points.
 * Each block of 5 streak days adds 5 points, and that bonus stays on later days
 * (day 1 → 50, day 5 → 55, day 6 → 55, day 10 → 60, day 101 → 150).
 * One restore per calendar month continues a broken streak.
 */
public final class DailyStreakRules {

    public static final int SUBMIT_GOAL = 3;
    public static final int PET_GOAL = 4;
    public static final int BASE_POINTS = 50;

    private DailyStreakRules() {
    }

    public static int rewardFor(int streakDay) {
        if (streakDay <= 0) {
            return 0;
        }
        return BASE_POINTS + (streakDay / 5) * 5;
    }

    public static void roll(StudentStreak row, LocalDate today) {
        if (today.equals(row.getMissionDate())) {
            return;
        }
        LocalDate last = row.getLastCompletedOn();
        if (!row.isStreakBroken()
                && row.getStreakCount() > 0
                && (last == null || last.isBefore(today.minusDays(1)))) {
            row.setStreakBroken(true);
            row.setSavedStreak(row.getStreakCount());
            row.setStreakCount(0);
        }
        row.setMissionDate(today);
        row.setLoggedIn(false);
        row.setSubmitCount(0);
        row.setPetCount(0);
        row.setAwardedToday(false);
    }

    public static void addPet(StudentStreak row) {
        if (row.getPetCount() < PET_GOAL) {
            row.setPetCount(row.getPetCount() + 1);
        }
    }

    public static void addSubmit(StudentStreak row) {
        row.setSubmitCount(row.getSubmitCount() + 1);
    }

    /**
     * @return points added by this call
     */
    public static int awardIfReady(StudentStreak row, LocalDate today) {
        if (row.isAwardedToday()) {
            return 0;
        }
        if (!row.isLoggedIn() || row.getSubmitCount() < SUBMIT_GOAL || row.getPetCount() < PET_GOAL) {
            return 0;
        }
        int next = row.isStreakBroken() ? 1 : row.getStreakCount() + 1;
        if (row.isStreakBroken()) {
            row.setStreakBroken(false);
            row.setSavedStreak(0);
        }
        int reward = rewardFor(next);
        row.setStreakCount(next);
        row.setPoints(row.getPoints() + reward);
        row.setLastCompletedOn(today);
        row.setAwardedToday(true);
        return reward;
    }

    public static void restore(StudentStreak row, LocalDate today) {
        if (!row.isStreakBroken() || row.getSavedStreak() <= 0) {
            throw new IllegalStateException("Nothing to restore");
        }
        String month = YearMonth.from(today).toString();
        if (month.equals(row.getRestoreYearMonth())) {
            throw new IllegalStateException("Restore already used this month");
        }
        row.setRestoreYearMonth(month);
        row.setStreakBroken(false);
        row.setStreakCount(row.getSavedStreak());
        row.setSavedStreak(0);
        row.setLastCompletedOn(today.minusDays(1));
    }

    public static boolean canRestore(StudentStreak row, LocalDate today) {
        if (!row.isStreakBroken() || row.getSavedStreak() <= 0) {
            return false;
        }
        return !YearMonth.from(today).toString().equals(row.getRestoreYearMonth());
    }

    public static boolean missionJustFinished(
            boolean loggedInBefore, int submitCountBefore, int petCountBefore, StudentStreak row) {
        return (!loggedInBefore && row.isLoggedIn())
                || (submitCountBefore < SUBMIT_GOAL && row.getSubmitCount() >= SUBMIT_GOAL)
                || (petCountBefore < PET_GOAL && row.getPetCount() >= PET_GOAL);
    }

    public static StudentStreakView toView(StudentStreak row, LocalDate today) {
        return toView(row, today, false);
    }

    public static StudentStreakView toView(StudentStreak row, LocalDate today, boolean missionCompletedNow) {
        return new StudentStreakView(
                row.getStreakCount(),
                row.getPoints(),
                row.isLoggedIn(),
                row.getSubmitCount(),
                SUBMIT_GOAL,
                row.getPetCount(),
                PET_GOAL,
                row.isAwardedToday(),
                row.isStreakBroken(),
                row.getSavedStreak(),
                canRestore(row, today),
                row.getEquippedMascot(),
                missionCompletedNow);
    }
}
