package com.eiu.capstone.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves the offline lecturer Bulk folder as a prebuilt zip.
 * Fixed locations only — no request-time URL. Assemble writes
 * {@code backend/target/OOP-AutoGrader-LecturerBulk.zip}. The image copies the
 * same filename from build-arg {@code LECTURER_BULK_BUNDLE_URL}.
 */
@Service
@Profile("!desktop & !desktop-lecturer")
public class LecturerDesktopBulkBundleService {

    static final String DOWNLOAD_FILENAME = "OOP-AutoGrader-LecturerBulk.zip";

    /** Local Maven / assemble output (cwd = {@code backend/}). */
    static final Path LOCAL_PREBUILT_ZIP = Path.of("target", DOWNLOAD_FILENAME);
    /** Docker/Render layout next to {@code app.jar} (cwd = {@code /app}). */
    static final Path DEPLOY_PREBUILT_ZIP = Path.of(DOWNLOAD_FILENAME);

    private final Path prebuiltZipOverride;

    public LecturerDesktopBulkBundleService() {
        this(null);
    }

    /** Test constructor — pass an explicit zip path; production resolves on each download. */
    public LecturerDesktopBulkBundleService(Path prebuiltZipOverride) {
        this.prebuiltZipOverride = prebuiltZipOverride == null
                ? null
                : prebuiltZipOverride.toAbsolutePath().normalize();
    }

    public record BundleDownload(String filename, Path zipFile) {}

    public BundleDownload buildForLecturer() {
        Path zip = resolvePrebuiltZip();
        if (!Files.isRegularFile(zip)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Offline Bulk download is not configured (prebuilt zip missing)");
        }
        return new BundleDownload(DOWNLOAD_FILENAME, zip);
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
