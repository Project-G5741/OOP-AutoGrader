package com.eiu.capstone.backend.desktop.bundle;

import java.nio.file.Path;

/**
 * Fixed locations for prebuilt offline install zips (assemble output or deploy prefetch).
 */
public final class DesktopBundlePaths {

    public static final String PRACTICE_FILENAME = "OOP-AutoGrader-Practice.zip";
    public static final String LECTURER_BULK_FILENAME = "OOP-AutoGrader-LecturerBulk.zip";

    /** Local Maven / assemble output (cwd = {@code backend/}). */
    public static final Path PRACTICE_LOCAL = Path.of("target", PRACTICE_FILENAME);
    public static final Path LECTURER_BULK_LOCAL = Path.of("target", LECTURER_BULK_FILENAME);

    /** Docker / cloud layout next to {@code app.jar} (cwd = {@code /app}). */
    public static final Path PRACTICE_DEPLOY = Path.of(PRACTICE_FILENAME);
    public static final Path LECTURER_BULK_DEPLOY = Path.of(LECTURER_BULK_FILENAME);

    private DesktopBundlePaths() {}
}