package com.eiu.capstone.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.ActiveUsersDTO;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.PresenceService;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public ActiveUsersDTO activeUsers(@AuthenticationPrincipal JwtUserPrincipal principal) {
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
}
