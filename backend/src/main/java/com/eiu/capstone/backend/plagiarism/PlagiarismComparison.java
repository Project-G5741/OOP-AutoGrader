package com.eiu.capstone.backend.plagiarism;

import java.math.BigDecimal;

public record PlagiarismComparison(
        boolean gitMatch,
        boolean metadataMatch,
        BigDecimal hashSimilarity,
        boolean flagged) {}
