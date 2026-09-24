package com.eiu.capstone.backend.grading.testcase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class InvocationRunner {

    private final JsonValueCoercer jsonValueCoercer;

    public InvocationRunner(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public InvocationOutcome invokeSingle(ChallengeGradingContext context,
                                          InvocationRubric invocation,
                                          List<String> snapshotFieldNames) {
        if (!hasRunnableClassesDir(context)) {
            return InvocationOutcome.error("Missing compiled classes directory");
        }
        if (invocation == null) {
            return InvocationOutcome.error("Missing invocation rubric");
        }
        WorkerSessionHandle handle = context.workerSession();
        if (handle == null) {
            return InvocationOutcome.error("Worker session missing");
        }
        return toInvocation(handle.invoke(context.classesDir().toAbsolutePath().toString(),
                invocation, snapshotFieldNames));
    }

    public List<InvocationOutcome> invokeScenario(ChallengeGradingContext context,
                                                  List<InvocationRubric> steps,
                                                  List<String> snapshotFieldNames) {
        if (!hasRunnableClassesDir(context)) {
            return List.of(InvocationOutcome.error("Missing compiled classes directory"));
        }
        if (steps == null || steps.isEmpty()) {
            return List.of(InvocationOutcome.error("Missing invocation rubric"));
        }
        WorkerSessionHandle handle = context.workerSession();
        if (handle == null) {
            return List.of(InvocationOutcome.error("Worker session missing"));
        }
        SerializedInvocationOutcome serialized = handle.scenario(
                context.classesDir().toAbsolutePath().toString(),
                steps,
                snapshotFieldNames);
        if (serialized != null && serialized.steps() != null && !serialized.steps().isEmpty()) {
            List<InvocationOutcome> outcomes = new java.util.ArrayList<>();
            for (SerializedInvocationOutcome step : serialized.steps()) {
                outcomes.add(toInvocation(step));
            }
            return List.copyOf(outcomes);
        }
        return List.of(toInvocation(serialized));
    }

    InvocationOutcome toInvocation(SerializedInvocationOutcome serialized) {
        if (serialized == null) {
            return InvocationOutcome.error("Empty worker response");
        }
        return switch (serialized.kind()) {
            case SerializedInvocationOutcome.KIND_TIMED_OUT -> InvocationOutcome.timedOut(serialized.stdout());
            case SerializedInvocationOutcome.KIND_ERROR -> InvocationOutcome.error(
                    serialized.errorMessage() != null ? serialized.errorMessage() : "Worker IPC failure");
            case SerializedInvocationOutcome.KIND_THREW -> InvocationOutcome.threw(
                    serialized.stdout(),
                    serialized.stdoutTruncated(),
                    serialized.exceptionSimpleName(),
                    serialized.exceptionSuperclassSimpleNames(),
                    decodeSnapshots(serialized.fieldSnapshotsJson()));
            case SerializedInvocationOutcome.KIND_NORMAL -> InvocationOutcome.normal(
                    decodeJson(serialized.returnValueJson()),
                    serialized.stdout(),
                    serialized.stdoutTruncated(),
                    decodeSnapshots(serialized.fieldSnapshotsJson()),
                    serialized.objectTypeSimpleName(),
                    decodeSnapshots(serialized.objectFieldSnapshotsJson()),
                    serialized.equalsNamed() == null ? Map.of() : serialized.equalsNamed());
            default -> InvocationOutcome.error("Malformed IPC JSON");
        };
    }

    private static boolean hasRunnableClassesDir(ChallengeGradingContext context) {
        return context != null
                && context.classesDir() != null
                && java.nio.file.Files.isDirectory(context.classesDir());
    }

    private Map<String, Object> decodeSnapshots(Map<String, String> snapshots) {
        Map<String, Object> decoded = new LinkedHashMap<>();
        if (snapshots == null) {
            return decoded;
        }
        for (Map.Entry<String, String> entry : snapshots.entrySet()) {
            decoded.put(entry.getKey(), decodeJson(entry.getValue()));
        }
        return decoded;
    }

    private Object decodeJson(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return null;
        }
        try {
            JsonNode node = jsonValueCoercer.parseTree(json);
            return jsonValueCoercer.coerceFromNode(node, null);
        } catch (Exception e) {
            return json;
        }
    }
}
