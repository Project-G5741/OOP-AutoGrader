package com.eiu.capstone.backend.analytics.dto;

import java.util.List;

import com.eiu.capstone.backend.DTO.UserDTO;

public record UsersBootstrapResponse(
        List<UserDTO.UserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
