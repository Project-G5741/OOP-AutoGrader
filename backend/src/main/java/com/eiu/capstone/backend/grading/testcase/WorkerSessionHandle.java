package com.eiu.capstone.backend.grading.testcase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.transport.WorkerTransport;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;

/**
 * Per-request worker JVM. Respawn keeps the host slot; callers acquire/release that slot.
 */
public final class WorkerSessionHandle implements AutoCloseable {

    /** Extra seconds beyond execution budget so result return does not consume the invoke timeout. */
    public static final int RETURN_SLACK_SECONDS = 30;

    private final WorkerProcessClient client;
    private final int timeoutSeconds;
    private final Object lock = new Object();
    private WorkerSession session;
    private final List<String> restartCommand;
    private String spawnFailure;
    private final AtomicInteger respawns = new AtomicInteger();
    private final long spawnMs;
    private UnaryOperator<String> classesDirMapper;
    private final boolean remote;
    private final Supplier<RemoteSessionParts> remoteReopen;

    private WorkerSessionHandle(WorkerProcessClient client,
                                int timeoutSeconds,
                                WorkerSession session,
                                String spawnFailure,
                                long spawnMs,
                                List<String> restartCommand,
                                UnaryOperator<String> classesDirMapper,
                                boolean remote,
                                Supplier<RemoteSessionParts> remoteReopen) {
        this.client = client;
        this.timeoutSeconds = timeoutSeconds;
        this.session = session;
        this.spawnFailure = spawnFailure;
        this.spawnMs = spawnMs;
        this.restartCommand = restartCommand;
        this.classesDirMapper = classesDirMapper == null ? UnaryOperator.identity() : classesDirMapper;
        this.remote = remote;
        this.remoteReopen = remoteReopen;
    }

    public static WorkerSessionHandle start(WorkerProcessClient client, int timeoutSeconds) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.start(), null,
                    System.currentTimeMillis() - started, null, UnaryOperator.identity(), false, null);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, null, UnaryOperator.identity(), false, null);
        }
    }

    public static WorkerSessionHandle startCommand(WorkerProcessClient client,
                                                   int timeoutSeconds,
                                                   List<String> command) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.startCommand(command), null,
                    System.currentTimeMillis() - started, command, UnaryOperator.identity(), false, null);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, command, UnaryOperator.identity(), false, null);
        }
    }

    public static WorkerSessionHandle startTransport(WorkerTransport transport,
                                                     int timeoutSeconds,
                                                     UnaryOperator<String> classesDirMapper,
                                                     long spawnMs,
                                                     boolean remote) {
        return startTransport(transport, timeoutSeconds, classesDirMapper, spawnMs, remote, null);
    }

    public static WorkerSessionHandle startTransport(WorkerTransport transport,
                                                     int timeoutSeconds,
                                                     UnaryOperator<String> classesDirMapper,
                                                     long spawnMs,
                                                     boolean remote,
                                                     Supplier<RemoteSessionParts> remoteReopen) {
        return new WorkerSessionHandle(null, timeoutSeconds, new WorkerSession(transport), null,
                spawnMs, null, classesDirMapper, remote, remoteReopen);
    }

    public static WorkerSessionHandle failedRemote(String message, long spawnMs) {
        return new WorkerSessionHandle(null, 5, null, message, spawnMs, null, UnaryOperator.identity(), true, null);
    }

    public long spawnMs() {
        return spawnMs;
    }

    public int respawnCount() {
        return respawns.get();
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public SerializedInvocationOutcome invoke(String classesDir,
                                              InvocationRubric invocation,
                                              List<String> snapshotFieldNames) {
        WorkerIpc.Request request = new WorkerIpc.Request(
                WorkerIpc.OP_INVOKE,
                classesDirMapper.apply(classesDir),
                toInvokeSpec(invocation),
                null,
                snapshotFieldNames,
                WorkerIpc.DEFAULT_STDOUT_CAP);
        SerializedInvocationOutcome outcome = roundTrip(request, transportWaitSeconds(1));
        if (WorkerIpc.shouldAbortSession(outcome)) {
            synchronized (lock) {
                clearSessionLocked();
                respawnLocked();
            }
        }
        return outcome;
    }

    public SerializedInvocationOutcome scenario(String classesDir,
                                                List<InvocationRubric> steps,
                                                List<String> snapshotFieldNames) {
        List<WorkerIpc.ScenarioStepSpec> specs = new ArrayList<>();
        if (steps != null) {
            for (InvocationRubric step : steps) {
                specs.add(toScenarioStep(step));
            }
        }
        WorkerIpc.Request request = new WorkerIpc.Request(
                WorkerIpc.OP_SCENARIO,
                classesDirMapper.apply(classesDir),
                null,
                null,
                snapshotFieldNames,
                WorkerIpc.DEFAULT_STDOUT_CAP,
                specs);
        SerializedInvocationOutcome outcome = roundTrip(request, transportWaitSeconds(1));
        if (WorkerIpc.shouldAbortSession(outcome)) {
            synchronized (lock) {
                clearSessionLocked();
                respawnLocked();
            }
        }
        return outcome;
    }

    /**
     * One or more round-trips until every item has an outcome. After a hang timeout the worker
     * process is killed (worker halt + local close) and remaining items run on a fresh JVM.
     */
    public SerializedInvocationOutcome batch(String classesDir, List<WorkerIpc.BatchItemSpec> items) {
        if (items == null || items.isEmpty()) {
            return SerializedInvocationOutcome.error("Missing batch items");
        }
        List<SerializedInvocationOutcome> merged = new ArrayList<>(items.size());
        int offset = 0;
        while (offset < items.size()) {
            List<WorkerIpc.BatchItemSpec> slice = items.subList(offset, items.size());
            WorkerIpc.Request request = new WorkerIpc.Request(
                    WorkerIpc.OP_BATCH,
                    classesDirMapper.apply(classesDir),
                    null,
                    null,
                    null,
                    WorkerIpc.DEFAULT_STDOUT_CAP,
                    null,
                    slice,
                    Math.max(1, timeoutSeconds));
            SerializedInvocationOutcome partial = roundTrip(request, transportWaitSeconds(slice.size()));
            if (partial == null) {
                return SerializedInvocationOutcome.error(SandboxInfraErrors.STUDENT_MESSAGE);
            }
            if (SerializedInvocationOutcome.KIND_ERROR.equals(partial.kind())
                    && (partial.batch() == null || partial.batch().isEmpty())) {
                if (merged.isEmpty()) {
                    return partial;
                }
                while (merged.size() < items.size()) {
                    merged.add(SerializedInvocationOutcome.error(
                            partial.errorMessage() != null ? partial.errorMessage() : SandboxInfraErrors.STUDENT_MESSAGE));
                }
                return SerializedInvocationOutcome.batchOf(merged);
            }
            if (SerializedInvocationOutcome.KIND_TIMED_OUT.equals(partial.kind())
                    && (partial.batch() == null || partial.batch().isEmpty())) {
                // Outer transport timeout — remaining unknown.
                while (merged.size() < items.size()) {
                    merged.add(SerializedInvocationOutcome.timedOut("", false));
                }
                synchronized (lock) {
                    clearSessionLocked();
                    respawnLocked();
                }
                return SerializedInvocationOutcome.batchOf(merged);
            }
            List<SerializedInvocationOutcome> part = partial.batch() == null ? List.of() : partial.batch();
            merged.addAll(part);
            offset = merged.size();
            boolean timedOut = WorkerIpc.shouldAbortSession(partial);
            if (!timedOut) {
                break;
            }
            synchronized (lock) {
                clearSessionLocked();
                if (offset >= items.size()) {
                    respawnLocked();
                    break;
                }
                respawnLocked();
                if (session == null || !session.isAlive()) {
                    while (merged.size() < items.size()) {
                        merged.add(SerializedInvocationOutcome.timedOut("", false));
                    }
                    break;
                }
            }
        }
        return SerializedInvocationOutcome.batchOf(merged);
    }

    static int transportWaitSeconds(int itemCount, int timeoutSeconds) {
        return Math.max(1, Math.max(1, itemCount) * Math.max(1, timeoutSeconds) + RETURN_SLACK_SECONDS);
    }

    private int transportWaitSeconds(int itemCount) {
        return transportWaitSeconds(itemCount, timeoutSeconds);
    }

    private SerializedInvocationOutcome roundTrip(WorkerIpc.Request request, int waitSeconds) {
        synchronized (lock) {
            if (session == null || !session.isAlive()) {
                maybeRespawnLocked();
            }
            if (session == null || !session.isAlive()) {
                return SerializedInvocationOutcome.error(
                        spawnFailure != null ? spawnFailure : SandboxInfraErrors.STUDENT_MESSAGE);
            }
            String json;
            try {
                json = WorkerIpc.mapper().writeValueAsString(request);
            } catch (Exception e) {
                return SerializedInvocationOutcome.error("Failed to encode IPC request");
            }
            try {
                session.writeLine(json);
                String line = session.readLine(
                        Duration.ofSeconds(Math.max(1, waitSeconds)),
                        WorkerProcessClient.IPC_LINE_CAP_BYTES);
                if (line == null) {
                    maybeRespawnLocked();
                    return SerializedInvocationOutcome.error(SandboxInfraErrors.STUDENT_MESSAGE);
                }
                SerializedInvocationOutcome parsed = WorkerIpc.mapper()
                        .readValue(line, SerializedInvocationOutcome.class);
                if (parsed == null || parsed.kind() == null) {
                    return SerializedInvocationOutcome.error("Malformed IPC JSON");
                }
                return parsed;
            } catch (WorkerTimeoutException e) {
                maybeRespawnLocked();
                return SerializedInvocationOutcome.timedOut("", false);
            } catch (WorkerSpawnException e) {
                maybeRespawnLocked();
                return SerializedInvocationOutcome.error(SandboxInfraErrors.STUDENT_MESSAGE);
            } catch (Exception e) {
                maybeRespawnLocked();
                return SerializedInvocationOutcome.error(SandboxInfraErrors.STUDENT_MESSAGE);
            }
        }
    }

    private void clearSessionLocked() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    private void maybeRespawnLocked() {
        respawnLocked();
    }

    private void respawnLocked() {
        clearSessionLocked();
        if (remote) {
            if (remoteReopen == null) {
                return;
            }
            try {
                RemoteSessionParts parts = remoteReopen.get();
                if (parts == null || parts.transport() == null) {
                    spawnFailure = SandboxInfraErrors.STUDENT_MESSAGE;
                    return;
                }
                session = new WorkerSession(parts.transport());
                if (parts.classesDirMapper() != null) {
                    classesDirMapper = parts.classesDirMapper();
                }
                spawnFailure = null;
                respawns.incrementAndGet();
            } catch (RuntimeException e) {
                session = null;
                spawnFailure = e.getMessage() != null ? e.getMessage() : SandboxInfraErrors.STUDENT_MESSAGE;
            }
            return;
        }
        if (client == null) {
            return;
        }
        try {
            session = restartCommand != null ? client.startCommand(restartCommand) : client.start();
            spawnFailure = null;
            respawns.incrementAndGet();
        } catch (RuntimeException e) {
            session = null;
            if (spawnFailure == null || spawnFailure.isBlank()) {
                spawnFailure = e.getMessage();
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            clearSessionLocked();
        }
    }

    private static WorkerIpc.InvokeSpec toInvokeSpec(InvocationRubric invocation) {
        return new WorkerIpc.InvokeSpec(
                invocation.kind().name(),
                invocation.className(),
                invocation.methodName(),
                invocation.parameterTypes(),
                invocation.paramsJson(),
                invocation.receiverClassName(),
                invocation.receiverParameterTypes(),
                invocation.receiverParamsJson());
    }

    static WorkerIpc.ScenarioStepSpec toScenarioStep(InvocationRubric invocation) {
        return new WorkerIpc.ScenarioStepSpec(
                invocation.kind().name(),
                invocation.className(),
                invocation.methodName(),
                invocation.parameterTypes(),
                invocation.paramsJson(),
                invocation.receiverClassName(),
                invocation.receiverParameterTypes(),
                invocation.receiverParamsJson(),
                invocation.instanceName(),
                invocation.dispatchClassName());
    }

    /**
     * Fresh remote transport + classes-dir mapper after a hang kill.
     */
    public record RemoteSessionParts(
            com.eiu.capstone.backend.grading.testcase.transport.WorkerTransport transport,
            UnaryOperator<String> classesDirMapper) {}
}
