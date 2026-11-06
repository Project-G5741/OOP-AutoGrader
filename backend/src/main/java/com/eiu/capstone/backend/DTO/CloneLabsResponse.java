package com.eiu.capstone.backend.DTO;

import java.util.List;

public record CloneLabsResponse(
        List<CloneLabRefDTO> created,
        List<CloneLabErrorDTO> errors) {
}
