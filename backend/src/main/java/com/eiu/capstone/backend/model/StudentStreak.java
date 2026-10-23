package com.eiu.capstone.backend.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_daily_streak")
public class StudentStreak {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "streak_count", nullable = false)
    private int streakCount;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "last_completed_on")
    private LocalDate lastCompletedOn;

    @Column(name = "mission_date")
    private LocalDate missionDate;

    @Column(name = "logged_in", nullable = false)
    private boolean loggedIn;

    @Column(name = "submit_count", nullable = false)
    private int submitCount;

    @Column(name = "pet_count", nullable = false)
    private int petCount;

    @Column(name = "awarded_today", nullable = false)
    private boolean awardedToday;

    @Column(name = "streak_broken", nullable = false)
    private boolean streakBroken;

    @Column(name = "saved_streak", nullable = false)
    private int savedStreak;

    @Column(name = "restore_year_month", length = 7)
    private String restoreYearMonth;

    @Column(name = "equipped_mascot", nullable = false, length = 32)
    private String equippedMascot = "fox";

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getStreakCount() {
        return streakCount;
    }

    public void setStreakCount(int streakCount) {
        this.streakCount = streakCount;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public LocalDate getLastCompletedOn() {
        return lastCompletedOn;
    }

    public void setLastCompletedOn(LocalDate lastCompletedOn) {
        this.lastCompletedOn = lastCompletedOn;
    }

    public LocalDate getMissionDate() {
        return missionDate;
    }

    public void setMissionDate(LocalDate missionDate) {
        this.missionDate = missionDate;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public int getSubmitCount() {
        return submitCount;
    }

    public void setSubmitCount(int submitCount) {
        this.submitCount = submitCount;
    }

    public int getPetCount() {
        return petCount;
    }

    public void setPetCount(int petCount) {
        this.petCount = petCount;
    }

    public boolean isAwardedToday() {
        return awardedToday;
    }

    public void setAwardedToday(boolean awardedToday) {
        this.awardedToday = awardedToday;
    }

    public boolean isStreakBroken() {
        return streakBroken;
    }

    public void setStreakBroken(boolean streakBroken) {
        this.streakBroken = streakBroken;
    }

    public int getSavedStreak() {
        return savedStreak;
    }

    public void setSavedStreak(int savedStreak) {
        this.savedStreak = savedStreak;
    }

    public String getRestoreYearMonth() {
        return restoreYearMonth;
    }

    public void setRestoreYearMonth(String restoreYearMonth) {
        this.restoreYearMonth = restoreYearMonth;
    }

    public String getEquippedMascot() {
        return equippedMascot == null || equippedMascot.isBlank() ? "fox" : equippedMascot;
    }

    public void setEquippedMascot(String equippedMascot) {
        this.equippedMascot = equippedMascot;
    }
}
