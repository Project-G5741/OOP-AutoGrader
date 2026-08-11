package com.eiu.capstone.backend.DTO.rubric;

import java.util.UUID;

public record CreateLabRequest(String name, UUID termId) {}
