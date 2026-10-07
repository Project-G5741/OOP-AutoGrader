---
title: OT Sync Grade Speed - Plan
type: perf
date: 2026-10-07
topic: ot-sync-grade-speed
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
deepened: 2026-10-07
---

# OT Sync Grade Speed - Plan

## Goal Capsule

**Objective:** Cut sync operational-testcase grade compute so a representative lab (6 challenges, ~60 OT each) finishes Class + MMD + OT on this machine around **~500ms** (under **1s** acceptable), for student upload, lecturer bulk grade, and lecturer dry-run on the shared worker path — without adding a second worker JVM on Render free tier.

**Product authority:** This Product Contract. Neon persist, compile, Class/MMD micro-opts, async OT, and multi-worker concurrency are surrounding work, not active scope.

**Open blockers:** None.

**Stop conditions:** Do not raise `workerJvmSlot` above 1. Do not change scoring, assertion kinds, or student OT card shape. Do not make CI fail solely because a shared runner misses the local ~500ms budget.

**Execution:** Code.

**Product Contract preservation:** unchanged — planning adds HOW sections only; Outstanding Questions resolved into KTDs below.

---

## Product Contract

### Summary

Keep today’s sync contract (full OT-inclusive scores in the same grade response) and make OT compute fast enough for a large lab on a developer machine by using **one** isolated worker, a **lab-wide OT batch**, **warm worker reuse**, and **in-worker parallel scenarios** capped by available processors so Render free tier never runs a second `-Xmx64m` worker process.

### Problem Frame

Student upload and lecturer bulk grade both wait on `gradeSubmission`. When a lab has OT, class+MMD finish quickly, then OT runs sequentially per challenge on a single `workerJvmSlot` session. Live timing shows grade compute ~2.3s for a multi-challenge OT lab (~88% of student wait), with per-challenge OT batches summing to that wall-clock. Prior work already batched OT **within** a challenge; upload-wide batch and warm-pool were deferred. Render free tier shares one cgroup (API `-Xmx256m`, worker `-Xmx64m`); raising host slot concurrency risks OOM and fights the product’s hosting constraint.

### Key Decisions

- **Sync full OT in the response** (session-settled: user-directed — chosen over Class/MMD-first with trailing OT: same contract as today). **Governs R1, R2.**
- **Local grade/OT compute budget, not full upload total** (session-settled: user-directed — chosen over `[timing] Upload total` SLA: Neon persist stays outside the ~500ms number). **Governs R8, R9.**
- **Render constraint is parallelism / one worker, not matching local wall-clock** (session-settled: user-directed — chosen over a Render ~500ms SLA: free-tier headroom is the hosting rule). **Governs R6, R7.**
- **Dry-run shares the speed path** (session-settled: user-directed — chosen over upload/bulk-only: improve dry-run where it shares the worker path). **Governs R3.**
- **One worker JVM + lab-wide batch + warm reuse + in-worker parallel scenarios** (session-settled: user-approved — chosen over A-only and over multi-worker JVMs: local cores buy speed; Render stays one process). **Governs R4, R5, R6, R7, R10.**

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns **sync OT grade-compute speed** for upload, bulk grade, and dry-run under a one-worker hosting rule.

- Per-challenge OT batch invoke (shipped — `docs/plans/2026-09-26-002-perf-testcase-batch-invoke-timeout-plan.md`)
  - **Enables** this work: lab-wide batch extends the same batch contract across challenges.
- Student OT on upload (shipped — `docs/plans/2026-09-25-002-feat-student-operational-testcase-availability-plan.md`)
  - **Shares** the sync OT pillar and `lab_result` testcase payload.
- Lecturer bulk folder grading (shipped — `docs/plans/2026-10-06-001-feat-lecturer-bulk-folder-grading-plan.md`)
  - **Shares** `gradeSubmission`; per-student budget applies, not whole Main-folder wall-clock.
- Neon upload persist / access-cache hot path (shipped)
  - **Can proceed independently of** this plan; outside the grade/OT budget.
- Container sandbox warm-pool / tar create
  - **Can proceed independently of** this plan; not required for the local-worker path in this contract.
- Stale `docs/GRADING_WORKFLOWS.md` sections that still say upload skips OT
  - **Shares** documentation accuracy with this work when docs are touched; not a product behavior change.

### Actors

- A1. Student — uploads a lab folder and waits for complete Class + MMD + OT scores in the upload response.
- A2. Lecturer (bulk) — grades one student folder at a time through bulk grading; waits for complete scores per student call.
- A3. Lecturer (dry-run) — previews OT on the shared worker path.
- A4. API + isolated worker — one worker process at a time on the host; runs OT scenarios and returns outcomes for scoring.

### Requirements

**Sync contract**

- R1. Student upload and lecturer bulk grade still return only after Class, MMD, and applicable OT pillars finish for that grade call. No trailing OT fill-in for the score response.
- R2. Pass/fail and pillar totals for OT must match today’s serial semantics for the same sources and rubric (faster must not change meaning). Parallel scenario execution must preserve isolation so shared student static mutable state cannot cross-contaminate concurrent scenarios.
- R3. Lecturer dry-run that uses the shared worker path receives the same batching, warm-reuse, and in-worker parallelism benefits where applicable (single-testcase dry-run may be a one-item batch).

**Mechanism (product constraints)**

- R4. A grade call with OT across multiple challenges performs lab-wide OT execution in one worker session without a serial per-challenge round-trip tax as the default path (one session’s worth of IPC for the lab’s OT set, not one trip per challenge).
- R5. Idle warm worker reuse is allowed across consecutive upload, bulk, and dry-run calls when safe; cold spawn remains correct when no warm worker is available.
- R6. The host still allows at most one isolated worker JVM at a time (`workerJvmSlot` capacity stays 1). Speed must not come from a second concurrent worker process.
- R7. In-worker scenario concurrency is capped by available processors (and never by spawning another worker JVM). On a 1-CPU host this may stay effectively 1; that is acceptable for Render free tier.
- R10. Scoring rules, assertion kinds, student-visible OT card shape, and attempt/persist semantics stay unchanged except for wall-clock of grade compute.

**Success budgets**

- R8. On a developer machine, for a representative lab of **6 challenges with ~60 OT each**, `[timing] Grade submission` `compute` (or equivalent grade/OT wall-clock) targets **~500ms** preferred and must stay **under 1s** on a warm path when student code is ordinary (no intentional hangs).
- R9. Neon persist, plagiarism inspect, multipart ingest, and compile are outside R8’s budget. Render free-tier wall-clock need not match R8; Render must still obey R6–R7.

### Key Flows

- F1. Student upload with OT
  - **Trigger:** Student uploads a lab folder that includes challenges with OT rows.
  - **Actors:** A1, A4
  - **Steps:** Access + compile + class/MMD as today → one worker session → lab-wide OT (warm reuse if available; in-worker parallel scenarios within CPU cap) → assemble + persist → response with full scores.
  - **Outcome:** Complete OT-inclusive `lab_result` in the upload response; R8 measured on grade compute.
  - **Covered by:** R1, R2, R4, R5, R6, R7, R8

- F2. Lecturer bulk one-student grade
  - **Trigger:** Lecturer bulk grades one student folder for a lab with OT.
  - **Actors:** A2, A4
  - **Steps:** Same `gradeSubmission` OT path as F1 (lecturer disclosure); R8 applies per student call, not to the whole Main folder.
  - **Outcome:** Complete ephemeral scores for that student when the call returns.
  - **Covered by:** R1, R2, R3, R4, R6, R8

- F3. Lecturer dry-run
  - **Trigger:** Lecturer runs OT dry-run on the shared worker path.
  - **Actors:** A3, A4
  - **Steps:** Warm reuse preferred; one-item or small batch; same isolation and timeout meaning as today.
  - **Outcome:** Preview results without persistence; benefits from R5/R7 when applicable.
  - **Covered by:** R3, R5, R6, R7

### Acceptance Examples

- AE1. Large lab warm upload
  - **Covers:** R1, R4, R8
  - **Given:** A lab with 6 challenges and ~60 OT each; warm worker available; ordinary student code.
  - **When:** Student uploads on a developer machine.
  - **Then:** The response includes full OT scores; grade/OT compute is ~500ms preferred and under 1s.

- AE2. Bulk per-student budget
  - **Covers:** R1, R8, F2
  - **Given:** Lecturer bulk-grades a Main folder of many students for that lab.
  - **When:** Each student’s grade call runs.
  - **Then:** R8 is judged per student call; the folder’s total wall-clock may be N× that and still satisfy the contract.

- AE3. Result parity under parallelism
  - **Covers:** R2, R7
  - **Given:** The same submission graded twice (or serial vs parallel mode if planning exposes a check).
  - **When:** OT scenarios run with in-worker concurrency > 1 on a multi-core machine.
  - **Then:** Per-testcase pass/fail and challenge OT pillar totals match the serial meaning for that submission.

- AE4. Render one-worker rule
  - **Covers:** R6, R9
  - **Given:** API on a 512MB free-tier-like host with API heap and one worker heap as today.
  - **When:** A student upload with OT runs (alone or with dry-run contention).
  - **Then:** At most one worker JVM is live; no second worker process is started for speed; wall-clock may exceed R8.

- AE5. Dry-run warm path
  - **Covers:** R3, R5
  - **Given:** Lecturer dry-runs twice within the warm idle window.
  - **When:** The second dry-run runs.
  - **Then:** It benefits from warm reuse (no mandatory cold spawn) and still returns correct preview results.

### Success Criteria

- S1. Local warm path meets R8 on the representative 6×~60 lab.
- S2. Upload, bulk, and dry-run that share the worker path all observe R4–R7 without separate product modes.
- S3. No regression in OT pass/fail meaning (R2) or in non-OT pillars.
- S4. Render deploy guidance remains one-worker-safe (R6); docs that still claim upload skips OT are corrected if touched for this work.

### Scope Boundaries

**In scope**

- Lab-wide OT batching in one worker session for multi-challenge grade calls.
- Warm worker reuse across upload / bulk / dry-run.
- In-worker parallel scenario execution capped by CPU, with isolation preserving R2.
- Timing/success measurement for grade/OT compute (R8).
- Shared path for student upload, lecturer bulk `gradeSubmission`, and dry-run.

**Deferred for later**

- Matching Render free-tier wall-clock to the local ~500ms target.
- Neon persist / access-check / plagiarism inspect latency.
- Compile or Class/MMD performance work beyond what falls out of the OT path.
- Container sandbox warm-pool and tarball create optimizations.
- Raising `workerJvmSlot` above 1 or multi-worker process pools.

**Out of scope**

- Async or trailing OT scores after the HTTP response.
- Changing scoring formulas, assertion kinds, or student OT card UX.
- Changing attempt numbering, persist shape, or plagiarism rules.

### Dependencies / Assumptions

- Per-challenge batch invoke and execution-only timeout semantics already ship.
- Representative lab size for acceptance is 6 challenges × ~60 OT (user-stated current scale).
- Ordinary student code means no intentional infinite loops; hangs still use the existing invoke timeout.
- Developer machine for R8 has multiple cores; 1-CPU hosts may not hit ~500ms and need not for R9.

### Outstanding Questions

**Deferred to Planning**

- None remaining — resolved into KTDs below.

**Resolve Before Planning**

- None.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Lab-wide `batch` with per-item `classesDir`** — extend each batch item so it carries its challenge `classesDir`; one IPC round-trip covers all runnable OT for the grade call. Prefer a single message; chunk only if IPC line-size forces it, and never fall back to one trip per challenge. Chosen over keeping per-challenge `classesDir` on the outer request (cannot span challenges). **Governs R4.**
- KTD2. **In-worker parallel pool capped at `availableProcessors()`** — replace the worker’s single-thread batch executor with a fixed pool of size `max(1, min(itemCount, availableProcessors()))`. Optional `app.grading.ot-batch-parallelism` override may cap lower; default follows CPUs. Never raise `workerJvmSlot`. **Governs R6, R7.**
- KTD3. **Per-item student `URLClassLoader` under parallel** — each concurrent scenario gets its own loader (same classes root bytes, fresh loader) so static mutable state cannot race across concurrent items. Serial semantics for pass/fail are proven by a concurrency=1 vs concurrency>1 fixture. On TIMED_OUT, keep today’s halt-and-respawn-continue contract for remaining items. **Governs R2, AE3.**
- KTD4. **One shared local warm worker cache** — generalize `DryRunWorkerCache` into a host-wide idle local worker (same ~60s idle TTL) borrowed by upload, bulk `gradeSubmission`, and dry-run after `workerJvmSlot` acquire. Sandbox sessions stay uncached. Classes path is per-request via batch items (KTD1), so a warm JVM is not bound to a prior submission root. **Governs R3, R5, AE5.** (session-settled: user-approved — shared pool over dry-run-only cache.)
- KTD5. **R8 is a local/dev gate, not a CI fail** — prove under-1s (prefer ~500ms) with timing-log / optional manual or `@EnabledIf` local probe on a representative fixture; CI must not fail solely because a shared runner is slow. Unit/AE tests own correctness and parity. **Governs R8, R9, S1.** (session-settled: user-approved — local/dev timing over CI SLA.)
- KTD6. **Slot still acquired on the HTTP thread** — upload/bulk/dry-run keep acquire/release of `workerJvmSlot` on the request thread; do not nest worker waits on `gradingExecutor`. **Governs R6** (Render deadlock learning).

### High-Level Technical Design

```
gradeSubmission / dry-run
  → class+MMD (upload/bulk) as today
  → workerJvmSlot.acquire() on HTTP thread
  → SharedLocalWorkerCache.borrow(...)   // or cold open
  → collect runnable OT across challenges → LabOtBatch (items with classesDir)
  → one WorkerSessionHandle.batch(items) // IPC once (or rare size chunks)
       → WorkerInvokeEngine: pool size = CPU cap
            → per item: own URLClassLoader + Future.get(τ)
  → map outcomes → TestcaseGrader evaluation per challenge
  → release session to SharedLocalWorkerCache (or close)
  → workerJvmSlot.release()
```

Transport wait stays `itemCount × timeoutSeconds + 30s` for the (possibly lab-wide) item count. `ProcessWorkerTransport` / `HttpWorkerTransport` invoke mutex still serializes IPC lines; parallelism is inside the worker for one `batch` line.

### Assumptions

- Flattened Java source paths under `backend/src/main/java/grading/...` (not the old nested `com/eiu/...` tree).
- Rebuild worker JAR (`mvn package` / root `npm run backend`) after worker changes.
- Sandbox path: still no second host worker; remote session open rules unchanged; warm cache remains local-only.
- Existing per-challenge batch tests remain valid as a subset (one classesDir for all items).

### Implementation Constraints

- `workerJvmSlot` stays `Semaphore(1)`.
- Do not block `gradingExecutor` workers on worker IPC (slot + session on HTTP thread only).
- Do not set API `JAVA_OPTS=-Xmx512m` on 512MB hosts; worker stays `-Xmx64m`.
- Update DOX: `grading/AGENTS.md`, `grading/testcase/worker/AGENTS.md`, stale `docs/GRADING_WORKFLOWS.md` OT-skip claims, `CONCEPTS.md` isolated-worker / upload-hot-path wording if behavior changes.

### Sequencing

1. U1 — Worker multi-root batch + CPU-capped parallel + per-item loader
2. U2 — Lab-wide OT collect + single batch from `GradingService` / grader path
3. U3 — Shared warm worker cache (upload, bulk, dry-run)
4. U4 — Parity + timing verification hooks
5. U5 — Docs / AGENTS / GRADING_WORKFLOWS drift

U2 depends on U1. U3 can land after U1 (independent of lab-wide collect) but should merge before claiming R5/AE5. U4 tracks U1–U3. U5 last.

### Research Notes

- Today `WorkerInvokeEngine.batch` uses `Executors.newSingleThreadExecutor` and one `URLClassLoader` for the whole challenge batch — serial OT wall-clock inside the worker.
- `ProcessWorkerTransport` / `HttpWorkerTransport` hold an invoke mutex — parallel IPC from the API is not available; in-worker parallel is the lever.
- `DryRunWorkerCache` (60s TTL) is dry-run-only; upload opens a fresh session via `workerSessionFactory.open`.
- Live upload timing: grade ~2.3s dominated by summed per-challenge OT (~250–600ms each); class/MMD ~0–19ms; persist ~170ms outside R8.
- Learnings: `docs/solutions/architecture-patterns/testcase-batch-invoke-execution-timeout.md`, `grading-executor-deadlock-render.md`, `lighting-student-operational-testcases.md`; deferred upload-wide batch in `docs/plans/2026-09-26-002-perf-testcase-batch-invoke-timeout-plan.md`.

---

## Implementation Units

### U1. Worker lab-wide batch: per-item classesDir + CPU-capped parallel

- **Goal:** Worker runs a multi-challenge OT batch in one op with parallel scenarios under the CPU cap and per-item loaders.
- **Requirements:** R2, R4, R6, R7
- **Dependencies:** none
- **Files:**
  - `backend/src/main/java/grading/testcase/worker/WorkerIpc.java`
  - `backend/src/main/java/grading/testcase/worker/WorkerInvokeEngine.java`
  - `backend/src/main/java/grading/testcase/WorkerSessionHandle.java`
  - `backend/src/main/java/grading/testcase/SerializedInvocationOutcome.java` (only if wire shape needs it)
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngineTest.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/IsolatedWorkerAeTest.java`
- **Approach:** Add optional/required `classesDir` on `BatchItemSpec` (lab-wide). Keep backward-compatible outer `classesDir` for single-challenge callers. Engine: pool size per KTD2; each item uses its own loader (KTD3); preserve TIMED_OUT → halt → API respawn-continue. Directional only — do not pre-write the pool wiring.
- **Technical design:** Prefer one NDJSON `batch` response with results in request order. Chunk only when line-size forces it; document chunk merge on the handle.
- **Patterns to follow:** Existing `batch` op and execution-only timeout in `WorkerInvokeEngine`; hang AE in `IsolatedWorkerAeTest`.
- **Test scenarios:**
  - Two items, two different classesDirs → both NORMAL, results ordered.
  - Parallel pool (concurrency > 1) vs forced concurrency=1 on a fixture with static mutable state → same pass/fail (AE3).
  - Hang past τ on one item → TIMED_OUT; remaining items still gradable after respawn (existing continue contract).
  - Missing classesDir on an item → ERROR for that item without crashing the worker process for unrelated items when possible.
- **Verification:** `mvn test -Dtest=WorkerInvokeEngineTest,IsolatedWorkerAeTest` from `backend/`.

### U2. Lab-wide OT collect on gradeSubmission path

- **Goal:** Upload and bulk send one lab-wide batch (not one IPC per challenge) after class+MMD.
- **Requirements:** R1, R4, R10
- **Dependencies:** U1
- **Files:**
  - `backend/src/main/java/grading/GradingService.java`
  - `backend/src/main/java/grading/pipeline/GradingPipeline.java`
  - `backend/src/main/java/grading/pipeline/TestcaseGrader.java`
  - `backend/src/main/java/grading/testcase/InvocationRunner.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/pipeline/TestcaseGraderTest.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/InvocationRunnerTest.java`
  - Existing `GradingService` OT tests if present under `backend/src/test/java/unit/.../grading/`
- **Approach:** After class+MMD phase, build a lab-ordered list of runnable items with per-challenge `classesDir`; one `invokeBatch` (or handle.batch); partition outcomes back to challenges for existing assertion evaluation / pending results. Dry-run may stay one-item batch. Do not change assemble/persist contracts.
- **Patterns to follow:** Today’s `completeOperationalTestcasePhase` + `TestcaseGrader.grade` short-circuit / runnable split.
- **Test scenarios:**
  - Three challenges with OT → one worker `batch` call (mock/fake transport) with items from all three.
  - Challenge without OT skipped in the lab batch; class+MMD scores unchanged.
  - Compile-failed types still short-circuit locally and are not sent as runnable items.
- **Verification:** targeted grader / runner / grading-service tests from `backend/`.

### U3. Shared local warm worker cache

- **Goal:** Upload, bulk, and dry-run reuse one idle local worker under the existing slot.
- **Requirements:** R3, R5, AE5
- **Dependencies:** U1 helpful (classesDir on items); can start after U1
- **Files:**
  - `backend/src/main/java/grading/testcase/DryRunWorkerCache.java` (rename/generalize in place or thin facade)
  - `backend/src/main/java/grading/GradingService.java`
  - `backend/src/main/java/service/TestcaseDryRunService.java`
  - `backend/src/main/java/service/bulk/LecturerBulkGradingService.java` (only if it opens sessions itself — otherwise via `GradingService`)
  - Tests: dry-run cache / session factory tests under `backend/src/test/java/unit/.../grading/testcase/`
- **Approach:** Borrow/release after slot acquire; sandbox never cached; ~60s idle TTL retained unless a short constant rename is clearer. Invalidation: discard idle on process death; do not require rubric invalidation for local worker (classes come from request paths).
- **Patterns to follow:** Current `DryRunWorkerCache.borrow` / `release` + slot acquire order in `TestcaseDryRunService`.
- **Test scenarios:**
  - Second local open within TTL reuses the same process (or handle identity) without spawn.
  - After idle expiry, next borrow spawns cold.
  - Sandbox enabled → never caches.
  - Slot still released on all exit paths (try/finally).
- **Verification:** unit tests for cache + dry-run service paths; smoke upload twice with timing-log `worker_spawn_ms` ~0 on second when warm.

### U4. Parity and local timing verification

- **Goal:** Prove R2 parity and give a local R8 check that does not brick CI.
- **Requirements:** R2, R8, R9, AE1, AE3, S1
- **Dependencies:** U1, U2 (timing); U3 for warm-path timing
- **Files:**
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngineTest.java`
  - Optional new test under `backend/src/test/java/unit/.../grading/` for lab-wide timing probe (`@EnabledIf` / env-gated)
  - Timing surfaces already in `GradingService` / `TimingLog` — extend labels only if needed to show lab-wide OT vs per-challenge
- **Approach:** Automated parity fixture (concurrency 1 vs >1). Local/manual or env-gated timing for 6×~60 under 1s; document the command in Verification Contract. No hard CI fail on R8 alone (KTD5).
- **Test scenarios:**
  - Parity fixture green on multi-core.
  - Env-gated timing test skipped in default CI; when enabled locally, fails if compute ≥ 1000ms on the fixture lab with ordinary code.
- **Verification:** default `mvn test` stays green without the timing gate; local enablement documented.

### U5. DOX and GRADING_WORKFLOWS drift

- **Goal:** Docs match OT-on-upload + lab-wide / parallel / warm contracts; remove stale “upload skips OT” claims.
- **Requirements:** S4, R4, R5, R6, R7
- **Dependencies:** U1–U3 behavior landed
- **Files:**
  - `backend/src/main/java/grading/AGENTS.md`
  - `backend/src/main/java/grading/testcase/worker/AGENTS.md`
  - `docs/GRADING_WORKFLOWS.md` (stale § claiming upload skips OT / student upload does not wait on slot)
  - `CONCEPTS.md` (Upload hot path / Isolated testcase worker — only if wording is wrong after ship)
  - `backend/DEPLOY_RENDER.md` (one-worker note only if concurrency wording needs a one-line update)
- **Approach:** Align pipeline bullets with lab-wide batch + in-worker parallel + shared warm cache; delete contradictory “OT skipped on student upload” lines.
- **Test scenarios:** N/A (docs).
- **Verification:** DOX pass on touched AGENTS; spot-check GRADING_WORKFLOWS §1/§12/§14 for consistency.

---

## Verification Contract

| Gate | Command / check | Proves |
|---|---|---|
| Worker unit | `mvn test -Dtest=WorkerInvokeEngineTest` from `backend/` | U1 |
| Isolated worker AE | `mvn test -Dtest=IsolatedWorkerAeTest` from `backend/` | U1 hang/continue |
| Grader / runner | `mvn test -Dtest=TestcaseGraderTest,InvocationRunnerTest` from `backend/` | U2 |
| Broader OT | `mvn test -Dtest=*Testcase*,*Worker*` from `backend/` when time allows | regression |
| Local timing (optional) | `OT_LAB_TIMING=true mvn test "-Dtest=WorkerInvokeEngineTest#batch_labWideWarmPathUnder1s"` from `backend/`; or timing-log upload on 6×~60 warm path | R8 / AE1 — not CI-blocking |
| Manual warm | two uploads or dry-runs within 60s; second `worker_spawn_ms` ~0 | R5 / AE5 |
| Docs | AGENTS + GRADING_WORKFLOWS consistency | U5 |

---

## Definition of Done

- [x] U1–U5 complete
- [x] Product requirements R1–R10 satisfied
- [x] `workerJvmSlot` still capacity 1; no second worker process for speed
- [x] Worker JAR rebuilt so runtime uses new batch behavior
- [x] Parity tests green; local warm path under 1s on representative lab (manual or env-gated)
- [x] Stale “upload skips OT” doc claims removed
- [x] No change to scoring / assertion kinds / student OT card shape / persist attempt rules
