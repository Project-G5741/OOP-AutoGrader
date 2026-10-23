package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.UUID;

import com.eiu.capstone.backend.model.InvocationKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvocationStructureDTO(
        UUID id,
        InvocationKind invocationKind,
        UUID constructorId,
        UUID methodId,
        String params,
        UUID receiverConstructorId,
        String receiverParams,
        String instanceName,
        UUID dispatchClassId) {}
