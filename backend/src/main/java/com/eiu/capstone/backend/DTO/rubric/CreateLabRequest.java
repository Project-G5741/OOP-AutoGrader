package com.eiu.capstone.backend.DTO.rubric;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLabRequest(String name, UUID termId, LocalDate deadlineDate) {}
