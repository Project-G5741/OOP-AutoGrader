package com.eiu.capstone.backend.desktop.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class DesktopPackSigningKeys {

    private static final String PUBLIC_KEY_RESOURCE = "desktop-pack-public.key";

    private final Optional<PrivateKey> signingPrivateKey;
    private final PublicKey verificationPublicKey;

    public DesktopPackSigningKeys(
            @Value("${desktop.pack.signing-private-key:}") String signingPrivateKeyBase64)
            throws GeneralSecurityException, IOException {
        this.verificationPublicKey = loadPublicKey();
        if (signingPrivateKeyBase64 == null || signingPrivateKeyBase64.isBlank()) {
            this.signingPrivateKey = Optional.empty();
        } else {
            byte[] pkcs8 = Base64.getDecoder().decode(signingPrivateKeyBase64.trim());
            this.signingPrivateKey = Optional.of(DesktopPackCrypto.decodePrivateKey(pkcs8));
        }
    }

    public Optional<PrivateKey> signingPrivateKey() {
        return signingPrivateKey;
    }

    public PublicKey verificationPublicKey() {
        return verificationPublicKey;
    }

    private static PublicKey loadPublicKey() throws IOException, GeneralSecurityException {
        ClassPathResource resource = new ClassPathResource(PUBLIC_KEY_RESOURCE);
        try (InputStream in = resource.getInputStream()) {
            String line = StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
            byte[] x509 = Base64.getDecoder().decode(line);
            return DesktopPackCrypto.decodePublicKey(x509);
        }
    }
}
