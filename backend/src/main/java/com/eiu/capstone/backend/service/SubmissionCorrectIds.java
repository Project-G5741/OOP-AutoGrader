package com.eiu.capstone.backend.service;

import java.util.Set;
import java.util.UUID;

public record SubmissionCorrectIds(
        Set<UUID> fieldIds,
        Set<UUID> methodIds,
        Set<UUID> constructorIds,
        Set<UUID> relationIds) {
}
