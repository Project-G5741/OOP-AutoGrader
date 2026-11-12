package com.eiu.capstone.backend.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.eiu.capstone.backend.model.LabPrefix;

/** Score total: average of Official labs only. A missing Official score counts as 0. */
public final class GradeOverviewOfficialTotal {

    private GradeOverviewOfficialTotal() {}

    public static BigDecimal average(List<String> prefixes, List<BigDecimal> scores) {
        if (prefixes.size() != scores.size()) {
            throw new IllegalArgumentException("prefix and score lists must match");
        }
        int countedLabs = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < prefixes.size(); i++) {
            if (!LabPrefix.countsInScoreTotal(prefixes.get(i))) {
                continue;
            }
            countedLabs++;
            BigDecimal score = scores.get(i);
            sum = sum.add(score != null ? score : BigDecimal.ZERO);
        }
        if (countedLabs == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(countedLabs), 2, RoundingMode.DOWN);
    }

    /**
     * Average of Official labs where the pillar applies.
     * A null score means the pillar could not be read; the average is then null.
     * Callers pass 0 for an Official lab the student did not submit.
     */
    public static BigDecimal averageApplicable(List<String> prefixes,
                                               List<BigDecimal> scores,
                                               List<Boolean> applicable) {
        if (prefixes.size() != scores.size() || prefixes.size() != applicable.size()) {
            throw new IllegalArgumentException("prefix, score, and applicability lists must match");
        }
        int countedLabs = 0;
        BigDecimal sum = BigDecimal.ZERO;
        boolean unknown = false;
        for (int i = 0; i < prefixes.size(); i++) {
            if (!LabPrefix.countsInScoreTotal(prefixes.get(i)) || !Boolean.TRUE.equals(applicable.get(i))) {
                continue;
            }
            countedLabs++;
            BigDecimal score = scores.get(i);
            if (score == null) {
                unknown = true;
            } else {
                sum = sum.add(score);
            }
        }
        if (countedLabs == 0 || unknown) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(countedLabs), 2, RoundingMode.DOWN);
    }
}
