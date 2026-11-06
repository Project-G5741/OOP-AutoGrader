package com.eiu.capstone.backend.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurrentTermMailRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 8000) String body) {
}
