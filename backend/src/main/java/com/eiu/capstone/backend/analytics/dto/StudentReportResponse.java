package com.eiu.capstone.backend.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public class StudentReportResponse {

    public final StudentProfile profile;
    public final List<GradeTrendItem> gradeTrend;
    public final List<ChallengeBreakdownItem> challengeBreakdown;
    public final List<String> weakSkills;
    public final List<SubmissionHistoryItem> submissionHistory;
    public final AiRecommendation aiRecommendation;

    public StudentReportResponse(
            StudentProfile profile,
            List<GradeTrendItem> gradeTrend,
            List<ChallengeBreakdownItem> challengeBreakdown,
            List<String> weakSkills,
            List<SubmissionHistoryItem> submissionHistory,
            AiRecommendation aiRecommendation) {
        this.profile = profile;
        this.gradeTrend = gradeTrend;
        this.challengeBreakdown = challengeBreakdown;
        this.weakSkills = weakSkills;
        this.submissionHistory = submissionHistory;
        this.aiRecommendation = aiRecommendation;
    }

    public static record StudentProfile(
            String name,
            String studentCode,
            String email,
            BigDecimal overallAverage,
            BigDecimal highestScore,
            int completedLabs,
            long submissionCount) {
    }

    public static record GradeTrendItem(
            String labId,
            String labName,
            BigDecimal score) {
    }

    public static record ChallengeBreakdownItem(
            String challengeName,
            int scorePercent) {
    }

    public static record SubmissionHistoryItem(
            String labName,
            int attempt,
            BigDecimal score,
            String submittedAt,
            boolean bestSubmission) {
    }

    public static record AiRecommendation(
            String summary,
            List<String> suggestions) {
    }
}
