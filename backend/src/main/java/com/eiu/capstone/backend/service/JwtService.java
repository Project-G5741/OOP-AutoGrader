package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.model.GoogleTokenInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final int MIN_HS256_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final long validitySeconds;

    public JwtService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.validity-seconds}") long validitySeconds) {
        this.signingKey = deriveSigningKey(jwtSecret);
        this.validitySeconds = validitySeconds;
    }

    private static SecretKey deriveSigningKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret / JWT_SECRET is missing or blank. Set a value of at least 32 bytes (HS256).");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret / JWT_SECRET must be at least 32 bytes (256 bits) for HS256; got "
                            + keyBytes.length + " bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(GoogleTokenInfo tokenInfo, List<String> roles, String irn) {
        return createToken(tokenInfo.getEmail(), tokenInfo.getName(), tokenInfo.getDomain(), roles, tokenInfo.getSub(), irn);
    }

    public String createToken(String email, String name, String domain, List<String> roles, String irn) {
        return createToken(email, name, domain, roles, email, irn);
    }

    private String createToken(String email, String name, String domain, List<String> roles, String subject, String irn) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .claim("email", email)
                .claim("name", name)
                .claim("domain", domain)
                .claim("roles", roles)
                .claim("irn", irn)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}
