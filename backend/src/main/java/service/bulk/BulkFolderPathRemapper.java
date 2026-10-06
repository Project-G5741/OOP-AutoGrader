package com.eiu.capstone.backend.service.bulk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rewrites bulk Main-folder multipart paths into the single-student Lab shape expected by
 * {@code SubmissionStorageService}: {@code IRN_Name/challenge_n/...}.
 *
 * <ul>
 *   <li>LAB — strips an optional shared Main prefix; paths already under challenge folders stay.</li>
 *   <li>EXAM — strips Main prefix and inserts {@code challenge_1} under the student root.</li>
 * </ul>
 */
public final class BulkFolderPathRemapper {

    private static final Pattern SUBMISSION_ROOT_PATTERN =
            Pattern.compile("^(\\d+)_([a-z0-9_\\s]+)(_.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHALLENGE_PATTERN =
            Pattern.compile("^challenge[_-]?\\d+$", Pattern.CASE_INSENSITIVE);

    private BulkFolderPathRemapper() {}

    public static List<MultipartFile> remap(List<MultipartFile> files, BulkGradeMode mode) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }
        if (mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required (LAB or EXAM)");
        }

        String sharedTop = sharedTopSegment(files);
        boolean stripMain = sharedTop != null && !SUBMISSION_ROOT_PATTERN.matcher(sharedTop).matches();

        List<MultipartFile> remapped = new ArrayList<>(files.size());
        String studentRoot = null;
        for (MultipartFile file : files) {
            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) {
                continue;
            }
            String normalized = normalize(original);
            String[] segments = normalized.split("/");
            if (segments.length == 0) {
                continue;
            }
            int start = 0;
            if (stripMain && segments[0].equals(sharedTop)) {
                start = 1;
            }
            if (start >= segments.length) {
                continue;
            }
            String root = segments[start];
            if (!SUBMISSION_ROOT_PATTERN.matcher(root).matches()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Expected student folder IRN_Name, got: " + root);
            }
            if (studentRoot == null) {
                studentRoot = root;
            } else if (!studentRoot.equals(root)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Bulk grade accepts one student folder per request");
            }

            List<String> rest = new ArrayList<>();
            for (int i = start + 1; i < segments.length; i++) {
                rest.add(segments[i]);
            }
            if (rest.isEmpty()) {
                continue;
            }

            String rewritten;
            if (mode == BulkGradeMode.EXAM) {
                if (CHALLENGE_PATTERN.matcher(rest.get(0)).matches()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Exam mode expects files directly under IRN_Name (no challenge_n folders)");
                }
                rewritten = root + "/challenge_1/" + String.join("/", rest);
            } else {
                if (!CHALLENGE_PATTERN.matcher(rest.get(0)).matches()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Lab mode expects challenge_n folders under IRN_Name");
                }
                rewritten = root + "/" + String.join("/", rest);
            }
            remapped.add(new RenamedMultipartFile(file, rewritten));
        }

        if (remapped.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No .java or .mmd files to grade");
        }
        return remapped;
    }

    public static String extractStudentFolder(List<MultipartFile> remappedFiles) {
        for (MultipartFile file : remappedFiles) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank()) {
                continue;
            }
            String[] segments = normalize(name).split("/");
            if (segments.length > 0 && SUBMISSION_ROOT_PATTERN.matcher(segments[0]).matches()) {
                return segments[0];
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not resolve student folder from upload");
    }

    public static String extractIrn(String studentFolder) {
        if (studentFolder == null) {
            return "";
        }
        int underscore = studentFolder.indexOf('_');
        return underscore > 0 ? studentFolder.substring(0, underscore) : studentFolder;
    }

    private static String sharedTopSegment(List<MultipartFile> files) {
        String top = null;
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank()) {
                continue;
            }
            String[] segments = normalize(name).split("/");
            if (segments.length == 0) {
                continue;
            }
            if (top == null) {
                top = segments[0];
            } else if (!top.equals(segments[0])) {
                return null;
            }
        }
        return top;
    }

    private static String normalize(String relativePath) {
        return relativePath.replace('\\', '/').replaceAll("/+", "/").replaceAll("^/+", "");
    }
}
