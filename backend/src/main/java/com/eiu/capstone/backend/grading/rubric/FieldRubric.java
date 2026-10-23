package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

public record FieldRubric(UUID id, String name, String scope, String dataType) {}
