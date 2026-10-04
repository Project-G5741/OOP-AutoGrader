package com.eiu.capstone.backend.grading.testcase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class InvocationRunner {

    private final JsonValueCoercer jsonValueCoercer;

    public InvocationRunner(JsonValueCoercer jsonValueCoercer) {
        this.jsonValueCoercer = jsonValueCoercer;
    }

    public record BatchItem(List<InvocationRubric> steps, List<String> snapshotFieldNames) {}

    public InvocationOutcome invokeSingle(ChallengeGradingContext context,
                                          InvocationRubric invocation,
                                          List<String> snapshotFieldNames) {
        if (invocation == null) {
            return InvocationOutcome.error("Missing invocation rubric");
        }
        List<List<InvocationOutcome>> batch = invokeBatch(
                context, List.of(new BatchItem(List.of(invocation), snapshotFieldNames)));
        if (batch.isEmpty() || batch.get(0).isEmpty()) {
            return InvocationOutcome.error("Invocation failed");
        }
        return batch.get(0).get(0);
    }

    public List<InvocationOutcome> invokeScenario(ChallengeGradingContext context,
                                                  List<InvocationRubric> steps,
                                                  List<String> snapshotFieldNames) {
        List<List<InvocationOutcome>> batch = invokeBatch(context, List.of(new BatchItem(steps, snapshotFieldNames)));
        if (batch.isEmpty()) {
            return List.of(InvocationOutcome.error("Invocation failed"));
        }
        return batch.get(0);
    }

    /**
     * One round-trip for all challenge OT items. Each list in the result is that item's scenario step outcomes.
     */
    public List<List<InvocationOutcome>> invokeBatch(ChallengeGradingContext context, List<BatchItem> items) {
        if (!hasRunnableClassesDir(context)) {
            return fillErrors(items == null ? 0 : items.size(), "Missing compiled classes directory");
        }
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        WorkerSessionHandle handle = context.workerSession();
        if (handle == null) {
            return fillErrors(items.size(), "Worker session missing");
        }
        List<WorkerIpc.BatchItemSpec> specs = new ArrayList<>(items.size());
        for (BatchItem item : items) {
            List<WorkerIpc.ScenarioStepSpec> steps = new ArrayList<>();
            if (item != null && item.steps() != null) {
                for (InvocationRubric step : item.steps()) {
                    steps.add(WorkerSessionHandle.toScenarioStep(step));
                }
            }
            specs.add(new WorkerIpc.BatchItemSpec(steps, item == null ? List.of() : item.snapshotFieldNames()));
        }
        SerializedInvocationOutcome serialized = handle.batch(
                context.classesDir().toAbsolutePath().toString(), specs);
        return toBatchOutcomes(serialized, items.size());
    }

    private List<List<InvocationOutcome>> toBatchOutcomes(SerializedInvocationOutcome serialized, int expected) {
        if (serialized == null) {
            return fillErrors(expected, "Empty worker response");
        }
        if (SerializedInvocationOutcome.KIND_TIMED_OUT.equals(serialized.kind())) {
            // Outer transport kill — whole batch unknown; mark each item timed out.
            List<List<InvocationOutcome>> out = new ArrayList<>(expected);
            for (int i = 0; i < expected; i++) {
                out.add(List.of(InvocationOutcome.timedOut(serialized.stdout())));
            }
            return out;
        }
        if (SerializedInvocationOutcome.KIND_ERROR.equals(serialized.kind())
                && (serialized.batch() == null || serialized.batch().isEmpty())) {
            String message = serialized.errorMessage() != null ? serialized.errorMessage() : "Worker IPC failure";
            return fillErrors(expected, message);
        }
        List<SerializedInvocationOutcome> batch = serialized.batch();
        if (batch == null || batch.isEmpty()) {
            // Single scenario-shaped response (compat)
            return List.of(scenarioSteps(serialized));
        }
        List<List<InvocationOutcome>> out = new ArrayList<>(batch.size());
        for (SerializedInvocationOutcome item : batch) {
            out.add(scenarioSteps(item));
        }
        while (out.size() < expected) {
            out.add(List.of(InvocationOutcome.error("Missing batch item")));
        }
        return out;
    }

    private List<InvocationOutcome> scenarioSteps(SerializedInvocationOutcome serialized) {
        if (serialized != null && serialized.steps() != null && !serialized.steps().isEmpty()) {
            List<InvocationOutcome> outcomes = new ArrayList<>();
            for (SerializedInvocationOutcome step : serialized.steps()) {
                outcomes.add(toInvocation(step));
            }
            return List.copyOf(outcomes);
        }
        return List.of(toInvocation(serialized));
    }

    private static List<List<InvocationOutcome>> fillErrors(int count, String message) {
        List<List<InvocationOutcome>> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(List.of(InvocationOutcome.error(message)));
        }
        return out;
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
