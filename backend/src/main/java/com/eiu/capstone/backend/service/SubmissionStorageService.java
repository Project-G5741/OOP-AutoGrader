package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.utility.CompletableFutures;

@Service
public class SubmissionStorageService {

    private static final Pattern CHALLENGE_PATTERN =
            Pattern.compile("challenge[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);

    @Value("${app.storage.submission-base-dir}")
    private String baseDir;

    private final JavaCompilerService javaCompilerService;
    private final ExecutorService gradingExecutor;

    public SubmissionStorageService(JavaCompilerService javaCompilerService,
                                    ExecutorService gradingExecutor) {
        this.javaCompilerService = javaCompilerService;
        this.gradingExecutor = gradingExecutor;
    }

    public static class ChallengeResult {
        public final String challengeName;
        public final Path folder;
        public final int classFileCount;

        public ChallengeResult(String challengeName, Path folder, int classFileCount) {
            this.challengeName = challengeName;
            this.folder = folder;
            this.classFileCount = classFileCount;
        }
    }

    public static class ProcessResult {
        public final Path submissionFolder;
        public final List<ChallengeResult> challenges;
        public final Map<String, List<MultipartFile>> mmdByChallenge;

        public ProcessResult(Path submissionFolder,
                             List<ChallengeResult> challenges,
                             Map<String, List<MultipartFile>> mmdByChallenge) {
            this.submissionFolder = submissionFolder;
            this.challenges = challenges;
            this.mmdByChallenge = mmdByChallenge;
        }
    }

    public ProcessResult processUpload(String irn, String requestId, List<MultipartFile> files) {
        String irnFolderName = sanitize(irn);
        if (irnFolderName.isEmpty()) {
            throw new SubmissionProcessingException("Token did not contain a usable IRN claim");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new SubmissionProcessingException("A requestId is required to avoid folder collisions");
        }
        Path submissionFolder = Path.of(baseDir, irnFolderName, requestId);

        Map<String, List<MultipartFile>> byChallenge = new java.util.LinkedHashMap<>();
        Map<String, List<MultipartFile>> mmdByChallenge = new java.util.LinkedHashMap<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;

            String lower = originalName.toLowerCase();
            if (!lower.endsWith(".mmd") && !lower.endsWith(".java")) {
                continue;
            }

            String challengeKey = extractChallengeKey(originalName);
            if (challengeKey == null) {
                continue;
            }

            if (lower.endsWith(".mmd")) {
                mmdByChallenge.computeIfAbsent(challengeKey, k -> new ArrayList<>()).add(file);
            } else {
                byChallenge.computeIfAbsent(challengeKey, k -> new ArrayList<>()).add(file);
            }
        }

        Set<String> challengeKeys = new LinkedHashSet<>();
        challengeKeys.addAll(byChallenge.keySet());
        challengeKeys.addAll(mmdByChallenge.keySet());

        try {
            List<CompletableFuture<ChallengeResult>> futures = challengeKeys.stream()
                    .map(challengeKey -> CompletableFuture.supplyAsync(
                            () -> processChallenge(
                                    submissionFolder,
                                    challengeKey,
                                    byChallenge.getOrDefault(challengeKey, List.of())),
                            gradingExecutor))
                    .collect(Collectors.toList());

            List<ChallengeResult> results = CompletableFutures.joinAll(futures);
            return new ProcessResult(submissionFolder, results, Map.copyOf(mmdByChallenge));
        } catch (RuntimeException e) {
            deleteFolder(submissionFolder);
            throw e;
        }
    }

    private ChallengeResult processChallenge(Path submissionFolder, String challengeName, List<MultipartFile> files) {
        Path challengeFolder = submissionFolder.resolve(challengeName);
        Path classesFolder = challengeFolder.resolve("classes");
        Path sourcesFolder = challengeFolder.resolve("_sources_tmp");

        try {
            Files.createDirectories(classesFolder);
            if (!files.isEmpty()) {
                Files.createDirectories(sourcesFolder);
            }
        } catch (IOException e) {
            throw new SubmissionProcessingException("Could not create folders for " + challengeName, e);
        }

        if (files.isEmpty()) {
            return new ChallengeResult(challengeName, challengeFolder, 0);
        }

        List<Path> javaSources = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = fileNameOnly(file.getOriginalFilename());
            if (!fileName.toLowerCase().endsWith(".java")) {
                continue;
            }
            try {
                Path target = sourcesFolder.resolve(fileName);
                file.transferTo(target);
                javaSources.add(target);
            } catch (IOException e) {
                throw new SubmissionProcessingException("Failed to save file: " + fileName, e);
            }
        }

        try {
            javaCompilerService.compile(javaSources, classesFolder);
        } catch (RuntimeException e) {
            deleteRecursively(challengeFolder);
            throw e;
        } finally {
            deleteRecursively(sourcesFolder);
        }

        int classCount = countFiles(classesFolder, ".class");
        return new ChallengeResult(challengeName, challengeFolder, classCount);
    }

    public void deleteFolder(Path folder) {
        deleteRecursively(folder);
    }

    private String extractChallengeKey(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String[] segments = normalized.split("/");
        if (segments.length < 2) {
            return null;
        }

        for (int i = 0; i < segments.length - 1; i++) {
            Matcher m = CHALLENGE_PATTERN.matcher(segments[i]);
            if (m.find()) {
                return "challenge_" + m.group(1);
            }
        }

        String parent = sanitize(segments[segments.length - 2]);
        return parent.isEmpty() ? null : parent;
    }

    private String fileNameOnly(String relativeOrPlainName) {
        String normalized = relativeOrPlainName.replace('\\', '/');
        return Path.of(normalized).getFileName().toString();
    }

    private String sanitize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private int countFiles(Path dir, String suffix) {
        try (var stream = Files.walk(dir)) {
            return (int) stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(suffix))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private void deleteRecursively(Path path) {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
