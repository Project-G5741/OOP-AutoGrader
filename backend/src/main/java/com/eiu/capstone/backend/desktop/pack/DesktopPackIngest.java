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
        DesktopPackFileNames.ParsedFilename parsed = DesktopPackFileNames.parse(originalFilename);
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
}
