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
        String receiverParamsJson,
        String instanceName,
        UUID dispatchClassId,
        String dispatchClassName) {

    public InvocationRubric(
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
        this(
                id,
                kind,
                constructorId,
                methodId,
                className,
                methodName,
                parameterTypes,
                paramsJson,
                receiverConstructorId,
                receiverClassName,
                receiverParameterTypes,
                receiverParamsJson,
                null,
                null,
                null);
    }

    public boolean hasReceiver() {
        return receiverConstructorId != null && receiverClassName != null && !receiverClassName.isBlank();
    }
}
