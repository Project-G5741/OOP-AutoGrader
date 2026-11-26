package com.eiu.capstone.backend.DTO;

import java.util.List;

public record OopBlitzQuestionImportRequest(List<OopBlitzQuestionUpsertRequest> questions) {
}
