package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;

@Service
public class SubmissionStorageService {

    private static final Pattern CHALLENGE_PATTERN =
            Pattern.compile("challenge[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);

    @Value("${app.storage.submission-base-dir}")
    private String baseDir;

    private final JavaCompilerService javaCompilerService;

    public SubmissionStorageService(JavaCompilerService javaCompilerService) {
        this.javaCompilerService = javaCompilerService;
    }

    /** Result for a single challenge subfolder. */
    public static class ChallengeResult {
        public final String challengeName;
        public final Path folder;
        public final int mmdFileCount;
        public final int classFileCount;

        public ChallengeResult(String challengeName, Path folder, int mmdFileCount, int classFileCount) {
            this.challengeName = challengeName;
            this.folder = folder;
            this.mmdFileCount = mmdFileCount;
            this.classFileCount = classFileCount;
        }
    }

    /** Overall result for the whole upload (may span several challenge folders). */
    public static class ProcessResult {
        /** The folder that should be deleted once grading is done — unique per upload request. */
        public final Path submissionFolder;
        public final List<ChallengeResult> challenges;

        public ProcessResult(Path submissionFolder, List<ChallengeResult> challenges) {
            this.submissionFolder = submissionFolder;
            this.challenges = challenges;
        }
    }

    /**
     * Expects each MultipartFile's originalFilename to carry the file's path *relative to
     * the dropped folder*, e.g. "Nguyen_Van_A_lab2/Nguyen_Van_A_challenge_1/Car.java" or just
     * "Nguyen_Van_A_challenge_1/Car.java" (see DropZone.jsx — it appends each file with its
     * webkitRelativePath as the multipart filename).
     *
     * Files are written under a folder unique to this specific upload request:
     *   <baseDir>/<irn>/<requestId>/challenge_<n>/mmd/       -> uploaded .mmd files, as-is
     *   <baseDir>/<irn>/<requestId>/challenge_<n>/classes/   -> .class files compiled from
     *                                                           that challenge's .java files only
     *
     * The requestId (a fresh UUID minted per upload — see SubmissionController) is what
     * prevents two overlapping submissions from the same student (e.g. a double-click, or
     * submitting Lab 2 and then Lab 3 before Lab 2 finishes grading) from colliding in the
     * same challenge_N folder. Without it, "challenge_1" would mean the same path regardless
     * of which lab or which submission it came from.
     *
     * Anything that isn't .mmd or .java, or that isn't nested inside a recognizable
     * challenge folder, is ignored.
     *
     * Does NOT delete anything afterward — see deleteFolder().
     */
    public ProcessResult processUpload(String irn, String requestId, List<MultipartFile> files) {
        String irnFolderName = sanitize(irn);
        if (irnFolderName.isEmpty()) {
            throw new SubmissionProcessingException("Token did not contain a usable IRN claim");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new SubmissionProcessingException("A requestId is required to avoid folder collisions");
        }
        Path submissionFolder = Path.of(baseDir, irnFolderName, requestId);

        // Group incoming files by the challenge folder they were dropped under.
        Map<String, List<MultipartFile>> byChallenge = new LinkedHashMap<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;

            String lower = originalName.toLowerCase();
            if (!lower.endsWith(".mmd") && !lower.endsWith(".java")) {
                continue; // ignore everything else, per spec
            }

            String challengeKey = extractChallengeKey(originalName);
            if (challengeKey == null) {
                continue; // couldn't place this file under any challenge folder — skip it
            }

            byChallenge.computeIfAbsent(challengeKey, k -> new ArrayList<>()).add(file);
        }

        List<ChallengeResult> results = new ArrayList<>();
        for (Map.Entry<String, List<MultipartFile>> entry : byChallenge.entrySet()) {
            results.add(processChallenge(submissionFolder, entry.getKey(), entry.getValue()));
        }

        return new ProcessResult(submissionFolder, results);
    }

    private ChallengeResult processChallenge(Path submissionFolder, String challengeName, List<MultipartFile> files) {
        Path challengeFolder = submissionFolder.resolve(challengeName);
        Path mmdFolder = challengeFolder.resolve("mmd");
        Path classesFolder = challengeFolder.resolve("classes");
        Path sourcesFolder = challengeFolder.resolve("_sources_tmp"); // temp holding area, deleted below

        try {
            Files.createDirectories(mmdFolder);
            Files.createDirectories(classesFolder);
            Files.createDirectories(sourcesFolder);
        } catch (IOException e) {
            throw new SubmissionProcessingException("Could not create folders for " + challengeName, e);
        }

        int mmdCount = 0;
        List<Path> javaSources = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = fileNameOnly(file.getOriginalFilename());
            String lower = fileName.toLowerCase();

            try {
                if (lower.endsWith(".mmd")) {
                    file.transferTo(mmdFolder.resolve(fileName));
                    mmdCount++;
                } else if (lower.endsWith(".java")) {
                    Path target = sourcesFolder.resolve(fileName);
                    file.transferTo(target);
                    javaSources.add(target);
                }
            } catch (IOException e) {
                throw new SubmissionProcessingException("Failed to save file: " + fileName, e);
            }
        }

        try {
            javaCompilerService.compile(javaSources, classesFolder);
        } 
        finally {
            deleteRecursively(sourcesFolder);
        }

        int classCount = countFiles(classesFolder, ".class");
        return new ChallengeResult(challengeName, challengeFolder, mmdCount, classCount);
    }

    /**
     * Deletes a folder tree — call with ProcessResult.submissionFolder once grading for
     * this whole upload is done. Because submissionFolder is unique per request (keyed by
     * requestId), this is always safe to call without risk of deleting another in-flight
     * submission's files, even from the same student. Nothing calls this automatically.
     */
    public void deleteFolder(Path folder) {
        deleteRecursively(folder);
    }

    /**
     * Looks through every path segment (except the filename itself) for one matching
     * "challenge[_-]<number>" (case-insensitive), e.g. "Nguyen_Van_A_challenge_1" -> "challenge_1".
     * Falls back to a sanitized version of the immediate parent folder name if no segment
     * matches the pattern, so unexpected-but-real folders still land somewhere instead of
     * being silently dropped. Returns null if the file has no parent folder at all.
     */
    private String extractChallengeKey(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String[] segments = normalized.split("/");
        if (segments.length < 2) {
            return null; // file wasn't inside any folder — can't tell which challenge it belongs to
        }

        for (int i = 0; i < segments.length - 1; i++) {
            Matcher m = CHALLENGE_PATTERN.matcher(segments[i]);
            if (m.find()) {
                return "challenge_" + m.group(1);
            }
        }

        // Fallback: use the immediate parent folder's sanitized name rather than dropping the file.
        String parent = sanitize(segments[segments.length - 2]);
        return parent.isEmpty() ? null : parent;
    }

    private String fileNameOnly(String relativeOrPlainName) {
        String normalized = relativeOrPlainName.replace('\\', '/');
        return Path.of(normalized).getFileName().toString();
    }

    /** Lowercases, strips diacritics (e.g. "Khá" -> "kha"), keeps only [a-z0-9_]. */
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
                            // best-effort cleanup
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}