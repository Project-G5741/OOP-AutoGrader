package com.eiu.capstone.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.model.AuthRequest;
import com.eiu.capstone.backend.model.AuthResponse;
import com.eiu.capstone.backend.service.GoogleTokenVerifier;
import com.eiu.capstone.backend.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {"http://localhost:5174", "http://127.0.0.1:5174", "http://localhost:5173", "http://127.0.0.1:5173"},
        allowedHeaders = "*",
        allowCredentials = "true"
)
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody AuthRequest request) {
        var tokenInfo = googleTokenVerifier.verify(request.token());
        var jwt = jwtService.createToken(tokenInfo);
        var response = new AuthResponse(jwt, tokenInfo.getEmail(), tokenInfo.getName(), tokenInfo.getDomain());
        return ResponseEntity.ok(response);
    }
}
