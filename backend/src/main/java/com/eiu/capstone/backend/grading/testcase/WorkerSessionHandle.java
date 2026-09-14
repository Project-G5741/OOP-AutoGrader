package com.eiu.capstone.backend.grading.testcase;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
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
    private final String spawnFailure;
    private final AtomicInteger respawns = new AtomicInteger();
    private final long spawnMs;

    private WorkerSessionHandle(WorkerProcessClient client,
                                int timeoutSeconds,
                                WorkerSession session,
                                String spawnFailure,
                                long spawnMs,
                                List<String> restartCommand) {
        this.client = client;
        this.timeoutSeconds = timeoutSeconds;
        this.session = session;
        this.spawnFailure = spawnFailure;
        this.spawnMs = spawnMs;
        this.restartCommand = restartCommand;
    }

    public static WorkerSessionHandle start(WorkerProcessClient client, int timeoutSeconds) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.start(), null,
                    System.currentTimeMillis() - started, null);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, null);
        }
    }

    public static WorkerSessionHandle startCommand(WorkerProcessClient client,
                                                   int timeoutSeconds,
                                                   List<String> command) {
        long started = System.currentTimeMillis();
        try {
            return new WorkerSessionHandle(client, timeoutSeconds, client.startCommand(command), null,
                    System.currentTimeMillis() - started, command);
        } catch (WorkerSpawnException e) {
            return new WorkerSessionHandle(client, timeoutSeconds, null, e.getMessage(),
                    System.currentTimeMillis() - started, command);
        }
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
                "invoke",
                classesDir,
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
                "compare",
                classesDir,
                null,
                new WorkerIpc.CompareSpec(
                        comparisonMethod != null ? comparisonMethod.name() : "EQUALS",
                        toInstance(instances.get(0)),
                        toInstance(instances.get(1))),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        return roundTrip(request);
    }

    private SerializedInvocationOutcome roundTrip(WorkerIpc.Request request) {
        synchronized (lock) {
            if (spawnFailure != null) {
                return SerializedInvocationOutcome.error(spawnFailure);
            }
            if (session == null || !session.isAlive()) {
                respawnLocked();
            }
            if (session == null || !session.isAlive()) {
                return SerializedInvocationOutcome.error("Worker stopped");
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
                if (session.process().getInputStream().available() > 0) {
                    respawnLocked();
                    return SerializedInvocationOutcome.error("Worker returned extra IPC lines");
                }
                if (line == null) {
                    respawnLocked();
                    return SerializedInvocationOutcome.error("Worker stopped");
                }
                SerializedInvocationOutcome parsed = WorkerIpc.mapper()
                        .readValue(line, SerializedInvocationOutcome.class);
                if (parsed == null || parsed.kind() == null) {
                    return SerializedInvocationOutcome.error("Malformed IPC JSON");
                }
                return parsed;
            } catch (WorkerSpawnException e) {
                boolean timeout = e.getMessage() != null && e.getMessage().contains("Timed out");
                respawnLocked();
                if (timeout) {
                    return SerializedInvocationOutcome.timedOut("", false);
                }
                return SerializedInvocationOutcome.error("Worker IPC failure");
            } catch (Exception e) {
                respawnLocked();
                return SerializedInvocationOutcome.error("Worker IPC failure");
            }
        }
    }

    private void respawnLocked() {
        if (session != null) {
            session.close();
        }
        try {
            session = restartCommand != null ? client.startCommand(restartCommand) : client.start();
            respawns.incrementAndGet();
        } catch (RuntimeException e) {
            session = null;
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

    private static WorkerIpc.InstanceSpec toInstance(InstanceRubric instance) {
        return new WorkerIpc.InstanceSpec(
                instance.className(),
                instance.parameterTypes(),
                instance.paramsJson());
    }
}
