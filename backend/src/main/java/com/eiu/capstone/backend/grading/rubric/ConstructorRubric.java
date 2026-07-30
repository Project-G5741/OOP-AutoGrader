package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record ConstructorRubric(UUID id, String scope, boolean isDefault, List<String> parameterTypes) {}
