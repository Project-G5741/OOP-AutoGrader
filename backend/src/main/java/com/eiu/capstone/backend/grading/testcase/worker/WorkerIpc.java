package com.eiu.capstone.backend.grading.testcase.worker;

import java.nio.file.Path;
import java.util.List;

import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class WorkerIpc {

    public static final int MAX_LINE_BYTES = 524288;
    public static final int DEFAULT_STDOUT_CAP = 65536;
    public static final int MAX_NESTING_DEPTH = 8;
    public static final String OP_INVOKE = "invoke";
    public static final String OP_SCENARIO = "scenario";

    private static final ObjectMapper MAPPER = createMapper();
    private static final WorkerInvokeEngine ENGINE = new WorkerInvokeEngine();

    private WorkerIpc() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static SerializedInvocationOutcome handleLine(String line) {
        if (line == null) {
            return SerializedInvocationOutcome.error("Empty IPC line");
        }
        if (line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LINE_BYTES) {
            return SerializedInvocationOutcome.error("IPC line exceeded " + MAX_LINE_BYTES + " bytes");
        }
        Request request;
        try {
            request = MAPPER.readValue(line, Request.class);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error("Malformed IPC JSON");
        }
        if (request == null || request.op() == null) {
            return SerializedInvocationOutcome.error("Missing IPC op");
        }
        int stdoutCap = request.stdoutCap() > 0 ? request.stdoutCap() : DEFAULT_STDOUT_CAP;
        Path classesDir = request.classesDir() != null ? Path.of(request.classesDir()) : null;
        try {
            return switch (request.op()) {
                case OP_INVOKE -> ENGINE.invoke(classesDir, request.invoke(), request.snapshotFieldNames(), stdoutCap);
                case OP_SCENARIO -> ENGINE.scenario(classesDir, request.steps(), request.snapshotFieldNames(), stdoutCap);
                default -> SerializedInvocationOutcome.error("Unknown IPC op");
            };
        } catch (Exception e) {
            return SerializedInvocationOutcome.error("Worker invoke failed");
        }
    }

    public static String writeLine(SerializedInvocationOutcome outcome) {
        try {
            return MAPPER.writeValueAsString(outcome);
        } catch (Exception e) {
            return "{\"kind\":\"ERROR\",\"errorMessage\":\"Failed to encode worker facts\",\"stdout\":\"\",\"stdoutTruncated\":false}";
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.deactivateDefaultTyping();
        mapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(MAX_NESTING_DEPTH)
                .maxStringLength(MAX_LINE_BYTES)
                .build());
        return mapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Request(
            String op,
            String classesDir,
            InvokeSpec invoke,
            CompareSpec compare,
            List<String> snapshotFieldNames,
            int stdoutCap,
            List<ScenarioStepSpec> steps) {

        public Request(String op,
                       String classesDir,
                       InvokeSpec invoke,
                       CompareSpec compare,
                       List<String> snapshotFieldNames,
                       int stdoutCap) {
            this(op, classesDir, invoke, compare, snapshotFieldNames, stdoutCap, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InvokeSpec(
            String kind,
            String className,
            String methodName,
            List<String> parameterTypes,
            String paramsJson,
            String receiverClassName,
            List<String> receiverParameterTypes,
            String receiverParamsJson) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioStepSpec(
            String kind,
            String className,
            String methodName,
            List<String> parameterTypes,
            String paramsJson,
            String receiverClassName,
            List<String> receiverParameterTypes,
            String receiverParamsJson,
            String instanceName,
            String dispatchClassName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompareSpec(
            String comparisonMethod,
            InstanceSpec a,
            InstanceSpec b) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstanceSpec(
            String className,
            List<String> parameterTypes,
            String paramsJson) {}
}
