package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzQuestionImportResponse(
        int importedCount,
        int skippedCount,
        List<String> errors) {
}
