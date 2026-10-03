package com.eiu.capstone.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;

/**
 * Serves the offline practice folder as a prebuilt runtime-only zip (no rubric pack).
 * Fixed paths only ({@code target/…} locally, cwd next to {@code app.jar} in Docker).
 * No runtime env — image packaging is in {@code DEPLOY_RENDER.md}.
 */
@Service
@Profile("!desktop")
public class StudentDesktopPracticeBundleService {

    /** Local Maven / assemble output (cwd = {@code backend/}). */
    static final Path LOCAL_PREBUILT_ZIP = Path.of("target", "OOP-AutoGrader-Practice.zip");
    /** Docker/Render layout next to {@code app.jar} (cwd = {@code /app}). */
    static final Path DEPLOY_PREBUILT_ZIP = Path.of("OOP-AutoGrader-Practice.zip");

    private final TermService termService;
    private final StudentTermAccessService studentTermAccessService;
    private final Path prebuiltZipOverride;

    @Autowired
    public StudentDesktopPracticeBundleService(
            TermService termService,
            StudentTermAccessService studentTermAccessService) {
        this(termService, studentTermAccessService, null);
    }

    /** Test constructor — pass an explicit zip path; production resolves on each download. */
    public StudentDesktopPracticeBundleService(
            TermService termService,
            StudentTermAccessService studentTermAccessService,
            Path prebuiltZipOverride) {
        this.termService = termService;
        this.studentTermAccessService = studentTermAccessService;
        this.prebuiltZipOverride = prebuiltZipOverride == null
                ? null
                : prebuiltZipOverride.toAbsolutePath().normalize();
    }

    public record BundleDownload(String filename, Path zipFile) {}

    public BundleDownload buildForStudent(UserAccount user) {
        if (!studentTermAccessService.isInCurrentTerm(user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Offline practice download is only available in the current quarter");
        }
        Term current = termService.findCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No current quarter is configured"));
        Path zip = resolvePrebuiltZip();
        if (!Files.isRegularFile(zip)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Offline practice download is not configured (prebuilt zip missing)");
        }
        String filename = "OOP-AutoGrader-Practice-Q" + current.getTermNumber() + ".zip";
        return new BundleDownload(filename, zip);
    }

    Path resolvePrebuiltZip() {
        if (prebuiltZipOverride != null) {
            return prebuiltZipOverride;
        }
        for (Path candidate : List.of(LOCAL_PREBUILT_ZIP, DEPLOY_PREBUILT_ZIP)) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(absolute)) {
                return absolute;
            }
        }
        return LOCAL_PREBUILT_ZIP.toAbsolutePath().normalize();
    }
}
