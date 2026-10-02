package com.eiu.capstone.backend.desktop.pack;

import java.security.PublicKey;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Opens and validates a desktop practice pack, resolving the canonical on-disk filename.
 */
public final class DesktopPackIngest {

    private DesktopPackIngest() {}

    public record OpenedPack(
            DesktopPackFile file,
            DesktopPackInnerPayload inner,
            DesktopPackFileNames.ParsedFilename parsed,
            byte[] rawBytes) {}

    public static OpenedPack open(
            byte[] raw,
            String originalFilename,
            DesktopPackCrypto crypto,
            PublicKey verificationKey,
            DesktopPackSerializer serializer) {
        if (raw == null || raw.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a practice pack file to import");
        }
        DesktopPackFile pack;
        DesktopPackInnerPayload inner;
        try {
            pack = crypto.deserializeFile(raw);
            byte[] innerJson = crypto.openPack(verificationKey, pack);
            inner = serializer.fromJson(innerJson);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ex.getMessage() != null ? ex.getMessage() : "Invalid or tampered practice pack");
        }
        DesktopPackFileNames.ParsedFilename parsed = resolveFilename(originalFilename, pack, inner);
        DesktopPackFileNames.assertMatchesPayload(parsed, pack.manifest(), inner);
        return new OpenedPack(pack, inner, parsed, raw);
    }

    public static OpenedPack openFromDiskPath(
            byte[] raw,
            String onDiskFileName,
            DesktopPackCrypto crypto,
            PublicKey verificationKey,
            DesktopPackSerializer serializer) {
        return open(raw, onDiskFileName, crypto, verificationKey, serializer);
    }

    static DesktopPackFileNames.ParsedFilename resolveFilename(
            String originalFilename,
            DesktopPackFile file,
            DesktopPackInnerPayload inner) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            try {
                return DesktopPackFileNames.parse(originalFilename);
            } catch (ResponseStatusException ignored) {
                // fall through — browser may send a wrong name; infer from payload
            }
        }
        return inferFromPayload(file, inner, originalFilename);
    }

    private static DesktopPackFileNames.ParsedFilename inferFromPayload(
            DesktopPackFile file,
            DesktopPackInnerPayload inner,
            String originalFilename) {
        String hint = originalFilename == null ? "" : originalFilename.toLowerCase();
        boolean labHint = hint.endsWith(".lab.agpack");
        boolean termHint = hint.endsWith(".term.agpack");

        if (termHint) {
            java.util.UUID termId = inner.termId();
            return new DesktopPackFileNames.ParsedFilename(
                    DesktopPackFileNames.termFilename(termId),
                    DesktopPackFileNames.PackKind.TERM,
                    termId);
        }
        if (labHint) {
            if (inner.labs().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab pack is empty");
            }
            java.util.UUID labId = inner.labs().get(0).lab().id();
            return new DesktopPackFileNames.ParsedFilename(
                    DesktopPackFileNames.labFilename(labId),
                    DesktopPackFileNames.PackKind.LAB,
                    labId);
        }
        if (inner.labs().size() > 1) {
            java.util.UUID termId = inner.termId();
            return new DesktopPackFileNames.ParsedFilename(
                    DesktopPackFileNames.termFilename(termId),
                    DesktopPackFileNames.PackKind.TERM,
                    termId);
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Rename the file to {" + inner.termId() + "}.term.agpack (quarter) or {"
                        + (inner.labs().isEmpty() ? "lab-uuid" : inner.labs().get(0).lab().id())
                        + "}.lab.agpack (single lab) before importing");
    }
}
