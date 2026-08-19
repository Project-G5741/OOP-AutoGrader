package com.eiu.capstone.backend.plagiarism;

public record GitCommitRecord(String hash, String authorName, String authorEmail, long timestampEpochSeconds) {}
