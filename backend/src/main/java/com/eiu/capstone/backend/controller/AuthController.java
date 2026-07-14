package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.AuthRequest;
import com.eiu.capstone.backend.model.AuthResponse;
import com.eiu.capstone.backend.model.GoogleLoginUpsertRequest;
import com.eiu.capstone.backend.model.LoginRequest;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.GoogleTokenVerifier;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.UserService;

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
    private final UserService userService;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, JwtService jwtService,
                          UserAccountRepository userAccountRepository, UserService userService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.userService = userService;
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

    @PostMapping("/google/upsert")
    public ResponseEntity<AuthResponse> upsertGoogleUser(@Valid @RequestBody GoogleLoginUpsertRequest request) {
        var tokenInfo = googleTokenVerifier.verify(request.token());
        var userAccount = userService.createOrUpdateGoogleUser(
                tokenInfo.getEmail(),
                tokenInfo.getName(),
                null,
                request.irn(),
                request.password(),
                request.role()
        );

        List<String> roleNames = userAccount.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .toList();

        var jwt = jwtService.createToken(tokenInfo, roleNames);
        var response = new AuthResponse(jwt, tokenInfo.getEmail(), tokenInfo.getName(), tokenInfo.getDomain(), roleNames);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateWithIrnPassword(@Valid @RequestBody LoginRequest request) {
        try {
            var userAccount = userService.authenticateByIrn(request.irn(), request.password());
            List<String> roleNames = userAccount.getRoles().stream()
                    .map(role -> role.getName().toUpperCase())
                    .toList();

            var jwt = jwtService.createToken(userAccount.getEmail(), userAccount.getFullName(), "local", roleNames);
            return ResponseEntity.ok(new AuthResponse(jwt, userAccount.getEmail(), userAccount.getFullName(), "local", roleNames));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }
}
