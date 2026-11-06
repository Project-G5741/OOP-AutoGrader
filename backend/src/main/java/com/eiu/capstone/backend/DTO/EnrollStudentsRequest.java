package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record EnrollStudentsRequest(@NotEmpty List<UUID> studentIds) {
}
