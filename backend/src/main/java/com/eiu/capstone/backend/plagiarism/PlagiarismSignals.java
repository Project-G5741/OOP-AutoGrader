package com.eiu.capstone.backend.plagiarism;

import java.util.List;

public record PlagiarismSignals(
        List<String> gitCommitHashes,
        String metadataCanonical,
        List<String> fileHashes) {

    public static PlagiarismSignals empty() {
        return new PlagiarismSignals(List.of(), "", List.of());
    }
}
