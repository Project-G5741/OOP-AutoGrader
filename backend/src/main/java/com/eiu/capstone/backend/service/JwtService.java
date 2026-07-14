package com.eiu.capstone.backend.service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.model.GoogleTokenInfo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${jwt.validity-seconds}")
    private long validitySeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        // Generated fresh every time the app starts — not loaded from config.
        // This means tokens signed before a restart can no longer be verified,
        // forcing all users to log in again after every deploy/restart.
        signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public String createToken(GoogleTokenInfo tokenInfo, List<String> roles) {
        return createToken(tokenInfo.getEmail(), tokenInfo.getName(), tokenInfo.getDomain(), roles, tokenInfo.getSub());
    }

    public String createToken(String email, String name, String domain, List<String> roles) {
        return createToken(email, name, domain, roles, email);
    }

    private String createToken(String email, String name, String domain, List<String> roles, String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .claim("email", email)
                .claim("name", name)
                .claim("domain", domain)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}