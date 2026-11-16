package com.eiu.capstone.backend.ai;

import java.util.List;

public record FailingClassInfo(
        String className,
        String classType,
        String status,
        String compileError,
        List<String> fieldIssues,
        List<String> constructorIssues,
        List<String> methodIssues
) {}
