package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;

public record PillarScoreBreakdown(
        BigDecimal classPillar,
        BigDecimal mmdPillar,
        BigDecimal testcasePillar,
        BigDecimal total) {}
