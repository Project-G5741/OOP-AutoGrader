package com.eiu.capstone.backend.DTO.plagiarism;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlagiarismFlagsDTO(
        List<UUID> flaggedLabIds,
        Map<UUID, List<UUID>> flaggedLabsByStudentId) {}
