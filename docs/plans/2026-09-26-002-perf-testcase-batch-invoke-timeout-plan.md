---
title: Testcase Batch Invoke and Execution-Only Timeout - Plan
type: perf
date: 2026-09-26
topic: testcase-batch-invoke-timeout
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
deepened: 2026-09-26
---

# Testcase Batch Invoke and Execution-Only Timeout - Plan

## Goal Capsule

- **Objective:** Cut operational-testcase wall-clock by batching all OT for a challenge into one worker/sandbox round-trip, and bind the invoke timeout to student code execution only so result return and persist are outside that budget.
- **Product authority:** This Product Contract. Warm-pool sizing, session-create tar speed, upload-wide batching, and DB persist redesign are surrounding work, not active scope.
- **Open blockers:** None.
- **Stop conditions:** Do not change scoring rules, assertion semantics, or student-visible OT card shape. Do not remove per-testcase failure isolation beyond what one challenge batch requires.
- **Execution:** Code. `ce-work` owns the shipping tail for this pipeline run.
- **Product Contract preservation:** Planning resolved Outstanding Questions into KTDs without scope change.

---

## Product Contract

### Summary

Operational testcase grading sends one batched invoke per challenge instead of one remote round-trip per testcase. The configured invoke timeout applies only to student code execution inside the worker; network transfer and result return/save do not consume that budget. Behavior holds for remote sandbox and local worker paths.

### Problem Frame

With sandbox enabled, each testcase today is a separate HTTP round-trip to the runner on a shared session. RTT dominates labs with many OT rows even when student code is fast. The same timeout duration also wraps the full IPC wait, so slow result return can surface as a false TIMED_OUT even when code finished in time. DB detail persist is already off the request invoke clock; the false-timeout risk is the invoke transport and response path.

### Key Decisions

- **Batch + execution-only timeout in one deliverable** (session-settled: user-directed — chosen over timeout-only and batch-only: both RTT and false timeouts hurt). **Governs R1, R4.**
- **Per-challenge batch width** (session-settled: user-approved — agent recommended; chosen over upload-wide and parallel HTTP: most RTT win without reshaping the upload pipeline). **Governs R1, R2.**
- **Keep default timeout at 5 seconds** (session-settled: user-approved — synthesis call-out stood; user had believed ~3s but config remains 5). **Governs R5.**

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns **OT invoke latency and timeout accounting** for the existing worker/sandbox path.

- Isolated testcase worker / container sandbox (already shipped)
  - **Shares** session open, NDJSON IPC, assertion scoring in the API.
  - **Enables** this work: batch and timeout changes ride the same session.
- Upload-wide OT batch / warm-pool / tar create speed
  - **Can proceed independently of** this plan; deferred.

### Actors

- A1. Student — waits on upload when challenges include OT.
- A2. Lecturer — dry-runs OT on the same invoke path.
- A3. API backend — opens one worker/sandbox session per upload or dry-run; scores from serialized outcomes.
- A4. Sandbox runner / local worker — executes student code and returns outcomes.

### Requirements

**Batching**

- R1. For a challenge with N operational testcases, grading performs one worker/sandbox round-trip that executes all N scenarios for that challenge (not N separate trips).
- R2. Each testcase in the batch keeps today’s scenario semantics, assertion evaluation, and per-testcase pass/fail outcome; a TIMED_OUT or ERROR on one testcase does not rewrite unrelated testcases in the same batch as infrastructure failures.
- R3. Lecturer dry-run of a single testcase may use a one-item batch; it must obey the same timeout accounting as upload.

**Timeout accounting**

- R4. The configured invoke timeout bounds only student code execution wall-clock inside the worker for that testcase/scenario. Time spent returning the serialized outcome over IPC/HTTP, and time spent persisting results, must not count against that budget.
- R5. Default timeout remains `app.grading.testcase-invoke-timeout-seconds` at **5** seconds unless operators change the property.
- R6. Transport and client wait budgets must be long enough to cover execution budget plus result return under normal network conditions, without treating a successful on-time execution as TIMED_OUT because return was slow.

**Parity and failure**

- R7. Remote sandbox and local worker paths share the same batch and timeout contracts.
- R8. A hanging or infinite-loop testcase still fails as TIMED_OUT once its execution budget elapses; later challenges on the same session remain gradable after that failure is recorded.

### Key Flows

- F1. Upload OT for one challenge
  - **Trigger:** Challenge has OT rows and a worker/sandbox session is open.
  - **Actors:** A1, A3, A4
  - **Steps:** API builds one batch of scenarios for the challenge → one round-trip → worker runs each scenario under its execution budget → returns outcomes → API evaluates assertions and scores per testcase.
  - **Outcome:** One RTT per challenge with OT; per-testcase results unchanged in meaning.
  - **Covered by:** R1, R2, R4, R7

- F2. Execution timeout without false TIMED_OUT on return
  - **Trigger:** Student code finishes inside the budget; result return is slow.
  - **Actors:** A3, A4
  - **Steps:** Worker stops the execution clock when code completes → returns outcome → transport delivers after code finished.
  - **Outcome:** Testcase is not marked TIMED_OUT solely because return was slow.
  - **Covered by:** R4, R6

- F3. Hang inside a batch
  - **Trigger:** One scenario loops past the execution budget.
  - **Actors:** A3, A4
  - **Steps:** That scenario is TIMED_OUT; remaining batch policy preserves other testcase outcomes per R2; session stays usable for later challenges per R8.
  - **Outcome:** Contained timeout; upload continues.
  - **Covered by:** R2, R8

### Acceptance Examples

- AE1. Challenge with 5 OT rows completes with one sandbox/worker round-trip for that challenge’s OT (not five).
  - **Covers:** R1
- AE2. Code finishes in under 1s; artificial delay before the API receives the outcome does not flip the result to TIMED_OUT.
  - **Covers:** R4, R6
- AE3. One looping testcase in a 3-testcase challenge is TIMED_OUT; the other two still receive real outcomes when they ran.
  - **Covers:** R2, R8
- AE4. Lecturer dry-run of one testcase still times out only on execution overrun, not on post-execution return.
  - **Covers:** R3, R4

### Success Criteria

- Representative lab with multiple OT per challenge shows fewer invoke round-trips equal to OT-bearing challenge count (not testcase count).
- No regression where normal-speed code is graded TIMED_OUT because result return counted against the timeout.
- Existing OT assertion and display behavior remains correct under unit/regression coverage for grader and worker paths.

### Scope Boundaries

**In scope**

- Per-challenge OT batch invoke.
- Execution-only timeout accounting for sandbox and local worker.
- Transport wait policy that satisfies R6.

**Deferred for later**

- Upload-wide single batch across all challenges.
- Warm-pool / container cold-start / tarball create optimizations.
- Changing the default timeout value from 5s to 3s.

**Out of scope**

- Scoring formula or assertion kind changes.
- Moving DB detail UPSERT onto the invoke path or into the timeout budget.
- Class-tab / MMD / compile path changes.

### Dependencies / Assumptions

- A session is already opened once per upload or dry-run when OT applies.
- Parallel challenges may still serialize on the shared session mutex; this plan does not require concurrent invokes on one session.
- Default timeout of 5s remains the product default (R5).

### Outstanding Questions

None blocking. Planning resolved prior deferred items into KTDs below.

### Sources / Research

- Current per-testcase round-trip: `TestcaseGrader` → `InvocationRunner.invokeScenario` → `WorkerSessionHandle.roundTrip` / `HttpWorkerTransport`.
- Timeout wraps `readLine` duration today: `WorkerSessionHandle`, `SessionRecord.invokeLine`.
- Config default: `app.grading.testcase-invoke-timeout-seconds=5` in `backend/src/main/resources/application.properties`.
- Prior sandbox plan: `docs/plans/2026-09-15-001-feat-container-sandbox-invoke-plan.md`.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **New IPC op `batch`** — request carries `classesDir`, `timeoutSeconds`, and `items[]` each with `steps` + `snapshotFieldNames`. Response is top-level `NORMAL` with `batch[]` of per-item scenario outcomes (each may contain `steps`). Chosen over stuffing multiple scenarios into one `scenario` op so unit/composition step semantics stay unchanged. **Governs R1, R2, R3.**
- KTD2. **Worker enforces per-item execution timeout** — each batch item runs on a worker-side `Future.get(timeoutSeconds)`; on timeout emit `TIMED_OUT` for that item and **continue** later items. Chosen over aborting the rest of the batch (would violate R2). Tight-loop threads that ignore interrupt are an accepted residual until the session ends or outer transport kill. **Governs R2, R4, R8.**
- KTD3. **Transport wait = N×τ + return slack** — API/runner `readLine` / HTTP client timeout is `itemCount * timeoutSeconds + 30s` (fixed return slack), not `timeoutSeconds` alone. Chosen so result serialization and network cannot consume the execution budget (R4/R6). Outer kill remains the hang safety net when the worker never returns. **Governs R4, R6, R7.**
- KTD4. **`TestcaseGrader.grade` one batch per challenge** — pre-filter compile-error / failed-type testcases locally; batch only runnable ones; map `batch[]` back by index. `gradeSingle` uses a one-item batch. **Governs R1, R3.**

### High-Level Technical Design

```
TestcaseGrader.grade(challenge)
  → split ERROR shorts vs runnable
  → InvocationRunner.invokeBatch(context, items[])
      → WorkerSessionHandle.batch(...)  // one roundTrip
          → WorkerIpc OP_BATCH
              → for item: Future.get(τ) around scenario(...)
  → evaluate assertions per testcase from batch[i]
```

Transport layers (`ProcessWorkerTransport`, `HttpWorkerTransport`, sandbox `SessionRecord`) continue to receive a single wait duration from `WorkerSessionHandle` for that round-trip; only the duration formula changes (KTD3).

### Assumptions

- Sandbox-runner needs no new HTTP endpoints; existing `/sessions/{id}/invoke` carries the batch NDJSON line.
- `SerializedInvocationOutcome` gains an optional `batch` list; Jackson `@JsonIgnoreProperties(ignoreUnknown=true)` keeps older lines compatible.
- Rebuild worker JAR (`mvn package`) after worker changes so local/sandbox invoke the new op.

### Implementation Constraints

- Do not schedule nested blocking work on `gradingExecutor` / `pillarExecutor` beyond today’s pattern.
- Keep student classes off the API classloader; batch still runs only in the worker JVM.
- Update `backend/.../grading/testcase/worker/AGENTS.md` and parent grading AGENTS for the new op and timeout contract.

### Sequencing

1. U1 — IPC + worker batch + execution timeout
2. U2 — Session handle + transport wait formula
3. U3 — TestcaseGrader + InvocationRunner batch path
4. U4 — Docs / AGENTS parity

U2 can land with U1; U3 depends on both. U4 last.

### Research Notes

- Today TIMED_OUT for hangs originates from API kill after `readLine` timeout, not from inside `WorkerInvokeEngine`.
- `HttpWorkerTransport` currently sets `HttpRequest.timeout(timeout)` equal to invoke τ — that is the false-timeout lever for slow returns.

---

## Implementation Units

### U1. Worker batch op and per-item execution timeout

- **Goal:** Worker accepts `batch`, runs each item as today’s scenario under `timeoutSeconds`, continues after TIMED_OUT.
- **Requirements:** R2, R4, R5, R7, R8
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerIpc.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngine.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/SerializedInvocationOutcome.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngineTest.java`
- **Approach:** Add `OP_BATCH`, `BatchItemSpec`, request fields `timeoutSeconds` + `items`. Engine method `batch(...)` loops items with `Future.get`. Populate `SerializedInvocationOutcome.batch`. Keep `invoke`/`scenario` for tests and AE helpers.
- **Test scenarios:**
  - Two-item batch returns two NORMAL scenario outcomes.
  - First item hangs past τ → TIMED_OUT; second item still runs and returns NORMAL.
  - Missing classesDir → ERROR top-level or per policy matching today’s scenario errors.
- **Verification:** `mvn -pl backend -Dtest=WorkerInvokeEngineTest test` (or repo-equivalent from `backend/`).

### U2. Session transport wait uses execution×N + slack

- **Goal:** Round-trip wait no longer equals bare τ; batch round-trips use KTD3 formula.
- **Requirements:** R4, R6, R7
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/WorkerSessionHandle.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/transport/HttpWorkerTransport.java` (only if needed for clarity)
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/IsolatedWorkerAeTest.java` (timeout AE still green)
- **Approach:** Add `batch(...)` on handle; `roundTrip(request, waitSeconds)` where wait = `max(1, itemCount * timeoutSeconds + 30)`. Single scenario/invoke paths use wait = `timeoutSeconds + 30` so return slack applies even for one-ops. On outer timeout still TIMED_OUT + local respawn as today.
- **Test scenarios:**
  - Fast code with wait >> τ still not TIMED_OUT (existing hang AE still times out when code loops).
  - Batch of 2 uses one IPC write/read (unit or handle-level fake transport).
- **Verification:** `IsolatedWorkerAeTest` hang/timeout cases; new handle/batch unit coverage as added.

### U3. TestcaseGrader challenge batch

- **Goal:** One batch round-trip per challenge; dry-run one-item batch.
- **Requirements:** R1, R2, R3
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/TestcaseGrader.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/InvocationRunner.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/pipeline/TestcaseGraderTest.java` (or existing OT grader tests)
- **Approach:** Collect runnable items; `invokeBatch`; evaluate each pending result from matching batch index. Preserve compile-error short-circuit without sending those items.
- **Test scenarios:**
  - Challenge with 3 runnable OT → runner invoked once with 3 items.
  - Mix of compile-failed + runnable → only runnable batched; failed get ERROR locally.
  - `gradeSingle` still grades one testcase via one-item batch.
- **Verification:** targeted grader + invocation runner tests under `backend/`.

### U4. AGENTS / workflow doc parity

- **Goal:** Document `batch` op and execution-only timeout + transport slack.
- **Requirements:** R4, R5, R7 (contract visibility)
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/AGENTS.md`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`
  - `docs/GRADING_WORKFLOWS.md` (timeout / OT invoke bullets only if they state the old contract)
- **Approach:** Replace “whole-scenario timeout is session readLine budget” with worker execution budget + transport slack wording. Note per-challenge batch.
- **Test scenarios:** N/A (docs).
- **Verification:** DOX pass — nearest AGENTS match behavior.

---

## Verification Contract

| Gate | Command / check | Proves |
|---|---|---|
| Worker unit | `mvn test -Dtest=WorkerInvokeEngineTest` from `backend/` | U1 |
| Isolated worker AE | `mvn test -Dtest=IsolatedWorkerAeTest` from `backend/` | U2 hang/timeout |
| Grader | `mvn test -Dtest=TestcaseGraderTest,InvocationRunnerTest` from `backend/` (adjust names to existing) | U3 |
| Broader OT | `mvn test -Dtest=*Testcase*,*Worker*,*Sandbox*` from `backend/` when time allows | regression |
| Manual (optional) | timing-log upload with sandbox on, multi-OT challenge | R1 RTT reduction |

---

## Definition of Done

- [ ] U1–U4 complete
- [ ] Product requirements R1–R8 satisfied
- [ ] Worker JAR rebuilt so runtime uses new op
- [ ] AGENTS docs match shipped behavior
- [ ] No change to scoring / assertion kinds / student OT card shape
