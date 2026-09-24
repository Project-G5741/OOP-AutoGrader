package com.eiu.capstone.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.ActiveUsersDTO;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.PresenceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public ActiveUsersDTO activeUsers(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest request) {
        // Public count stays open; a Bearer that failed session validity must 401 so the SPA hard-cuts.
        if (principal == null && hasBearer(request)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session revoked");
        }
        if (principal != null) {
            presenceService.heartbeat(principal.email());
        }
        return new ActiveUsersDTO(presenceService.countActive());
    }

    @DeleteMapping
    public ActiveUsersDTO leave(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal != null) {
            presenceService.leave(principal.email());
        }
        return new ActiveUsersDTO(presenceService.countActive());
    }

    private static boolean hasBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ");
    }
}
