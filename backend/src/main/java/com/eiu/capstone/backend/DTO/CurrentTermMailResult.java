package com.eiu.capstone.backend.DTO;

public record CurrentTermMailResult(
        int recipientCount,
        int sentCount,
        int failedCount,
        int skippedNoEmail) {
}
