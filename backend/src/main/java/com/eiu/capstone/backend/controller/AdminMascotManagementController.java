package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.AdminGachaResourceUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantResult;
import com.eiu.capstone.backend.DTO.AdminMascotEquipRequest;
import com.eiu.capstone.backend.DTO.AdminMascotGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminUserMascotRow;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.AdminMascotManagementService;

@RestController
@RequestMapping("/api/lecturer/admin-mascots")
@Profile("!desktop & !desktop-lecturer")
public class AdminMascotManagementController {

    private final JwtAuthHelper jwtAuthHelper;
    private final AdminMascotManagementService mascotManagementService;

    public AdminMascotManagementController(
            JwtAuthHelper jwtAuthHelper,
            AdminMascotManagementService mascotManagementService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.mascotManagementService = mascotManagementService;
    }

    @GetMapping("/users")
    public List<AdminUserMascotRow> listUsers(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireAdmin(principal);
        return mascotManagementService.listUsers();
    }

    @PutMapping("/users/{userId}/resources")
    public AdminUserMascotRow updateResources(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID userId,
            @RequestBody AdminGachaResourceUpdateRequest request) {
        requireAdmin(principal);
        return mascotManagementService.updateResources(userId, request);
    }

    @PostMapping("/users/{userId}/pets")
    public AdminUserMascotRow grantPets(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID userId,
            @RequestBody AdminMascotGrantRequest request) {
        requireAdmin(principal);
        return mascotManagementService.grantPets(userId, request);
    }

    @PostMapping("/bulk-grant")
    public AdminMascotBulkGrantResult bulkGrantPets(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody AdminMascotBulkGrantRequest request) {
        requireAdmin(principal);
        return mascotManagementService.bulkGrantPets(request);
    }

    @PutMapping("/users/{userId}/pets/{mascotId}")
    public AdminUserMascotRow updatePet(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID userId,
            @PathVariable String mascotId,
            @RequestBody AdminMascotUpdateRequest request) {
        requireAdmin(principal);
        return mascotManagementService.updatePet(userId, mascotId, request);
    }

    @PutMapping("/users/{userId}/equipped")
    public AdminUserMascotRow setEquippedMascot(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID userId,
            @RequestBody AdminMascotEquipRequest request) {
        requireAdmin(principal);
        return mascotManagementService.setEquippedMascot(userId, request);
    }

    @DeleteMapping("/users/{userId}/pets/{mascotId}")
    public AdminUserMascotRow deletePet(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID userId,
            @PathVariable String mascotId) {
        requireAdmin(principal);
        return mascotManagementService.deletePet(userId, mascotId);
    }

    private void requireAdmin(JwtUserPrincipal principal) {
        jwtAuthHelper.requireActiveUser(principal);
        if (principal == null || !principal.hasRole(JwtRoleNames.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}
