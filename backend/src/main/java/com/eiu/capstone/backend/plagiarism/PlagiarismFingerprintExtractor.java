package com.eiu.capstone.backend.plagiarism;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

/**
 * Builds plagiarism signals from the uploaded multipart folder, including {@code .git}.
 */
public final class PlagiarismFingerprintExtractor {

    private PlagiarismFingerprintExtractor() {}

    public static PlagiarismSignals extract(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return PlagiarismSignals.empty();
        }
        List<String> fileHashes = new ArrayList<>();
        List<GitFile> gitFiles = new ArrayList<>();
        String gitConfigText = null;
        String gitReflogText = null;
        boolean otherGitFiles = false;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        for (MultipartFile file : files) {
            String relative = relativePath(file);
            if (relative.isEmpty()) {
                continue;
            }
            if (isGitPath(relative)) {
                String gitRelative = gitRelative(relative);
                gitFiles.add(new GitFile(gitRelative, file));
                if (GitHistoryReader.CONFIG_FILE.equals(gitRelative)) {
                    gitConfigText = readUtf8(file);
                } else if (GitHistoryReader.REFLOG_FILE.equals(gitRelative)) {
                    gitReflogText = readUtf8(file);
                } else if (!gitRelative.isBlank()) {
                    otherGitFiles = true;
                }
                continue;
            }
            if (isHashedSource(relative)) {
                try {
                    fileHashes.add(sha256(digest, file.getBytes()));
                } catch (IOException ignored) {
                    // skip unreadable source
                }
            }
        }
        fileHashes.sort(String::compareTo);

        GitHistory history = GitHistoryReader.fromConfigAndReflog(gitConfigText, gitReflogText);
        if (!history.hasCommits() && otherGitFiles) {
            history = readGitHistory(gitFiles);
        }
        List<String> commitHashes = history.commits().stream()
                .map(GitCommitRecord::hash)
                .toList();
        return new PlagiarismSignals(
                commitHashes,
                PlagiarismComparator.metadataCanonical(history),
                List.copyOf(fileHashes));
    }

    public static boolean isGitPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String[] segments = normalize(relativePath).split("/");
        for (int i = 1; i < segments.length; i++) {
            if (".git".equals(segments[i])) {
                return true;
            }
        }
        return false;
    }

    static String relativePath(MultipartFile file) {
        if (file == null) {
            return "";
        }
        String name = file.getOriginalFilename();
        return name == null ? "" : normalize(name);
    }

    private static boolean isHashedSource(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java") || lower.endsWith(".mmd");
    }

    private static String gitRelative(String relativePath) {
        String[] segments = normalize(relativePath).split("/");
        for (int i = 0; i < segments.length; i++) {
            if (".git".equals(segments[i])) {
                if (i + 1 >= segments.length) {
                    return "";
                }
                return String.join("/", Arrays.copyOfRange(segments, i + 1, segments.length));
            }
        }
        return "";
    }

    private static String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static GitHistory readGitHistory(List<GitFile> gitFiles) {
        Path temp = null;
        try {
            temp = Files.createTempDirectory("plagiarism-git-");
            Path gitDir = temp.resolve(".git");
            Files.createDirectories(gitDir);
            gitFiles.sort(Comparator.comparing(GitFile::relative));
            for (GitFile gitFile : gitFiles) {
                if (gitFile.relative().isBlank() || gitFile.relative().contains("..")) {
                    continue;
                }
                Path target = gitDir.resolve(gitFile.relative()).normalize();
                if (!target.startsWith(gitDir)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.write(target, gitFile.file().getBytes());
            }
            return GitHistoryReader.read(gitDir);
        } catch (IOException e) {
            return GitHistory.empty();
        } finally {
            deleteRecursively(temp);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static String sha256(MessageDigest digest, byte[] bytes) {
        digest.reset();
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private static String normalize(String path) {
        return path.replace('\\', '/').trim();
    }

    private record GitFile(String relative, MultipartFile file) {}
}
