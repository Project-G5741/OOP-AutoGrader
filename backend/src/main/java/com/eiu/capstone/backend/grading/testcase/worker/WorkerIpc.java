package com.eiu.capstone.backend.grading.testcase.worker;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class WorkerIpc {

    /** Lab-wide OT batches need more than 512KiB for hundreds of outcomes. */
    public static final int MAX_LINE_BYTES = 4 * 1024 * 1024;
    public static final int DEFAULT_STDOUT_CAP = 65536;
    public static final int MAX_NESTING_DEPTH = 8;
    public static final String OP_INVOKE = "invoke";
    public static final String OP_SCENARIO = "scenario";
    public static final String OP_BATCH = "batch";
    /** Parse student classes on a live worker session (OT host lease). */
    public static final String OP_REFLECT = "reflect";

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
                case OP_INVOKE -> ENGINE.invoke(
                        classesDir, request.invoke(), request.snapshotFieldNames(),
                        request.snapshotFieldAliases(), stdoutCap);
                case OP_SCENARIO -> ENGINE.scenario(
                        classesDir, request.steps(), request.snapshotFieldNames(),
                        request.snapshotFieldAliases(), stdoutCap);
                case OP_BATCH -> ENGINE.batch(
                        classesDir,
                        request.items(),
                        request.timeoutSeconds() > 0 ? request.timeoutSeconds() : 3,
                        stdoutCap,
                        request.batchParallelism());
                case OP_REFLECT -> reflectClasses(classesDir);
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

    /**
     * After a hang timeout the invoke thread may ignore interrupt; the process must exit
     * so the API can respawn a clean worker for remaining work.
     */
    private static SerializedInvocationOutcome reflectClasses(Path classesDir) {
        if (classesDir == null || !java.nio.file.Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        try {
            var classes = WorkerClassReflector.parseClasses(classesDir);
            String json = MAPPER.writeValueAsString(classes);
            return new SerializedInvocationOutcome(
                    SerializedInvocationOutcome.KIND_NORMAL,
                    json,
                    "",
                    false,
                    Map.of(),
                    null,
                    List.of(),
                    null,
                    null);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error("Worker reflect failed");
        }
    }

    public static boolean shouldAbortSession(SerializedInvocationOutcome outcome) {
        if (outcome == null || outcome.kind() == null) {
            return false;
        }
        if (SerializedInvocationOutcome.KIND_TIMED_OUT.equals(outcome.kind())) {
            return true;
        }
        if (outcome.batch() == null) {
            return false;
        }
        for (SerializedInvocationOutcome item : outcome.batch()) {
            if (item != null && SerializedInvocationOutcome.KIND_TIMED_OUT.equals(item.kind())) {
                return true;
            }
        }
        return false;
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
            List<ScenarioStepSpec> steps,
            List<BatchItemSpec> items,
            int timeoutSeconds,
            int batchParallelism,
            Map<String, List<String>> snapshotFieldAliases) {

        public Request(String op,
                       String classesDir,
                       InvokeSpec invoke,
                       CompareSpec compare,
                       List<String> snapshotFieldNames,
                       int stdoutCap) {
            this(op, classesDir, invoke, compare, snapshotFieldNames, stdoutCap, null, null, 0, 0, null);
        }

        public Request(String op,
                       String classesDir,
                       InvokeSpec invoke,
                       CompareSpec compare,
                       List<String> snapshotFieldNames,
                       int stdoutCap,
                       List<ScenarioStepSpec> steps) {
            this(op, classesDir, invoke, compare, snapshotFieldNames, stdoutCap, steps, null, 0, 0, null);
        }

        public Request(String op,
                       String classesDir,
                       InvokeSpec invoke,
                       CompareSpec compare,
                       List<String> snapshotFieldNames,
                       int stdoutCap,
                       List<ScenarioStepSpec> steps,
                       List<BatchItemSpec> items,
                       int timeoutSeconds) {
            this(op, classesDir, invoke, compare, snapshotFieldNames, stdoutCap, steps, items, timeoutSeconds, 0, null);
        }

        public Request(String op,
                       String classesDir,
                       InvokeSpec invoke,
                       CompareSpec compare,
                       List<String> snapshotFieldNames,
                       int stdoutCap,
                       List<ScenarioStepSpec> steps,
                       List<BatchItemSpec> items,
                       int timeoutSeconds,
                       int batchParallelism) {
            this(
                    op, classesDir, invoke, compare, snapshotFieldNames, stdoutCap,
                    steps, items, timeoutSeconds, batchParallelism, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BatchItemSpec(
            List<ScenarioStepSpec> steps,
            List<String> snapshotFieldNames,
            String classesDir,
            Map<String, List<String>> snapshotFieldAliases) {

        public BatchItemSpec(List<ScenarioStepSpec> steps, List<String> snapshotFieldNames) {
            this(steps, snapshotFieldNames, null, null);
        }

        public BatchItemSpec(
                List<ScenarioStepSpec> steps,
                List<String> snapshotFieldNames,
                String classesDir) {
            this(steps, snapshotFieldNames, classesDir, null);
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
            String receiverParamsJson,
            List<String> methodNameAliases,
            List<List<String>> parameterTypeAliases,
            List<List<String>> receiverParameterTypeAliases) {

        public InvokeSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson,
                List<String> methodNameAliases,
                List<List<String>> parameterTypeAliases) {
            this(
                    kind, className, methodName, parameterTypes, paramsJson,
                    receiverClassName, receiverParameterTypes, receiverParamsJson,
                    methodNameAliases, parameterTypeAliases, null);
        }

        public InvokeSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson) {
            this(
                    kind, className, methodName, parameterTypes, paramsJson,
                    receiverClassName, receiverParameterTypes, receiverParamsJson, null, null, null);
        }
    }

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
            String dispatchClassName,
            int repeatCount,
            String constructMode,
            List<String> methodNameAliases,
            List<List<String>> parameterTypeAliases,
            List<List<String>> receiverParameterTypeAliases,
            List<String> snapshotFieldNames,
            Map<String, List<String>> snapshotFieldAliases) {

        public ScenarioStepSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson,
                String instanceName,
                String dispatchClassName,
                int repeatCount,
                String constructMode,
                List<String> methodNameAliases,
                List<List<String>> parameterTypeAliases) {
            this(
                    kind, className, methodName, parameterTypes, paramsJson,
                    receiverClassName, receiverParameterTypes, receiverParamsJson,
                    instanceName, dispatchClassName, repeatCount, constructMode,
                    methodNameAliases, parameterTypeAliases, null, null, null);
        }

        /** Composition step that builds the object the way a Unit hidden receiver does. */
        public static final String CONSTRUCT_DEFAULT = "DEFAULT";

        public ScenarioStepSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson,
                String instanceName,
                String dispatchClassName) {
            this(
                    kind,
                    className,
                    methodName,
                    parameterTypes,
                    paramsJson,
                    receiverClassName,
                    receiverParameterTypes,
                    receiverParamsJson,
                    instanceName,
                    dispatchClassName,
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public ScenarioStepSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson,
                String instanceName,
                String dispatchClassName,
                int repeatCount) {
            this(
                    kind,
                    className,
                    methodName,
                    parameterTypes,
                    paramsJson,
                    receiverClassName,
                    receiverParameterTypes,
                    receiverParamsJson,
                    instanceName,
                    dispatchClassName,
                    repeatCount,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public ScenarioStepSpec(
                String kind,
                String className,
                String methodName,
                List<String> parameterTypes,
                String paramsJson,
                String receiverClassName,
                List<String> receiverParameterTypes,
                String receiverParamsJson,
                String instanceName,
                String dispatchClassName,
                int repeatCount,
                String constructMode) {
            this(
                    kind, className, methodName, parameterTypes, paramsJson,
                    receiverClassName, receiverParameterTypes, receiverParamsJson,
                    instanceName, dispatchClassName, repeatCount, constructMode,
                    null, null, null, null, null);
        }

        public boolean implicitDefaultConstructor() {
            return CONSTRUCT_DEFAULT.equals(constructMode);
        }
    }

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
