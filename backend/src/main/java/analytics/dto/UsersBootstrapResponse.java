package com.eiu.capstone.backend.analytics.dto;

import java.util.List;

import com.eiu.capstone.backend.model.UserAccount;

public record UsersBootstrapResponse(
        List<UserAccount> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
