package com.eiu.capstone.backend.ai;

import java.util.ArrayList;
import java.util.List;

/** Token-budget thresholds: warn at 50% and 80%; hard-stop at 90%. */
public final class AiTokenBudget {

    public static final double WARN_50 = 0.50;
    public static final double WARN_80 = 0.80;
    public static final double STOP_90 = 0.90;

    private AiTokenBudget() {}

    public static double usageRatio(long tokensUsed, long tokenBudget) {
        if (tokenBudget <= 0) {
            return 1.0;
        }
        return (double) tokensUsed / (double) tokenBudget;
    }

    public static int usagePercent(long tokensUsed, long tokenBudget) {
        return (int) Math.min(100, Math.round(usageRatio(tokensUsed, tokenBudget) * 100.0));
    }

    public static boolean isExhausted(long tokensUsed, long tokenBudget) {
        return usageRatio(tokensUsed, tokenBudget) >= STOP_90;
    }

    /**
     * Marks newly crossed thresholds on the credential and returns alert labels
     * ({@code "50%"}, {@code "80%"}, {@code "90%"}) for the student UI.
     */
    public static List<String> applyUsageAndCollectAlerts(AiStudentCredential credential, long addedTokens) {
        if (addedTokens > 0) {
            credential.setTokensUsed(credential.getTokensUsed() + addedTokens);
        }
        double ratio = usageRatio(credential.getTokensUsed(), credential.getTokenBudget());
        List<String> alerts = new ArrayList<>(3);
        if (ratio >= WARN_50 && !credential.isNotified50()) {
            credential.setNotified50(true);
            alerts.add("50%");
        }
        if (ratio >= WARN_80 && !credential.isNotified80()) {
            credential.setNotified80(true);
            alerts.add("80%");
        }
        if (ratio >= STOP_90 && !credential.isNotified90()) {
            credential.setNotified90(true);
            alerts.add("90%");
        }
        return alerts;
    }
}
