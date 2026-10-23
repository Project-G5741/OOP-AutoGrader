package com.eiu.capstone.backend.DTO;

import com.eiu.capstone.backend.model.UserAccount;

public record BulkCreateResult(boolean success, UserAccount user, String email, String error) {
    public static BulkCreateResult success(UserAccount user) {
        return new BulkCreateResult(true, user, user.getEmail(), null);
    }
    public static BulkCreateResult failure(String email, String error) {
        return new BulkCreateResult(false, null, email, error);
    }
}
