package com.eiu.capstone.backend.grading.testcase;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.transport.WorkerTransport;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;

/**
 * Per-request worker JVM. Respawn keeps the host slot; callers acquire/release that slot.
 */
public final class WorkerSessionHandle implements AutoCloseable {

    private final WorkerProcessClient client;
    private final int timeoutSeconds;
    private final Object lock = new Object();
    private WorkerSession session;
    private final List<String> restartCommand;
    private String spawnFailure;
    private final AtomicInteger respawns = new AtomicInteger();
    private final long spawnMs;
    private final UnaryOperator<String> classesDirMapper;
    private final boolean remote;

    private WorkerSessionHandle(WorkerProcessClient client,
                                int timeoutSeconds,
                                WorkerSession session,
                                String spawnFailure,
                                long spawnMs,
                                List<String> restartCommand,
                                UnaryOperator<String> classesDirMapper,
                                boolean remote) {
        this.client = client;
        this.timeoutSeconds = timeoutSeconds;
        this.session = session;
        this.spawnFailure = spawnFailure;
        this.spawnMs = spawnMs;
        this.restartCommand = restartCommand;
        this.classesDirMapper = classesDirMapper == null ? UnaryOperator.identity() : classesDirMapper;
        this.remote = remote;
    }

    public static WorkerSessionHandle start(WorkerProcessClient client, int timeoutSeconds) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.start(), null,
                    System.currentTimeMillis() - started, null, UnaryOperator.identity(), false);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, null, UnaryOperator.identity(), false);
        }
    }

    public static WorkerSessionHandle startCommand(WorkerProcessClient client,
                                                   int timeoutSeconds,
                                                   List<String> command) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.startCommand(command), null,
                    System.currentTimeMillis() - started, command, UnaryOperator.identity(), false);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, command, UnaryOperator.identity(), false);
        }
    }

    public static WorkerSessionHandle startTransport(WorkerTransport transport,
                                                     int timeoutSeconds,
                                                     UnaryOperator<String> classesDirMapper,
                                                     long spawnMs,
                                                     boolean remote) {
        return new WorkerSessionHandle(null, timeoutSeconds, new WorkerSession(transport), null,
                spawnMs, null, classesDirMapper, remote);
    }

    public static WorkerSessionHandle failedRemote(String message, long spawnMs) {
        return new WorkerSessionHandle(null, 5, null, message, spawnMs, null, UnaryOperator.identity(), true);
    }

    public long spawnMs() {
        return spawnMs;
    }

    public int respawnCount() {
        return respawns.get();
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
        return roundTrip(request);
    }

    public SerializedInvocationOutcome compare(String classesDir,
                                               TestcaseComparisonMethod comparisonMethod,
                                               List<InstanceRubric> instances) {
        if (instances == null || instances.size() < 2) {
            return SerializedInvocationOutcome.error("Comparison testcase requires two instances");
        }
        WorkerIpc.Request request = new WorkerIpc.Request(
                WorkerIpc.OP_COMPARE,
                classesDirMapper.apply(classesDir),
                null,
                new WorkerIpc.CompareSpec(
                        comparisonMethod != null ? comparisonMethod.name() : "EQUALS",
                        toInstance(instances.get(0)),
                        toInstance(instances.get(1))),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        return roundTrip(request);
    }

    public SerializedInvocationOutcome scenario(String classesDir,
                                                List<InvocationRubric> steps,
                                                List<String> snapshotFieldNames) {
        List<WorkerIpc.ScenarioStepSpec> specs = new java.util.ArrayList<>();
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
        return roundTrip(request);
    }

    private SerializedInvocationOutcome roundTrip(WorkerIpc.Request request) {
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
                        Duration.ofSeconds(Math.max(1, timeoutSeconds)),
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

    private void maybeRespawnLocked() {
        if (!remote) {
            respawnLocked();
        }
    }

    private void respawnLocked() {
        if (remote) {
            session = null;
            return;
        }
        if (session != null) {
            session.close();
            session = null;
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
            if (session != null) {
                session.close();
                session = null;
            }
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

    private static WorkerIpc.ScenarioStepSpec toScenarioStep(InvocationRubric invocation) {
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

    private static WorkerIpc.InstanceSpec toInstance(InstanceRubric instance) {
        return new WorkerIpc.InstanceSpec(
                instance.className(),
                instance.parameterTypes(),
                instance.paramsJson());
    }
}

