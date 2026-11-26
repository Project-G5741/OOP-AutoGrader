package com.eiu.capstone.backend.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_oop_blitz_session")
public class StudentOopBlitzSession {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "mode", nullable = false, length = 16)
    private String mode;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "questions_json", nullable = false, columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson = "[]";

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "current_combo", nullable = false)
    private int currentCombo;

    @Column(name = "max_combo", nullable = false)
    private int maxCombo;

    @Column(name = "speed_bonus_total", nullable = false)
    private int speedBonusTotal;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(name = "points_credited", nullable = false)
    private int pointsCredited;

    @Column(name = "question_issued_at")
    private OffsetDateTime questionIssuedAt;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getPlayDate() {
        return playDate;
    }

    public void setPlayDate(LocalDate playDate) {
        this.playDate = playDate;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getCurrentCombo() {
        return currentCombo;
    }

    public void setCurrentCombo(int currentCombo) {
        this.currentCombo = currentCombo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(int maxCombo) {
        this.maxCombo = maxCombo;
    }

    public int getSpeedBonusTotal() {
        return speedBonusTotal;
    }

    public void setSpeedBonusTotal(int speedBonusTotal) {
        this.speedBonusTotal = speedBonusTotal;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public int getPointsCredited() {
        return pointsCredited;
    }

    public void setPointsCredited(int pointsCredited) {
        this.pointsCredited = pointsCredited;
    }

    public OffsetDateTime getQuestionIssuedAt() {
        return questionIssuedAt;
    }

    public void setQuestionIssuedAt(OffsetDateTime questionIssuedAt) {
        this.questionIssuedAt = questionIssuedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
