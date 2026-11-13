package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        String dispatchClassName,
        String resultTypeName,
        int repeatCount,
        List<String> methodNameAliases,
        List<List<String>> parameterTypeAliases) {

    public InvocationRubric {
        parameterTypes = parameterTypes == null ? List.of() : List.copyOf(parameterTypes);
        receiverParameterTypes = receiverParameterTypes == null
                ? List.of()
                : List.copyOf(receiverParameterTypes);
        methodNameAliases = methodNameAliases == null || methodNameAliases.isEmpty()
                ? (methodName == null ? List.of() : List.of(methodName))
                : List.copyOf(methodNameAliases);
        parameterTypeAliases = parameterTypeAliases == null || parameterTypeAliases.isEmpty()
                ? parameterTypes.stream().map(List::of).toList()
                : parameterTypeAliases.stream().map(List::copyOf).toList();
    }

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
            String receiverParamsJson,
            String instanceName,
            UUID dispatchClassId,
            String dispatchClassName,
            String resultTypeName,
            int repeatCount) {
        this(
                id, kind, constructorId, methodId, className, methodName, parameterTypes, paramsJson,
                receiverConstructorId, receiverClassName, receiverParameterTypes, receiverParamsJson,
                instanceName, dispatchClassId, dispatchClassName, resultTypeName, repeatCount, null, null);
    }

    public int effectiveRepeatCount() {
        return repeatCount < 1 ? 1 : repeatCount;
    }

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
                null,
                null,
                1,
                null,
                null);
    }

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
            String receiverParamsJson,
            String instanceName,
            UUID dispatchClassId,
            String dispatchClassName,
            String resultTypeName) {
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
                instanceName,
                dispatchClassId,
                dispatchClassName,
                resultTypeName,
                1,
                null,
                null);
    }

    /** Rubric type name for object RETURN_VALUE checks (constructor class or method return type). */
    public static String objectResultTypeName(InvocationRubric invocation) {
        if (invocation == null) {
            return null;
        }
        if (invocation.resultTypeName() != null && !invocation.resultTypeName().isBlank()) {
            return invocation.resultTypeName();
        }
        if (invocation.kind() == InvocationKind.CONSTRUCTOR) {
            return invocation.className();
        }
        return null;
    }

    public static String resultTypeNameForConstructor(String className) {
        return className;
    }

    public static String resultTypeNameForMethod(Method method) {
        if (method == null || method.getMethodDeclaration() == null) {
            return null;
        }
        return simpleTypeName(method.getMethodDeclaration().getReturnType());
    }

    private static String simpleTypeName(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String trimmed = type.trim();
        int generic = trimmed.indexOf('<');
        if (generic >= 0) {
            trimmed = trimmed.substring(0, generic);
        }
        trimmed = trimmed.replace("[]", "").trim();
        int dot = trimmed.lastIndexOf('.');
        return dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
    }

    public boolean hasReceiver() {
        return receiverConstructorId != null && receiverClassName != null && !receiverClassName.isBlank();
    }
}
