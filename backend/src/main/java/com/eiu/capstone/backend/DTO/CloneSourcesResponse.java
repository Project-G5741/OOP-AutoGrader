package com.eiu.capstone.backend.DTO;

import java.util.List;

public record CloneSourcesResponse(
        CloneSourceTermDTO sourceTerm,
        List<CloneLabRefDTO> labs) {
}
