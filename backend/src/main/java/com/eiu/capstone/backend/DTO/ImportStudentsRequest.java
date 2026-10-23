package com.eiu.capstone.backend.DTO;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ImportStudentsRequest(@NotEmpty List<ImportStudentRow> rows) {
}
