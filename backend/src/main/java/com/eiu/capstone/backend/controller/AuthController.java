package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.AuthRequest;
import com.eiu.capstone.backend.model.AuthResponse;
import com.eiu.capstone.backend.model.ForgotPasswordRequest;
import com.eiu.capstone.backend.model.GoogleLoginUpsertRequest;
import com.eiu.capstone.backend.model.LoginRequest;
import com.eiu.capstone.backend.model.ResetPasswordRequest;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.GoogleTokenVerifier;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.PasswordResetService;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final StudentTermAccessService studentTermAccessService;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, JwtService jwtService,
            UserAccountRepository userAccountRepository, UserService userService,
            PasswordResetService passwordResetService,
            StudentTermAccessService studentTermAccessService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.studentTermAccessService = studentTermAccessService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody AuthRequest request) {
        var tokenInfo = googleTokenVerifier.verify(request.token());

        var userAccount = userAccountRepository.findByEmail(tokenInfo.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Account not registered in the system"));
        requireActiveAccount(userAccount);

        List<String> roleNames = roleNamesFrom(userAccount);

        var jwt = jwtService.createToken(tokenInfo, roleNames, userAccount.getIrn());
        return ResponseEntity.ok(toAuthResponse(jwt, userAccount, tokenInfo.getDomain(), roleNames));
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
                request.role());

        List<String> roleNames = roleNamesFrom(userAccount);

        var jwt = jwtService.createToken(tokenInfo, roleNames, userAccount.getIrn());
        return ResponseEntity.ok(toAuthResponse(jwt, userAccount, tokenInfo.getDomain(), roleNames));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateWithIrnPassword(@Valid @RequestBody LoginRequest request) {
        try {
            var userAccount = userService.authenticateByIrn(request.irn(), request.password());
            List<String> roleNames = roleNamesFrom(userAccount);

            var jwt = jwtService.createToken(userAccount.getEmail(), userAccount.getFullName(), "local", roleNames,
                    userAccount.getIrn());
            return ResponseEntity.ok(toAuthResponse(jwt, userAccount, "local", roleNames));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {
        passwordResetService.requestReset(request.email(), origin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.completeReset(
                request.token(), request.newPassword(), request.confirmPassword());
        return ResponseEntity.ok().build();
    }

    private void requireActiveAccount(UserAccount userAccount) {
        if (userAccount == null || !userAccount.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "This account is inactive");
        }
    }

    private List<String> roleNamesFrom(UserAccount userAccount) {
        return userAccount.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .toList();
    }

    private AuthResponse toAuthResponse(String jwt, UserAccount userAccount, String domain, List<String> roleNames) {
        boolean inCurrentTerm = studentTermAccessService.isInCurrentTerm(userAccount);
        return new AuthResponse(
                jwt,
                userAccount.getId(),
                userAccount.getEmail(),
                userAccount.getFullName(),
                domain,
                roleNames,
                userAccount.getIrn(),
                userAccount.getStudentCode(),
                userAccount.getTeacherCode(),
                inCurrentTerm);
    }
}
