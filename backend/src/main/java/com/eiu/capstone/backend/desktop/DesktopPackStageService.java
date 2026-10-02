package com.eiu.capstone.backend.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackIngest;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSigningKeys;

@Service
@Profile("desktop")
public class DesktopPackStageService {

    private final DesktopPackBootstrapService bootstrapService;
    private final DesktopPackImportService importService;
    private final DesktopPackSerializer serializer;
    private final DesktopPackSigningKeys signingKeys;
    private final DesktopPackCrypto crypto;

    public DesktopPackStageService(
            DesktopPackBootstrapService bootstrapService,
            DesktopPackImportService importService,
            DesktopPackSerializer serializer,
            DesktopPackSigningKeys signingKeys) {
        this.bootstrapService = bootstrapService;
        this.importService = importService;
        this.serializer = serializer;
        this.signingKeys = signingKeys;
        this.crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
    }

    public StageResult stageUpload(MultipartFile file, boolean confirmReplace) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a practice pack file to import");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the selected file");
        }
        DesktopPackIngest.OpenedPack opened = DesktopPackIngest.open(
                bytes,
                file.getOriginalFilename(),
                crypto,
                signingKeys.verificationPublicKey(),
                serializer);
        DesktopPackFileNames.ParsedFilename parsed = opened.parsed();
        DesktopPackInnerPayload inner = opened.inner();
        if (parsed.kind() == DesktopPackFileNames.PackKind.LAB) {
            List<DesktopPackImportService.LabNameConflict> conflicts = importService.findLabNameConflicts(inner);
            if (!conflicts.isEmpty() && !confirmReplace) {
                throw new LabNameConflictException(conflicts);
            }
        }
        try {
            Path rubricDir = bootstrapService.rubricDirectory();
            Files.createDirectories(rubricDir);
            if (parsed.kind() == DesktopPackFileNames.PackKind.TERM) {
                bootstrapService.deleteOtherTermPacks(parsed.filename());
                bootstrapService.deleteLabPacks();
            }
            Path target = rubricDir.resolve(parsed.filename());
            Files.write(target, bytes);
            bootstrapService.clearImportFingerprint();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save practice pack");
        }
        String kindLabel = parsed.kind() == DesktopPackFileNames.PackKind.TERM ? "Quarter pack" : "Lab pack";
        return new StageResult(
                parsed.kind(),
                parsed.filename(),
                kindLabel + " saved. Close and restart the practice app to load it.");
    }

    public record StageResult(
            DesktopPackFileNames.PackKind kind,
            String filename,
            String message) {}

    /**
     * Not a {@link ResponseStatusException} so {@code GlobalExceptionHandler}'s generic
     * ResponseStatus mapping cannot strip the {@code conflicts} payload into {@code ErrorResponse}.
     */
    public static class LabNameConflictException extends RuntimeException {
        private final List<DesktopPackImportService.LabNameConflict> conflicts;

        public LabNameConflictException(List<DesktopPackImportService.LabNameConflict> conflicts) {
            super("A lab with the same name already exists in offline practice");
            this.conflicts = conflicts;
        }

        public List<DesktopPackImportService.LabNameConflict> conflicts() {
            return conflicts;
        }
    }
}
