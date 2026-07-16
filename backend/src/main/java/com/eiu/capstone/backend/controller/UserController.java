package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.eiu.capstone.backend.DTO.BulkCreateResult;
import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.DTO.UserDTO.UpdateUserRequest;
import com.eiu.capstone.backend.model.UserAccount;
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
        List<BulkCreateResult> results = userService.createUsers(requests);
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
}
