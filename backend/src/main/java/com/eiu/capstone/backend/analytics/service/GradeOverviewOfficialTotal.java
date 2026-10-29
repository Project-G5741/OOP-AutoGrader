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
}
