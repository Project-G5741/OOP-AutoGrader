package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.UUID;

import com.eiu.capstone.backend.model.InvocationKind;

public record InvocationStructureDTO(
        UUID id,
        InvocationKind invocationKind,
        UUID constructorId,
        UUID methodId,
        String params,
        UUID receiverConstructorId,
        String receiverParams) {}
