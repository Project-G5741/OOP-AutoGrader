package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.InvocationKind;

public record InvocationRubric(
        UUID id,
        InvocationKind kind,
        UUID constructorId,
        UUID methodId,
        String className,
        String methodName,
        List<String> parameterTypes,
        String paramsJson,
        UUID receiverConstructorId,
        String receiverClassName,
        List<String> receiverParameterTypes,
        String receiverParamsJson) {

    public boolean hasReceiver() {
        return receiverConstructorId != null && receiverClassName != null && !receiverClassName.isBlank();
    }
}
