package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTermRequest(
        @NotBlank String yearLabel,
        @NotNull @Min(1) @Max(3) Integer termNumber,
        LocalDate startDate,
        LocalDate endDate,
        boolean setCurrent) {
}
