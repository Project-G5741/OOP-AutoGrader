package com.eiu.capstone.backend.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFile;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSigningKeys;
import com.eiu.capstone.backend.repository.TermRepository;

@Service
@Profile("desktop")
public class DesktopPackBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DesktopPackBootstrapService.class);

    private final Path installDir;
    private final Path rubricDir;
    private final Path fingerprintFile;
    private final DesktopPackCrypto crypto;
    private final DesktopPackSerializer serializer;
    private final DesktopPackSigningKeys signingKeys;
    private final DesktopPackImportService importService;
    private final DesktopLocalUserService localUserService;
    private final TermRepository termRepository;

    public DesktopPackBootstrapService(
            @Value("${app.desktop.install-dir}") String installDir,
            @Value("${app.desktop.pack-dir}") String packDir,
            DesktopPackSerializer serializer,
            DesktopPackSigningKeys signingKeys,
            DesktopPackImportService importService,
            DesktopLocalUserService localUserService,
            TermRepository termRepository) {
        this.installDir = Path.of(installDir).toAbsolutePath().normalize();
        this.rubricDir = Path.of(packDir).toAbsolutePath().normalize();
        this.fingerprintFile = this.installDir.resolve("data").resolve("desktop-pack-fingerprint.txt");
        this.crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
        this.serializer = serializer;
        this.signingKeys = signingKeys;
        this.importService = importService;
        this.localUserService = localUserService;
        this.termRepository = termRepository;
    }

    public BootstrapResult bootstrap() {
        localUserService.ensureLocalStudent(termRepository.findCurrent().orElse(null));
        if (!Files.isDirectory(rubricDir)) {
            clearPracticeWhenNoPacks();
            return BootstrapResult.missing("Practice rubric folder not found. Import a pack and restart.");
        }
        try {
            List<Path> termPacks = listPackFiles(DesktopPackFileNames.PackKind.TERM);
            List<Path> labPacks = listPackFiles(DesktopPackFileNames.PackKind.LAB);
            if (termPacks.isEmpty() && labPacks.isEmpty()) {
                clearPracticeWhenNoPacks();
                return BootstrapResult.missing(
                        "No practice packs found. Import a Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack file and restart.");
            }
            String fingerprint = computeFingerprint(termPacks, labPacks);
            if (fingerprint.equals(readFingerprint())) {
                String version = readDisplayVersion(termPacks, labPacks);
                return BootstrapResult.ready(version);
            }
            boolean termImported = false;
            if (!termPacks.isEmpty()) {
                Path termPath = selectTermPackToApply(termPacks);
                DesktopPackInnerPayload payload = readPayload(termPath);
                importService.importTermPack(payload);
                termImported = true;
            }
            labPacks.sort(Comparator.comparing(path -> path.getFileName().toString()));
            int labPacksImported = 0;
            for (Path labPath : labPacks) {
                try {
                    DesktopPackInnerPayload payload = readPayload(labPath);
                    importService.importLabPack(payload);
                    labPacksImported++;
                } catch (Exception ex) {
                    log.warn("Skipping lab pack {}: {}", labPath.getFileName(),
                            ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                }
            }
            if (!termImported && labPacksImported == 0 && !labPacks.isEmpty()) {
                return BootstrapResult.failed("Could not import any lab practice packs from rubric/");
            }
            localUserService.ensureLocalStudent(termRepository.findCurrent().orElse(null));
            writeFingerprint(fingerprint);
            String version = readDisplayVersion(termPacks, labPacks);
            return BootstrapResult.ready(version);
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Desktop pack bootstrap failed";
            return BootstrapResult.failed(message);
        }
    }

    private void clearPracticeWhenNoPacks() {
        importService.wipePracticeData();
        try {
            clearImportFingerprint();
        } catch (IOException ex) {
            log.warn("Could not clear desktop pack fingerprint after empty rubric wipe: {}",
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
        localUserService.ensureLocalStudent(null);
    }

    public void clearImportFingerprint() throws IOException {
        Files.deleteIfExists(fingerprintFile);
    }

    /**
     * Keeps a single quarter pack on disk so bootstrap does not load a stale {@code Rubric_*_Q*.agpack}.
     */
    public void deleteOtherTermPacks(String keepFilename) throws IOException {
        if (keepFilename == null || keepFilename.isBlank()) {
            return;
        }
        if (!Files.isDirectory(rubricDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(rubricDir)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                if (name.equalsIgnoreCase(keepFilename)) {
                    continue;
                }
                try {
                    DesktopPackFileNames.ParsedFilename parsed = DesktopPackFileNames.parse(name);
                    if (parsed.kind() == DesktopPackFileNames.PackKind.TERM) {
                        Files.deleteIfExists(path);
                    }
                } catch (Exception ignored) {
                    // leave non-pack files untouched
                }
            }
        }
    }

    /**
     * Removes single-lab packs from {@code rubric/} so a UI term import alone defines the lab set.
     * Not used on bootstrap for manually placed packs (those may overlay after term apply).
     */
    public void deleteLabPacks() throws IOException {
        if (!Files.isDirectory(rubricDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(rubricDir)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                try {
                    DesktopPackFileNames.ParsedFilename parsed =
                            DesktopPackFileNames.parse(path.getFileName().toString());
                    if (parsed.kind() == DesktopPackFileNames.PackKind.LAB) {
                        Files.deleteIfExists(path);
                    }
                } catch (Exception ignored) {
                    // leave non-pack files untouched
                }
            }
        }
    }

    private Path selectTermPackToApply(List<Path> termPacks) throws IOException {
        return termPacks.stream()
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }))
                .orElse(termPacks.get(0));
    }

    public Path rubricDirectory() {
        return rubricDir;
    }

    private List<Path> listPackFiles(DesktopPackFileNames.PackKind kind) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.list(rubricDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    DesktopPackFileNames.ParsedFilename parsed = DesktopPackFileNames.parse(path.getFileName().toString());
                    if (parsed.kind() == kind) {
                        paths.add(path);
                    }
                } catch (Exception ignored) {
                    // skip invalid names
                }
            });
        }
        paths.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return paths;
    }

    private DesktopPackInnerPayload readPayload(Path path) throws Exception {
        byte[] raw = Files.readAllBytes(path);
        DesktopPackFile file = crypto.deserializeFile(raw);
        byte[] innerJson = crypto.openPack(signingKeys.verificationPublicKey(), file);
        return serializer.fromJson(innerJson);
    }

    private String readDisplayVersion(List<Path> termPacks, List<Path> labPacks) {
        try {
            if (!termPacks.isEmpty()) {
                Path termPath = selectTermPackToApply(termPacks);
                byte[] raw = Files.readAllBytes(termPath);
                DesktopPackFile file = crypto.deserializeFile(raw);
                return file.manifest().packVersion();
            }
            if (!labPacks.isEmpty()) {
                byte[] raw = Files.readAllBytes(labPacks.get(0));
                DesktopPackFile file = crypto.deserializeFile(raw);
                return file.manifest().packVersion();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "local";
    }

    private String computeFingerprint(List<Path> termPacks, List<Path> labPacks) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Path> all = new ArrayList<>(termPacks);
        all.addAll(labPacks);
        all.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path path : all) {
            digest.update(path.getFileName().toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(path));
            digest.update((byte) 1);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String readFingerprint() {
        try {
            if (!Files.isRegularFile(fingerprintFile)) {
                return "";
            }
            return Files.readString(fingerprintFile, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private void writeFingerprint(String fingerprint) throws IOException {
        Files.createDirectories(fingerprintFile.getParent());
        Files.writeString(fingerprintFile, fingerprint, StandardCharsets.UTF_8);
    }

    public record BootstrapResult(boolean ready, boolean packMissing, String packVersion, String error) {
        static BootstrapResult ready(String version) {
            return new BootstrapResult(true, false, version, null);
        }

        static BootstrapResult missing(String message) {
            return new BootstrapResult(false, true, null, message);
        }

        static BootstrapResult failed(String message) {
            return new BootstrapResult(false, false, null, message);
        }
    }
}
