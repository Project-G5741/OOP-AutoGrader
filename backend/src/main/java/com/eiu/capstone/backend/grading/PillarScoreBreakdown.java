package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;

public record PillarScoreBreakdown(
        BigDecimal classPillar,
        BigDecimal mmdPillar,
        BigDecimal testcasePillar,
        BigDecimal total,
        boolean classApplicable,
        boolean mmdApplicable,
        boolean testcaseApplicable) {

    /** Legacy ctor — Declaration always applicable. */
    public PillarScoreBreakdown(
            BigDecimal classPillar,
            BigDecimal mmdPillar,
            BigDecimal testcasePillar,
            BigDecimal total,
            boolean mmdApplicable,
            boolean testcaseApplicable) {
        this(classPillar, mmdPillar, testcasePillar, total, true, mmdApplicable, testcaseApplicable);
    }
}
