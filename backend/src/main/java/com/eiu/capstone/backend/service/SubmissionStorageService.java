package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;
import com.eiu.capstone.backend.utility.CompletableFutures;

@Service
public class SubmissionStorageService {

    private static final Pattern CHALLENGE_PATTERN =
            Pattern.compile("challenge[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBMISSION_ROOT_PATTERN =
            Pattern.compile("^(\\d+)_([a-z0-9_\\s]+)_lab_(\\d+)$", Pattern.CASE_INSENSITIVE);

    private static final String INVALID_STRUCTURE_MESSAGE =
            "Invalid folder structure. Expected root folder like 'IRN_StudentName_lab_1' with challenge folders named 'challenge_1' and only .mmd/.java files inside.";

    @Value("${app.storage.submission-base-dir}")
    private String baseDir;

    @Value("${app.grading.timing-log:false}")
    private boolean timingLog;

    private final JavaCompilerService javaCompilerService;
    private final ExecutorService compileExecutor;

    public SubmissionStorageService(JavaCompilerService javaCompilerService,
                                    @Qualifier("compileExecutor") ExecutorService compileExecutor) {
        this.javaCompilerService = javaCompilerService;
        this.compileExecutor = compileExecutor;
    }

    public static class ChallengeResult {
        public final String challengeName;
        public final Path folder;
        public final int classFileCount;
        public final String compileError;

        public ChallengeResult(String challengeName, Path folder, int classFileCount) {
            this(challengeName, folder, classFileCount, null);
        }

        public ChallengeResult(String challengeName, Path folder, int classFileCount, String compileError) {
            this.challengeName = challengeName;
            this.folder = folder;
            this.classFileCount = classFileCount;
            this.compileError = compileError;
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

    private record GroupedUpload(
            Map<String, List<MultipartFile>> javaByChallenge,
            Map<String, List<MultipartFile>> mmdByChallenge) {}

    public ProcessResult processUpload(String irn, String requestId, List<MultipartFile> files) {
        String irnFolderName = sanitize(irn);
        if (irnFolderName.isEmpty()) {
            throw new SubmissionProcessingException("Token did not contain a usable IRN claim");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new SubmissionProcessingException("A requestId is required to avoid folder collisions");
        }
        Path submissionFolder = Path.of(baseDir, irnFolderName, requestId);

        GroupedUpload grouped = validateAndGroup(files);

        Set<String> challengeKeys = new LinkedHashSet<>();
        challengeKeys.addAll(grouped.javaByChallenge().keySet());
        challengeKeys.addAll(grouped.mmdByChallenge().keySet());

        List<CompletableFuture<ChallengeResult>> futures = challengeKeys.stream()
                .map(challengeKey -> CompletableFuture.supplyAsync(
                        () -> processChallenge(
                                submissionFolder,
                                challengeKey,
                                grouped.javaByChallenge().getOrDefault(challengeKey, List.of())),
                        compileExecutor))
                .collect(Collectors.toList());

        List<ChallengeResult> results = CompletableFutures.joinAll(futures);
        return new ProcessResult(submissionFolder, results, Map.copyOf(grouped.mmdByChallenge()));
    }

    private GroupedUpload validateAndGroup(List<MultipartFile> files) {
        Map<String, List<MultipartFile>> javaByChallenge = new LinkedHashMap<>();
        Map<String, List<MultipartFile>> mmdByChallenge = new LinkedHashMap<>();
        String expectedRoot = null;

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                continue;
            }

            if (!isValidSubmissionPath(originalName)) {
                throw new SubmissionProcessingException(INVALID_STRUCTURE_MESSAGE);
            }

            String normalized = normalizePath(originalName);
            String[] segments = normalized.split("/");
            String rootFolder = segments[0];
            if (expectedRoot == null) {
                expectedRoot = rootFolder;
            } else if (!expectedRoot.equals(rootFolder)) {
                throw new SubmissionProcessingException(INVALID_STRUCTURE_MESSAGE);
            }

            String challengeKey = extractChallengeKey(originalName);
            if (challengeKey == null) {
                continue;
            }

            String lower = segments[segments.length - 1].toLowerCase();
            if (lower.endsWith(".mmd")) {
                mmdByChallenge.computeIfAbsent(challengeKey, k -> new ArrayList<>()).add(file);
            } else {
                javaByChallenge.computeIfAbsent(challengeKey, k -> new ArrayList<>()).add(file);
            }
        }

        return new GroupedUpload(javaByChallenge, mmdByChallenge);
    }

    private ChallengeResult processChallenge(Path submissionFolder, String challengeName, List<MultipartFile> files) {
        long start = System.currentTimeMillis();
        Path challengeFolder = submissionFolder.resolve(challengeName);
        try {
            return processChallengeWork(challengeFolder, challengeName, files, start);
        } catch (RuntimeException e) {
            return failedChallenge(challengeName, challengeFolder, start, challengeFolder, 0, 0, 0,
                    runtimeErrorMessage(e));
        }
    }

    private ChallengeResult processChallengeWork(Path challengeFolder,
                                                   String challengeName,
                                                   List<MultipartFile> files,
                                                   long start) {
        Path classesFolder = challengeFolder.resolve("classes");

        try {
            Files.createDirectories(classesFolder);
        } catch (IOException e) {
            return failedChallenge(challengeName, challengeFolder, start, challengeFolder, 0, 0, 0,
                    "Could not create folders for " + challengeName + ": " + e.getMessage());
        }

        if (files.isEmpty()) {
            logCompileTiming(challengeName, start, 0, 0, 0);
            return new ChallengeResult(challengeName, challengeFolder, 0);
        }

        long buildSourcesStart = System.currentTimeMillis();
        List<JavaFileObject> sources = new ArrayList<>();
        Set<String> seenSourcePaths = new HashSet<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".java")) {
                continue;
            }
            String sourcePath = challengeRelativeJavaPath(originalName, challengeName);
            if (seenSourcePaths.contains(sourcePath)) {
                return failedChallenge(challengeName, challengeFolder, start, challengeFolder, 0, 0, 0,
                        "Duplicate Java source path in challenge: " + sourcePath);
            }
            seenSourcePaths.add(sourcePath);
            try {
                sources.add(new MemorySourceJavaFileObject(sourcePath, file.getBytes()));
            } catch (IOException e) {
                return failedChallenge(challengeName, challengeFolder, start, challengeFolder, 0, 0, 0,
                        "Failed to read file: " + sourcePath);
            } catch (IllegalArgumentException e) {
                return failedChallenge(challengeName, challengeFolder, start, challengeFolder, 0, 0, 0,
                        e.getMessage());
            }
        }
        long buildSourcesMs = System.currentTimeMillis() - buildSourcesStart;

        if (sources.isEmpty()) {
            logCompileTiming(challengeName, start, buildSourcesMs, 0, 0);
            return new ChallengeResult(challengeName, challengeFolder, 0);
        }

        long javacStart = System.currentTimeMillis();
        try {
            javaCompilerService.compileSources(sources, classesFolder);
        } catch (RuntimeException e) {
            long javacMs = System.currentTimeMillis() - javacStart;
            return failedChallenge(challengeName, challengeFolder, start, classesFolder,
                    buildSourcesMs, javacMs, 0, runtimeErrorMessage(e));
        }
        long javacMs = System.currentTimeMillis() - javacStart;

        long countStart = System.currentTimeMillis();
        try {
            int classCount = countClassFiles(classesFolder);
            long countMs = System.currentTimeMillis() - countStart;
            logCompileTiming(challengeName, start, buildSourcesMs, javacMs, countMs);
            return new ChallengeResult(challengeName, challengeFolder, classCount);
        } catch (IOException e) {
            long countMs = System.currentTimeMillis() - countStart;
            return failedChallenge(challengeName, challengeFolder, start, classesFolder,
                    buildSourcesMs, javacMs, countMs,
                    "Failed to count class files: " + e.getMessage());
        }
    }

    private ChallengeResult failedChallenge(String challengeName,
                                              Path challengeFolder,
                                              long startMs,
                                              Path cleanupTarget,
                                              long buildSourcesMs,
                                              long javacMs,
                                              long countMs,
                                              String message) {
        deleteRecursively(cleanupTarget);
        logCompileTiming(challengeName, startMs, buildSourcesMs, javacMs, countMs);
        return new ChallengeResult(challengeName, challengeFolder, 0, message);
    }

    private static String runtimeErrorMessage(RuntimeException e) {
        if (e instanceof SubmissionProcessingException && e.getMessage() != null) {
            return e.getMessage();
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private void logCompileTiming(String challengeName,
                                  long startMs,
                                  long buildSourcesMs,
                                  long javacMs,
                                  long countMs) {
        if (!timingLog) {
            return;
        }
        long totalMs = System.currentTimeMillis() - startMs;
        System.out.printf(
                "compile_timing challenge=%s total_ms=%d build_sources_ms=%d javac_ms=%d count_ms=%d%n",
                challengeName, totalMs, buildSourcesMs, javacMs, countMs);
    }

    public void deleteFolder(Path folder) {
        deleteRecursively(folder);
    }

    static boolean isValidSubmissionPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String normalized = normalizePath(relativePath);
        String[] segments = normalized.split("/");
        if (segments.length < 3) {
            return false;
        }

        String rootFolder = segments[0];
        if (!SUBMISSION_ROOT_PATTERN.matcher(rootFolder).matches()) {
            return false;
        }

        for (int i = 1; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (segment.isBlank()) {
                return false;
            }
            if (!CHALLENGE_PATTERN.matcher(segment).matches()) {
                return false;
            }
        }

        String fileName = segments[segments.length - 1];
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mmd") || lower.endsWith(".java");
    }

    private String challengeRelativeJavaPath(String originalName, String challengeName) {
        String normalized = normalizePath(originalName);
        String[] segments = normalized.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            Matcher m = CHALLENGE_PATTERN.matcher(segments[i]);
            if (m.find() && ("challenge_" + m.group(1)).equals(challengeName)) {
                if (i + 1 >= segments.length) {
                    throw new IllegalArgumentException("Missing Java file name under " + challengeName);
                }
                return String.join("/", List.of(segments).subList(i + 1, segments.length));
            }
        }
        return fileNameOnly(originalName);
    }

    private String extractChallengeKey(String relativePath) {
        String normalized = normalizePath(relativePath);
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

    private static String normalizePath(String relativePath) {
        String normalized = relativePath.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
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

    private int countClassFiles(Path classesFolder) throws IOException {
        if (!Files.isDirectory(classesFolder)) {
            return 0;
        }
        try (var stream = Files.list(classesFolder)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .count();
        }
    }

    private void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
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
