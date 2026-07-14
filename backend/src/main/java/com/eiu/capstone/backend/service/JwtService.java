package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

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

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.validity-seconds}")
    private long validitySeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createToken(GoogleTokenInfo tokenInfo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(tokenInfo.getSub())
                .claim("email", tokenInfo.getEmail())
                .claim("name", tokenInfo.getName())
                .claim("domain", tokenInfo.getDomain())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
