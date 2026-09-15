---
title: "Container sandbox hardening review caused backend and sandbox-runner test failures"
date: 2026-09-15
category: test-failures
module: backend-grading-sandbox-runner
problem_type: test_failure
component: testing_framework
severity: medium
symptoms:
  - "Backend unit tests failed after stage-3 container sandbox hardening (SandboxInfraErrors visibility, InvocationOutcomeKind vs SerializedInvocationOutcome.kind String, SandboxClassesDirMapper path mapping)"
  - "sandbox-runner SessionServiceLifecycleTest failed on Java 23 with Byte Buddy inline Mockito mock creation errors"
  - "SandboxClassesDirMapper dry-run classes fallback incorrectly mapped absolute paths such as /other/classes"
root_cause: test_isolation
resolution_type: test_fix
related_components:
  - service_object
  - tooling
tags:
  - sandbox
  - testcase-grading
  - mockito
  - java-23
  - code-review
---

# Container sandbox hardening review caused backend and sandbox-runner test failures

## Problem

After the container-sandbox invoke code review, follow-up hardening changes (stricter path mapping, remote-session lifecycle ordering, synchronized HTTP transport teardown, and sandbox-runner session cleanup) landed together with new unit tests. The combined diff left `mvn test` red in both `backend/` and `sandbox-runner/` until several assertion mismatches, visibility issues, and Mockito setup problems were corrected. The fixes restore green tests while preserving the intended production behavior.

## Symptoms

- `backend/` failures in `SandboxClassesDirMapperTest`, `WorkerSessionFactoryTest`, and `WorkerSessionHandleRemoteTest`.
- `sandbox-runner/` failures in `SessionServiceLifecycleTest` when exercising shutdown and TTL sweep behavior.
- Tests asserting infra-error text could not reference `SandboxInfraErrors.STUDENT_MESSAGE` when the constant was package-private.
- Remote-worker tests compared `SerializedInvocationOutcome.kind()` (a `String`) against `InvocationOutcomeKind` enum values.
- `SandboxClassesDirMapperTest.rejectsPathOutsideSubmissionRoot` expected `/other/classes` to throw but nothing was thrown.

## What Didn't Work

- **Loose dry-run path mapping.** Treating any path whose file name was `classes` as a dry-run fallback let absolute paths outside the submission root map into the container instead of failing fast.
- **Package-private infra error constant.** Unit tests in `unit.com.eiu...` packages could not access `SandboxInfraErrors.STUDENT_MESSAGE` when the class was package-private.
- **`mock(WarmPoolManager)` in lifecycle tests.** Mockito inline mocks failed on Java 23 (Byte Buddy agent issues), and mocked pool teardown was not observable for container-destruction assertions.
- **`InvocationOutcomeKind` in remote IPC tests.** `WorkerSessionHandle.invoke()` returns `SerializedInvocationOutcome`, whose `kind()` is a wire-level `String` — not the trusted in-process `InvocationOutcomeKind` enum used after parsing inside `TestcaseGrader`.

## Solution

### 1. Strict `SandboxClassesDirMapper` — only relative `"classes"` for dry-run

Paths under the submission root still map relatively. Lecturer dry-run passes the bare relative dir name `"classes"`. Everything else throws.

```10:20:backend/src/main/java/com/eiu/capstone/backend/grading/testcase/SandboxClassesDirMapper.java
    static String map(String classesDir, Path normalizedRoot, String containerRoot) {
        Path p = Path.of(classesDir).toAbsolutePath().normalize();
        if (p.startsWith(normalizedRoot)) {
            Path rel = normalizedRoot.relativize(p);
            return containerRoot + "/" + rel.toString().replace('\\', '/');
        }
        Path raw = Path.of(classesDir);
        if (!raw.isAbsolute() && raw.normalize().toString().replace('\\', '/').equals("classes")) {
            return containerRoot + "/classes";
        }
        throw new IllegalArgumentException("Classes directory is outside submission root: " + classesDir);
    }
```

### 2. Public `SandboxInfraErrors.STUDENT_MESSAGE`

```3:8:backend/src/main/java/com/eiu/capstone/backend/grading/testcase/SandboxInfraErrors.java
public final class SandboxInfraErrors {

    public static final String STUDENT_MESSAGE = "Sandbox runner unavailable";

    private SandboxInfraErrors() {
    }
```

### 3. `SessionServiceLifecycleTest` — `RecordingFactory` stub instead of Mockito

A test `RecordingFactory` implements `ContainerFactory`, records `destroyContainer` calls, and is wired through a real `WarmPoolManager`. Shutdown and TTL sweep tests assert `factory.destroyed` contains the expected container id.

### 4. Remote tests use `SerializedInvocationOutcome.KIND_ERROR`

```23:30:backend/src/main/java/com/eiu/capstone/backend/grading/testcase/SerializedInvocationOutcome.java
    public static final String KIND_ERROR = "ERROR";
    public static final String KIND_NORMAL = "NORMAL";
    public static final String KIND_THREW = "THREW";
    public static final String KIND_TIMED_OUT = "TIMED_OUT";

    public static SerializedInvocationOutcome error(String message) {
        return new SerializedInvocationOutcome(
                KIND_ERROR, null, "", false, Map.of(), null, List.of(), null, message);
```

Tests assert `SerializedInvocationOutcome.KIND_ERROR` and `SandboxInfraErrors.STUDENT_MESSAGE`, not `InvocationOutcomeKind.ERROR`.

### 5. `SessionService` lifecycle — `@PreDestroy` shutdown and `@Scheduled` TTL sweep

```104:120:sandbox-runner/src/main/java/com/eiu/capstone/sandboxrunner/api/SessionService.java
    @Scheduled(fixedDelayString = "${sandbox.session.sweep-interval-ms:60000}")
    void sweepExpiredSessions() {
        // ...
    }

    @PreDestroy
    void shutdownAllSessions() {
```

### 6. Open remote session before `workerJvmSlot` (sandbox mode)

```134:139:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
        if (workerSessionFactory.isSandboxEnabled()) {
            WorkerSessionHandle workerSession = workerSessionFactory.open(submissionRoot, invokeTimeoutSeconds);
            try {
                long slotWaitStart = System.currentTimeMillis();
                acquireWorkerSlot();
                slotWaitMs = System.currentTimeMillis() - slotWaitStart;
```

Local-process mode remains slot-then-open. Remote session creation (tarball POST) no longer holds the single local worker semaphore during network I/O.

### 7. `HttpWorkerTransport.closeTransport()` synchronized

```95:97:backend/src/main/java/com/eiu/capstone/backend/grading/testcase/transport/HttpWorkerTransport.java
    public void closeTransport() {
        synchronized (invokeMutex) {
            closed = true;
```

Matches `writeLine` and `readLine`, which already synchronize on `invokeMutex`.

## Why This Works

- **Path mapping:** Upload grading passes absolute paths under `submissionRoot`; dry-run passes only the relative token `"classes"`. Absolute outsiders fail closed.
- **Public constant:** One student-safe message avoids drift between factory, client, handle, and tests.
- **RecordingFactory:** Lifecycle tests observe container destruction through the same `WarmPoolManager` → `ContainerFactory` path production uses, without Mockito on Java 23.
- **String kind on wire:** `SerializedInvocationOutcome` is the untrusted IPC DTO; `InvocationOutcomeKind` is the trusted post-parse enum. Tests on the raw handle must assert wire-level strings.
- **Session ordering:** Remote open is network-bound and does not need the local JVM slot; acquiring the slot only around invoke work prevents starving other requests during tarball upload.
- **Synchronized close:** Marking `closed` under the same mutex as read/write prevents races during transport teardown.

## Prevention

- Keep boundary unit tests: `SandboxClassesDirMapperTest` (under-root, dry-run `"classes"`, reject outsider), `WorkerSessionFactoryTest` / `WorkerSessionHandleRemoteTest` (failed remote → `KIND_ERROR` + `STUDENT_MESSAGE`), `SessionServiceLifecycleTest` (shutdown + TTL sweep with `RecordingFactory`).
- When asserting IPC-layer outcomes, use `SerializedInvocationOutcome.KIND_*` string constants; reserve `InvocationOutcomeKind` for `InvocationOutcome` / `TestcaseGrader` paths only.
- For sandbox-enabled flows, open the remote `WorkerSessionHandle` before `workerJvmSlot.acquire()`; keep local-process mode as slot-then-open.
- Prefer small test doubles (`RecordingFactory`) over Mockito for types that own resource lifecycle on Java 23+.
- Keep `SandboxInfraErrors.STUDENT_MESSAGE` public so tests lock the student-visible contract.
- Run `mvn test` in both `backend/` and `sandbox-runner/` after sandbox transport or session lifecycle changes.

## Related Issues

- [Operational testcase grading patterns and pitfalls](../architecture-patterns/operational-testcase-grading.md) — stage-3 sandbox architecture and invoke flow
- [Backend JUnit aspect-home packages](../conventions/backend-junit-aspect-home-packages.md) — aspect-home package layout and Mockito/JDK pitfalls
- `docs/plans/2026-09-15-001-feat-container-sandbox-invoke-plan.md` — implementation plan
- `docs/SANDBOX_RUNNER_DEPLOY.md` — runner deploy and session TTL config
