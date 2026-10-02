package com.eiu.capstone.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.desktop.pack.DesktopPackExportService;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;

@Service
@Profile("!desktop")
public class StudentDesktopPracticeBundleService {

    private static final String LAUNCHER_BAT = "OOP-AutoGrader-Practice.bat";
    private static final String README_ENTRY = "README.txt";
    private static final String RUBRIC_DIR = "rubric";

    private final DesktopPackExportService desktopPackExportService;
    private final TermService termService;
    private final StudentTermAccessService studentTermAccessService;
    private final Path backendJar;
    private final Path workerJar;
    private final Path uiDir;
    private final Path launcherDir;

    public StudentDesktopPracticeBundleService(
            DesktopPackExportService desktopPackExportService,
            TermService termService,
            StudentTermAccessService studentTermAccessService,
            @Value("${app.desktop.student-practice.backend-jar:}") String backendJarPath,
            @Value("${app.desktop.student-practice.worker-jar:}") String workerJarPath,
            @Value("${app.desktop.student-practice.ui-dir:}") String uiDirPath,
            @Value("${app.desktop.student-practice.launcher-dir:}") String launcherDirPath) {
        this.desktopPackExportService = desktopPackExportService;
        this.termService = termService;
        this.studentTermAccessService = studentTermAccessService;
        this.backendJar = resolveOptionalPath(backendJarPath);
        this.workerJar = resolveOptionalPath(workerJarPath);
        this.uiDir = resolveOptionalPath(uiDirPath);
        this.launcherDir = resolveOptionalPath(launcherDirPath);
    }

    public record BundleDownload(String filename, byte[] bytes) {}

    public BundleDownload buildForStudent(UserAccount user) {
        if (!studentTermAccessService.isInCurrentTerm(user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Offline practice download is only available in the current quarter");
        }
        Term current = termService.findCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No current quarter is configured"));
        byte[] pack = desktopPackExportService.exportTermPack(current.getId());
        String packEntry = RUBRIC_DIR + "/" + DesktopPackFileNames.termFilename(current.getId());
        try {
            byte[] zip = zipBundle(pack, packEntry);
            String filename = "OOP-AutoGrader-Practice-Q" + current.getTermNumber() + ".zip";
            return new BundleDownload(filename, zip);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build offline practice download", e);
        }
    }

    private byte[] zipBundle(byte[] termPack, String packEntry) throws IOException {
        assertBundleInputsExist();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            putFile(zip, "backend.jar", backendJar);
            putFile(zip, "worker.jar", workerJar);
            putDirectory(zip, "ui/dist-desktop", uiDir);
            putBytes(zip, packEntry, termPack);
            putBytes(zip, LAUNCHER_BAT, readClasspathResource("student-desktop/OOP-AutoGrader-Practice.bat"));
            putLauncherPublishRoot(zip, launcherDir);
            putBytes(zip, README_ENTRY, readClasspathResource("student-desktop/README.txt"));
            zip.putNextEntry(new ZipEntry("data/"));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }

    private void assertBundleInputsExist() {
        if (!Files.isRegularFile(backendJar)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Offline practice download is not configured (backend JAR missing)");
        }
        if (!Files.isRegularFile(workerJar)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Offline practice download is not configured (worker JAR missing)");
        }
        if (!Files.isDirectory(uiDir)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Offline practice download is not configured (desktop UI build missing)");
        }
    }

    private static Path resolveOptionalPath(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Path.of("");
        }
        return Path.of(raw.trim()).toAbsolutePath().normalize();
    }

    private static void putLauncherPublishRoot(ZipOutputStream zip, Path publishDir) throws IOException {
        if (publishDir == null || !Files.isDirectory(publishDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(publishDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                if (path.toString().endsWith(".pdb")) {
                    return;
                }
                try {
                    String relative = publishDir.relativize(path).toString().replace('\\', '/');
                    putFile(zip, relative, path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static void putFile(ZipOutputStream zip, String entryName, Path file) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private static void putBytes(ZipOutputStream zip, String entryName, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content);
        zip.closeEntry();
    }

    private static void putDirectory(ZipOutputStream zip, String prefix, Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    String entryName = prefix + "/" + relative;
                    putFile(zip, entryName, path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static byte[] readClasspathResource(String classpath) throws IOException {
        try (InputStream stream = StudentDesktopPracticeBundleService.class.getClassLoader()
                .getResourceAsStream(classpath)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + classpath);
            }
            return stream.readAllBytes();
        }
    }
}