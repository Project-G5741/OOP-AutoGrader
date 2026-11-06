package com.eiu.capstone.backend.desktop.pack;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Sign, encrypt, and verify term rubric packs for desktop practice mode.
 */
public final class DesktopPackCrypto {

    private static final String SIGN_ALG = "Ed25519";
    private static final String AES_TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final ObjectMapper mapper;
    private final byte[] contentKeyMaterial;

    public DesktopPackCrypto(byte[] contentKeyMaterial) {
        if (contentKeyMaterial == null || contentKeyMaterial.length < 16) {
            throw new IllegalArgumentException("contentKeyMaterial too short");
        }
        this.contentKeyMaterial = contentKeyMaterial.clone();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static DesktopPackCrypto withDefaultEmbeddedKey() {
        try {
            return new DesktopPackCrypto(
                    MessageDigest.getInstance("SHA-256")
                            .digest("eiu-capstone-desktop-pack-v1".getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] encryptPayload(byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] out = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
        return out;
    }

    public byte[] decryptPayload(byte[] ivPlusCiphertext) throws GeneralSecurityException {
        if (ivPlusCiphertext.length <= GCM_IV_BYTES) {
            throw new GeneralSecurityException("ciphertext too short");
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        System.arraycopy(ivPlusCiphertext, 0, iv, 0, GCM_IV_BYTES);
        byte[] ciphertext = new byte[ivPlusCiphertext.length - GCM_IV_BYTES];
        System.arraycopy(ivPlusCiphertext, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);
        Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    public byte[] sign(PrivateKey privateKey, DesktopPackManifest manifest, byte[] ciphertext) throws GeneralSecurityException {
        byte[] payload = signingPayload(manifest, ciphertext);
        Signature sig = Signature.getInstance(SIGN_ALG);
        sig.initSign(privateKey);
        sig.update(payload);
        return sig.sign();
    }

    public void verify(PublicKey publicKey, DesktopPackManifest manifest, byte[] ciphertext, byte[] signature)
            throws GeneralSecurityException {
        byte[] payload = signingPayload(manifest, ciphertext);
        Signature sig = Signature.getInstance(SIGN_ALG);
        sig.initVerify(publicKey);
        sig.update(payload);
        if (!sig.verify(signature)) {
            throw new GeneralSecurityException("pack signature invalid");
        }
    }

    public DesktopPackFile buildPack(PrivateKey privateKey, DesktopPackManifest manifest, byte[] innerJsonUtf8)
            throws GeneralSecurityException {
        byte[] ciphertext = encryptPayload(innerJsonUtf8);
        DesktopPackManifest signedManifest = new DesktopPackManifest(
                manifest.formatVersion(),
                manifest.packVersion(),
                manifest.termId(),
                manifest.termLabel(),
                manifest.createdAt(),
                manifest.labIds(),
                sha256Hex(ciphertext));
        byte[] signature = sign(privateKey, signedManifest, ciphertext);
        return new DesktopPackFile(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                signedManifest,
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(signature));
    }

    public byte[] openPack(PublicKey publicKey, DesktopPackFile file) throws GeneralSecurityException {
        if (file.formatVersion() != DesktopPackManifest.CURRENT_FORMAT_VERSION) {
            throw new GeneralSecurityException("unsupported pack formatVersion");
        }
        byte[] ciphertext = Base64.getDecoder().decode(file.ciphertextBase64());
        byte[] signature = Base64.getDecoder().decode(file.signatureBase64());
        verify(publicKey, file.manifest(), ciphertext, signature);
        String expectedHash = sha256Hex(ciphertext);
        if (!expectedHash.equalsIgnoreCase(file.manifest().contentSha256())) {
            throw new GeneralSecurityException("contentSha256 mismatch");
        }
        return decryptPayload(ciphertext);
    }

    public byte[] serializeFile(DesktopPackFile file) throws GeneralSecurityException {
        try {
            return mapper.writeValueAsBytes(file);
        } catch (Exception e) {
            throw new GeneralSecurityException("serialize failed", e);
        }
    }

    public DesktopPackFile deserializeFile(byte[] json) throws GeneralSecurityException {
        try {
            return mapper.readValue(json, DesktopPackFile.class);
        } catch (Exception e) {
            throw new GeneralSecurityException("deserialize failed", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static PublicKey decodePublicKey(byte[] x509) throws GeneralSecurityException {
        return KeyFactory.getInstance(SIGN_ALG).generatePublic(new X509EncodedKeySpec(x509));
    }

    public static PrivateKey decodePrivateKey(byte[] pkcs8) throws GeneralSecurityException {
        return KeyFactory.getInstance(SIGN_ALG).generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private SecretKey deriveAesKey() {
        return new SecretKeySpec(contentKeyMaterial, "AES");
    }

    private byte[] signingPayload(DesktopPackManifest manifest, byte[] ciphertext) throws GeneralSecurityException {
        try {
            byte[] manifestJson = mapper.writeValueAsBytes(manifest);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(ciphertext);
            byte[] out = new byte[manifestJson.length + hash.length];
            System.arraycopy(manifestJson, 0, out, 0, manifestJson.length);
            System.arraycopy(hash, 0, out, manifestJson.length, hash.length);
            return out;
        } catch (Exception e) {
            throw new GeneralSecurityException("signing payload build failed", e);
        }
    }
}
