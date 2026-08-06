package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.JwtException;

import com.eiu.capstone.backend.DTO.BulkCreateResult;
import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.model.ChangePasswordRequest;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;  // ← THÊM DÒNG NÀY

    public UserController(UserService userService, JwtService jwtService) {  // ← SỬA CONSTRUCTOR
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("getAllUser")
    public ResponseEntity<List<UserAccount>> getAllUser() {
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
    public ResponseEntity<UserAccount> deleteUser(@PathVariable UUID id) {
        UserAccount user = userService.deleteUser(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO.UserResponse> updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UserDTO.UpdateUserRequest request) {
 
        UserDTO.UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    // change password endpoint
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.parseToken(token);
            String email = claims.get("email", String.class);
            
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: email not found");
            }
            
            userService.changePassword(email, request.currentPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully",
                "success", true
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "message", "Invalid or expired token",
                "success", false
            ));
        } catch (ResponseStatusException e) {
            // Trả về message lỗi chi tiết
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