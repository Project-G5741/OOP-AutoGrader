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
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.validity-seconds}")
    private long validitySeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()
                || "replace_me_change_this_to_a_secret_at_least_32_chars".equals(jwtSecret.trim())) {
            signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("jwt.secret must be at least 32 bytes");
            }
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
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
