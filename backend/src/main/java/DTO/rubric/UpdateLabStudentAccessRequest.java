package com.eiu.capstone.backend.DTO.rubric;

import java.time.LocalDate;

public record UpdateLabStudentAccessRequest(Boolean studentVisible, LocalDate releaseDate) {}
