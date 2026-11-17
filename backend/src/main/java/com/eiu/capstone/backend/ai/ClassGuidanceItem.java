package com.eiu.capstone.backend.ai;

import java.util.List;

public record ClassGuidanceItem(
        String className,
        String classType,
        String status,
        String compileError,
        List<GuidanceDetail> guidance
) {}
