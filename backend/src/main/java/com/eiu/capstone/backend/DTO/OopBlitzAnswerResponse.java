package com.eiu.capstone.backend.DTO;

public record OopBlitzAnswerResponse(
        int questionIndex,
        boolean correct,
        boolean timedOut,
        int correctIndex,
        String explanation,
        int basePts,
        int comboBonus,
        int speedBonus,
        int questionPts,
        int combo,
        int maxCombo,
        int totalScoreSoFar,
        int correctCount,
        long feedbackHoldMs,
        boolean finished,
        OopBlitzQuestionView nextQuestion,
        OopBlitzSummaryView summary,
        OopBlitzStatusView status) {
}
