package com.eiu.capstone.backend.plagiarism;

import java.util.List;

public record GitHistory(String userName, String userEmail, List<GitCommitRecord> commits) {

    public static GitHistory empty() {
        return new GitHistory("", "", List.of());
    }

    public boolean hasCommits() {
        return commits != null && !commits.isEmpty();
    }
}
