package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkCreateResult;
import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.model.ChangePasswordRequest;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("getAllUser")
    public ResponseEntity<?> getAllUser(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int safePage = page != null ? Math.max(page, 0) : 0;
            int safeSize = size != null && size > 0 ? Math.min(size, 100) : 50;
            Page<UserAccount> userPage = userService.getAllUser(PageRequest.of(safePage, safeSize));
            return ResponseEntity.ok(userPage);
        }
        List<UserAccount> userList = userService.getAllUser();
        return ResponseEntity.ok(userList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUser(@PathVariable UUID id) {
        UserAccount user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("addUser")
    public ResponseEntity<UserAccount> addUser(@RequestBody CreateUserRequest request) {
        UserAccount created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<BulkCreateResult>> addUsers(@RequestBody List<CreateUserRequest> requests) {
        List<BulkCreateResult> results = userService.createUser(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserDTO.UserResponse> deleteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<UserDTO.UserResponse> suspendStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.suspendStudent(id));
    }

    @PostMapping("/{id}/unsuspend")
    public ResponseEntity<UserDTO.UserResponse> restoreStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.restoreStudent(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO.UserResponse> updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UserDTO.UpdateUserRequest request) {
        UserDTO.UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: email not found");
        }
        try {
            userService.changePassword(principal.email(), request.currentPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully",
                "success", true
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "message", e.getReason(),
                "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "An unexpected error occurred",
                "success", false
            ));
        }
    }
}
