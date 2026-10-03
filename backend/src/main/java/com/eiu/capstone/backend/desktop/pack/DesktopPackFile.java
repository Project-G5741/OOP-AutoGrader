package com.eiu.capstone.backend.desktop.pack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * On-disk {@code .agpack} envelope (JSON).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DesktopPackFile(
        int formatVersion,
        DesktopPackManifest manifest,
        String ciphertextBase64,
        String signatureBase64
) {
}
