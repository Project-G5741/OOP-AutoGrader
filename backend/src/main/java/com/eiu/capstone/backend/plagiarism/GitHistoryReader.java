package com.eiu.capstone.backend.plagiarism;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads ordered commit history from a reconstructed {@code .git} directory.
 */
public final class GitHistoryReader {

    private static final Pattern REFLOG_LINE = Pattern.compile(
            "^[0-9a-f]{4,40}\\s+([0-9a-f]{7,40})\\s+(.+?)\\s+<([^>]+)>\\s+(\\d+)\\s+");
    private static final Pattern CONFIG_NAME = Pattern.compile("(?im)^\\s*name\\s*=\\s*(.+)$");
    private static final Pattern CONFIG_EMAIL = Pattern.compile("(?im)^\\s*email\\s*=\\s*(.+)$");

    private GitHistoryReader() {}

    public static GitHistory read(Path gitDir) {
        if (gitDir == null || !Files.isDirectory(gitDir)) {
            return GitHistory.empty();
        }
        String userName = "";
        String userEmail = "";
        Path config = gitDir.resolve("config");
        if (Files.isRegularFile(config)) {
            try {
                String text = Files.readString(config, StandardCharsets.UTF_8);
                Matcher name = CONFIG_NAME.matcher(text);
                if (name.find()) {
                    userName = name.group(1).trim();
                }
                Matcher email = CONFIG_EMAIL.matcher(text);
                if (email.find()) {
                    userEmail = email.group(1).trim();
                }
            } catch (IOException ignored) {
                // keep empty git user
            }
        }

        List<GitCommitRecord> commits = readReflog(gitDir.resolve("logs").resolve("HEAD"));
        if (commits.isEmpty()) {
            commits = readGitLog(gitDir);
        }
        return new GitHistory(userName, userEmail, List.copyOf(commits));
    }

    static List<GitCommitRecord> parseReflog(String text) {
        List<GitCommitRecord> commits = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return commits;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = REFLOG_LINE.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            commits.add(new GitCommitRecord(
                    matcher.group(1).toLowerCase(Locale.ROOT),
                    matcher.group(2).trim(),
                    matcher.group(3).trim().toLowerCase(Locale.ROOT),
                    parseLong(matcher.group(4))));
        }
        return commits;
    }

    private static List<GitCommitRecord> readReflog(Path headLog) {
        if (!Files.isRegularFile(headLog)) {
            return List.of();
        }
        try {
            return parseReflog(Files.readString(headLog, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<GitCommitRecord> readGitLog(Path gitDir) {
        try {
            Process process = new ProcessBuilder(
                    "git", "--git-dir=" + gitDir.toAbsolutePath(),
                    "log", "--reverse", "--format=%H%x09%an%x09%ae%x09%at")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return List.of();
            }
            if (process.exitValue() != 0) {
                return List.of();
            }
            List<GitCommitRecord> commits = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t", 4);
                    if (parts.length < 4) {
                        continue;
                    }
                    commits.add(new GitCommitRecord(
                            parts[0].trim().toLowerCase(Locale.ROOT),
                            parts[1].trim(),
                            parts[2].trim().toLowerCase(Locale.ROOT),
                            parseLong(parts[3].trim())));
                }
            }
            return commits;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
