package com.eiu.capstone.backend.DTO;

import java.util.List;

public record ClassTabResponse(
        List<ClassDetailDTO> classes,
        String normalizationNotice
) {}
