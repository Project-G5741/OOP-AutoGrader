package com.eiu.capstone.backend.DTO;

import java.util.List;

public record ImportStudentsResult(
        int enrolled,
        int alreadyInTerm,
        int notFound,
        int skipped,
        List<String> unmatched,
        List<TermStudentDTO> students) {
}
