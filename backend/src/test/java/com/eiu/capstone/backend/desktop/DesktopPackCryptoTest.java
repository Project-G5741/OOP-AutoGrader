package com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFile;
import com.eiu.capstone.backend.desktop.pack.DesktopPackManifest;

class DesktopPackCryptoTest {

    private static KeyPair keyPair;
    private static DesktopPackCrypto crypto;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
        keyPair = gen.generateKeyPair();
        crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
    }

    @Test
    void roundtrip_encrypt_sign_open() throws Exception {
        byte[] inner = "{\"labs\":[]}".getBytes();
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                "2026-fa-v1",
                UUID.randomUUID(),
                "2026-FA",
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(UUID.randomUUID()),
                "placeholder");
        DesktopPackFile pack = crypto.buildPack(keyPair.getPrivate(), manifest, inner);
        byte[] opened = crypto.openPack(keyPair.getPublic(), pack);
        assertArrayEquals(inner, opened);
    }

    @Test
    void tampered_signature_rejected() throws Exception {
        byte[] inner = "{}".getBytes();
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                "v1",
                UUID.randomUUID(),
                "term",
                Instant.now(),
                List.of(UUID.randomUUID()),
                "placeholder");
        DesktopPackFile pack = crypto.buildPack(keyPair.getPrivate(), manifest, inner);
        DesktopPackFile bad = new DesktopPackFile(
                pack.formatVersion(),
                pack.manifest(),
                pack.ciphertextBase64(),
                pack.signatureBase64().substring(0, pack.signatureBase64().length() - 2) + "AA");
        assertThrows(Exception.class, () -> crypto.openPack(keyPair.getPublic(), bad));
    }

    @Test
    void serialize_deserialize_preserves_open() throws Exception {
        byte[] inner = "{\"ok\":true}".getBytes();
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                "v2",
                UUID.randomUUID(),
                "term",
                Instant.now(),
                List.of(UUID.randomUUID()),
                "placeholder");
        DesktopPackFile built = crypto.buildPack(keyPair.getPrivate(), manifest, inner);
        byte[] json = crypto.serializeFile(built);
        DesktopPackFile parsed = crypto.deserializeFile(json);
        assertEquals(built.manifest().packVersion(), parsed.manifest().packVersion());
        assertArrayEquals(inner, crypto.openPack(keyPair.getPublic(), parsed));
    }
}
