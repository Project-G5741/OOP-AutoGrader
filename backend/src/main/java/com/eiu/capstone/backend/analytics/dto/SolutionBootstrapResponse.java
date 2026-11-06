package com.eiu.capstone.backend.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.MasterDataItemDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;

public record SolutionBootstrapResponse(
        List<MasterDataItemDTO> scopeOptions,
        List<MasterDataItemDTO> declaringTypeOptions,
        List<MasterDataItemDTO> relationTypeOptions,
        List<TermSummaryDTO> terms,
        List<LabItem> labs,
        UUID selectedLabId,
        LabStructureResponse structure
) {
    /**
     * Lab row for Solution and Grading bootstraps.
     * Grading requires {@code challenges} (sidebar DTOs) for Lab/Exam mode gating and results tabs;
     * Solution may send an empty list.
     */
    public record LabItem(
            UUID id,
            String name,
            LocalDate deadlineDate,
            boolean studentVisible,
            List<ChallengeDTO> challenges
    ) {
        public LabItem {
            challenges = challenges == null ? List.of() : List.copyOf(challenges);
        }
    }
}
