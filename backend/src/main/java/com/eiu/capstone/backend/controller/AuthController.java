package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.AuthRequest;
import com.eiu.capstone.backend.model.AuthResponse;
import com.eiu.capstone.backend.repository.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, JwtService jwtService, UserAccountRepository userAccountRepository) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody AuthRequest request) {
        var tokenInfo = googleTokenVerifier.verify(request.token());

        var userAccount = userAccountRepository.findByEmail(tokenInfo.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Account not registered in the system"));

        List<String> roleNames = userAccount.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .toList();

        var jwt = jwtService.createToken(tokenInfo, roleNames);
        var response = new AuthResponse(jwt, tokenInfo.getEmail(), tokenInfo.getName(), tokenInfo.getDomain(), roleNames);
        return ResponseEntity.ok(response);
    }
}
