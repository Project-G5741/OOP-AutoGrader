package com.eiu.capstone.backend.DTO;

public record StudentStreakView(
        int streak,
        int points,
        boolean loggedIn,
        int submitCount,
        int submitGoal,
        int petCount,
        int petGoal,
        boolean completedToday,
        boolean broken,
        int savedStreak,
        boolean canRestore,
        String equipped,
        boolean missionCompletedNow) {
}
