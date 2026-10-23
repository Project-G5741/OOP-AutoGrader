package com.eiu.capstone.backend.DTO.plagiarism;

import java.util.List;
import java.util.UUID;

public record LabPlagiarismReportDTO(UUID labId, List<PlagiarismMatchDTO> matches) {}
