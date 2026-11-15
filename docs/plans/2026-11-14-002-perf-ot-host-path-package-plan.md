---
title: OT Host-Path Package - Plan
type: perf
date: 2026-11-14
topic: ot-host-path-package
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections; D1–D3 resolved into KTDs
---

# OT Host-Path Package - Plan

## Goal Capsule

- **Objective:** Cut OT-dominated upload wall-clock and slot contention without raising peak worker count or leaving quiet hosts with a permanent parked worker, by shipping one atomic host-path package: early one-lease for `--reflect` + lab-wide `OP_BATCH`, parallelism-aware transport wait, and demand + short clean TTL warm park.
- **Product authority:** This Product Contract. Ideation survivors outside this package (off-slot reflect, challenge-chunked batches, hang shootdown, CRaC) are surrounding candidates, not active scope.
- **Open blockers:** None.
- **Stop conditions:** Do not raise `workerJvmSlot` capacity above 1. Do not put student types into the API JVM. Do not change OT scoring, assertion semantics, or invent a second grader for desktop. Do not park sandbox/remote sessions.
- **Execution:** Code. `ce-work` owns the shipping tail.
- **Product Contract preservation:** Planning resolved Outstanding Questions into KTDs without scope change.

## Product Contract

### Summary

On student upload and lecturer bulk grade, when the lab needs operational testcases, the host takes one early worker lease, runs challenge `--reflect` on that session, then the lab-wide OT batch on the same handle. Transport wait scales with in-worker pool parallelism. After a clean release, the local worker may warm-park under demand + short clean TTL (same policy on cloud and desktop); hang dirt destroys and never parks. Class-tab GETs and no-OT labs keep today’s separate `--reflect` path. Scores stay the same; the win is wall-clock, contention, and idle-RSS discipline.

### Problem Frame

OT often dominates request-thread wall-clock. Today `--reflect` and OT each take `workerJvmSlot` separately (one-shot process, then a second cold open). Host transport wait is still `itemCount × timeout + 30s` even though the worker already parallelizes. Warm reuse exists but defaults to close-on-release with no dirty-bit, so back-to-back uploads always pay spawn cost while free-tier RSS still matters.

### Key Decisions

- **One lease, not off-slot** — early lease covering reflect then `OP_BATCH` on one session. (session-settled: user-directed — chosen over off-slot: same peak worker count.) Governs R1, R2.
- **Atomic package** — lease + transport wait + warm park ship together. (session-settled: user-directed — chosen over phased rollout.) Governs R1–R6.
- **Approach A** — acquire before class+MMD when OT applies; serial reflect on the leased session; then lab-wide batch; then release/park. (session-settled: user-approved — chosen over B/C: clearest one-JVM story; reflect already slot-serialized.) Governs R1, R2.
- **Lease scope** — upload/bulk only when the lab needs OT; Class-tab GET and no-OT labs unchanged. (session-settled: user-directed — chosen over all-worker-users.) Governs R1, R7.
- **Warm park policy** — demand + short clean TTL; destroy on dirt; never park sandbox; same policy cloud and desktop. (session-settled: user-directed — chosen over demand-only / cloud-never-park / desktop-more-aggressive.) Governs R5, R6.
- **Success triad** — wall-clock, slot contention, and idle-RSS together. (session-settled: user-directed — chosen over single-metric success.) Governs S1–S3.

### Actors

- **Student upload** — waits on the upload hot path for scores.
- **Lecturer bulk grade** — same OT host path as upload when OT applies.
- **Lecturer dry-run / Class-tab GET** — may contend on `workerJvmSlot`; dry-run benefits from warm park when policy fires; Class-tab `--reflect` stays on today’s separate path.
- **Host ops (cloud free-tier)** — cares that quiet hosts do not keep a permanent parked worker.

### Key Flows

**F1 — OT lab upload/bulk (in scope)**  
1. Compile completes; grading detects the lab needs OT.  
2. Acquire `workerJvmSlot` once; borrow/open one local (or sandbox) worker session.  
3. For each challenge that needs class parse, run `--reflect` on that session (no second acquire/spawn).  
4. Run class + MMD scoring using those parse results (session still held).  
5. Run lab-wide OT `OP_BATCH` on the same session.  
6. Release: if dirty (hang/abort), destroy; else park when waiters exist or within short clean TTL; sandbox never parks.  
7. Assemble/persist as today.

**F2 — No-OT lab or Class-tab GET (unchanged)**  
Today’s separate `--reflect` acquire/one-shot path remains; no lab-wide OT lease.

**F3 — Transport wait during `OP_BATCH`**  
Host wait budget is based on `⌈itemCount / poolSize⌉ × invokeTimeout + slack`, not serial `itemCount × timeout`.

### Requirements

**One lease**

- **R1.** When upload or bulk grade needs OT, the host acquires `workerJvmSlot` once before challenge class+MMD and holds that lease through `--reflect` and lab-wide `OP_BATCH` on one worker session.
- **R2.** Under that lease, challenge `--reflect` uses the leased session; it must not open a second worker JVM for reflect on that grade.
- **R3.** Student types remain out of the API JVM; scoring still consumes only trusted worker facts.

**Transport wait**

- **R4.** Lab-wide OT transport wait must account for in-worker pool parallelism (`⌈T/P⌉ × timeout + slack`), not treat every item as serial wall-clock. Exact slack is a planning tuning choice that must avoid false outer timeouts under normal load.

**Warm park**

- **R5.** On clean local release, park when another waiter is already queued on the slot, otherwise allow a short clean TTL peek; after hang/abort dirt, destroy and never park that session.
- **R6.** Sandbox/remote sessions are never parked. Cloud and desktop use the same park policy; only resource envelopes (heap/pool caps) may differ.

**Parity / non-regression**

- **R7.** Class-tab GET `--reflect` and no-OT uploads keep today’s separate reflect behavior.
- **R8.** Dashboard upload, bulk grade, and desktop practice keep the same OT grading logic and scores; this package changes host lifecycle and wait math only.
- **R9.** `workerJvmSlot` capacity remains 1.

### Acceptance Examples

- **AE1.** Covers R1, R2. Given an OT lab upload, when grading runs, the host opens at most one worker JVM for reflect+OT (no reflect one-shot then separate OT open on the happy path).
- **AE2.** Covers R4. Given pool size P and T items, transport wait ceiling is based on `⌈T/P⌉` slices, not `T` serial timeouts (plus configured slack).
- **AE3.** Covers R5. Given a clean release with no queued waiters and TTL elapsed, the parked worker is closed so a quiet host does not keep permanent RSS.
- **AE4.** Covers R5. Given a hang/abort dirty session, release destroys the handle; the next borrow cold-opens.
- **AE5.** Covers R6. Given a sandbox-enabled OT grade, release never parks the remote session.
- **AE6.** Covers R7. Given a Class-tab GET or no-OT upload, reflect behavior matches today’s separate path (no early OT lease).
- **AE7.** Covers R8. Given the same submission and rubric, OT pass/fail and pillar scores match pre-change results.

### Success Criteria

- **S1.** Measurable drop in upload grade wall-clock and/or `worker_spawn_ms` on OT labs for warm and cold paths (timing-log).
- **S2.** Under dry-run + upload overlap, `worker_slot_wait_ms` is clearer and not inflated by a second reflect/OT handoff on OT labs.
- **S3.** Quiet free-tier hosts do not retain a permanent parked worker after the short clean TTL.

### Scope Boundaries

**In scope:** early one-lease (Approach A), parallelism-aware transport wait, demand + short clean TTL warm park with dirty destroy, same park policy on cloud/desktop, upload/bulk when OT applies.

**Out of scope / non-goals:** off-slot reflect; `workerJvmSlot > 1`; CRaC/zygote snapshots; challenge-chunked `OP_BATCH`; hang soft-shootdown ladder; changing assertion/scoring rules; parking sandbox sessions; rewriting Class-tab GET reflect into the OT lease protocol.

**Deferred to Follow-Up Work:** heartbeats for progressive transport wait; PriorityClass fair admission among reflect/OT/dry-run; Metaspace-aware pool governor; hang soft-shootdown ladder.

### Dependencies / Assumptions

- `SharedLocalWorkerCache` already supports positive idle TTL park; this work adds demand/dirty policy around it.
- In-worker pool size P is already derived from `ot-batch-parallelism` / CPUs; transport wait consumes that P.
- Longer single hold (class+MMD + OT) is an accepted trade-off for one-lease.

### Outstanding Questions

None blocking. D1–D3 resolved in Planning Contract KTDs.

### Sources / Research

- Ideation: `docs/ideation/2026-11-14-ot-grading-mechanism-ideation.html`
- Wall-clock model: `docs/GRADING_WORKFLOWS.md` §14
- Current OT-after-class+MMD acquire: `backend/.../grading/GradingService.java`
- One-shot reflect: `backend/.../grading/ReflectionClassParser.java`
- Transport formula: `backend/.../grading/testcase/WorkerSessionHandle.java` (`transportWaitSeconds`)
- Warm cache: `backend/.../grading/testcase/SharedLocalWorkerCache.java`
- Concept: `CONCEPTS.md` → OT host lease

---

## Planning Contract

### Key Technical Decisions

- **KTD1. Early lease reorder in `GradingService`** — when `needsWorkerSession`, acquire slot + borrow **before** `computeClassAndMmdPhase`, pass the handle into class+MMD, then `completeOperationalTestcasePhase`, then release/park. (session-settled: user-approved — Approach A.) **Governs R1.**
- **KTD2. Session IPC `reflect` op** — add worker NDJSON op that runs existing `WorkerClassReflector` against a `classesDir` on the live session; `ReflectionClassParser` uses that path when a leased handle is provided, and keeps today’s ProcessBuilder `--reflect` + slot acquire for Class-tab / no-OT. Prefer this over a second JVM under the lease. **Governs R2, R3, R7.**
- **KTD3. Parallel class+MMD, session-locked reflect** — keep `gradingExecutor` parallel challenge workers; reflect IPC on the shared handle is serialized on the session lock (same as batch round-trips). Do not nest blocking submits onto `gradingExecutor`. **Governs R1, R2** and former D3.
- **KTD4. Transport wait = `⌈T/P⌉×τ + 30s`** — change `WorkerSessionHandle.transportWaitSeconds` to use effective slices `ceil(itemCount / max(1, poolSize))` with poolSize from the same parallelism sent on the batch request; keep `RETURN_SLACK_SECONDS = 30`. No heartbeats in this plan. (session-settled: user-approved — formula-only.) **Governs R4** and former D2.
- **KTD5. Default short clean TTL = 5000 ms** — set `app.grading.worker-idle-ttl-ms` default to `5000` (env `WORKER_IDLE_TTL_MS` still overrides); same property on cloud and desktop YAML (do not special-case desktop). (session-settled: user-approved — 5s.) **Governs R5, R6** and former D1.
- **KTD6. Dirty-bit + demand park in `SharedLocalWorkerCache`** — mark handle dirty on hang/abort/respawn paths; `release` destroys dirty or sandbox; clean release parks if `workerJvmSlot.hasQueuedThreads()` **or** `idleTtlMs > 0` (deadline = now + TTL). Capacity stays `Semaphore(1)`. **Governs R5, R6, R9.**

### High-Level Technical Design

```
needsWorkerSession?
  yes:
    acquire workerJvmSlot
    handle = SharedLocalWorkerCache.borrow(...)
    try:
      parallel gradeClassAndMmd(challenge)  // reflect via handle.reflect(classesDir)
      completeOperationalTestcasesLabWide(..., handle)  // OP_BATCH
    finally:
      cache.release(handle)  // dirty→close; else demand|TTL park
      workerJvmSlot.release()
  no:
    today's grade path; ReflectionClassParser one-shot --reflect
```

Transport: `waitSeconds = ceil(T / max(1,P)) * τ + 30`.

### Assumptions

- `WorkerClassReflector` output shape can be reused for the session `reflect` op without changing Declaration scoring.
- `Semaphore.hasQueuedThreads()` is acceptable demand signal (unfair semaphore; waiters still visible).
- Sandbox path already never cached; dirty-bit still applies for local respawn hygiene.

### Implementation Constraints

- Rebuild worker JAR after worker IPC changes (`mvn package` / `npm run backend` path).
- Desktop grading logic parity: same jars and grader; only resource envelopes may differ (`ot-batch-parallelism`, heap).
- Update grading AGENTS / `CONCEPTS.md` Upload hot path wording after behavior ships.
- No nested `join` on `gradingExecutor` beyond today’s pattern.

### Sequencing

1. U1 — Session `reflect` op + parser leased path  
2. U2 — Early lease orchestration in `GradingService` / pipeline  
3. U3 — Parallelism-aware transport wait  
4. U4 — Dirty-bit + demand/TTL warm park + default 5s  
5. U5 — Docs / AGENTS / CONCEPTS parity  

U1 before U2. U3 and U4 can proceed in parallel after U1/U2 land or alongside if carefully isolated. U5 last.

### Research Notes

- Today OT labs already run class+MMD **before** OT acquire (`GradingService`); reflect still separately acquires inside `ReflectionClassParser.parseViaWorker`.
- Lab-wide batch and in-worker pool already exist; only the host wait formula lags §14’s `⌈T/W⌉` model.

---

## Implementation Units

### U1. Session reflect IPC and leased parser path

- **Goal:** Leased worker sessions can parse classes without a second JVM; Class-tab / no-OT keep one-shot `--reflect`.
- **Requirements:** R2, R3, R7
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerIpc.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerMain.java` / `WorkerClassReflector.java` (as needed)
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/WorkerSessionHandle.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/ReflectionClassParser.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/...` (new or existing worker/session tests)
  - `backend/src/test/java/unit/.../ReflectionClassParser...` (if present; else add focused unit)
- **Approach:** Add `OP_REFLECT` (name per existing IPC style) with `classesDir`; reuse `WorkerClassReflector`. Handle method `reflect(Path)`. Parser accepts optional `WorkerSessionHandle`; when non-null, call session reflect and **do not** acquire `workerJvmSlot` or `ProcessBuilder`. When null, keep today’s path.
- **Test scenarios:**
  - Session reflect returns same class list shape as one-shot for a tiny classes dir.
  - Parser with null handle still acquires slot / one-shot (or in-process fallback).
  - Parser with handle never calls `workerJvmSlot.acquire()`.
- **Verification:** targeted unit tests from `backend/`.

### U2. Early OT host lease orchestration

- **Goal:** OT upload/bulk acquires once before class+MMD and holds through batch.
- **Requirements:** R1, R2, R8, R9
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/ChallengeGradingContext.java` (only if needed to thread handle)
  - unit/support tests around grading orchestration if present
- **Approach:** Reorder `needsWorkerSession` branch per KTD1/KTD3; pass handle into `gradeClassAndMmd` → parser. Ensure bulk grade uses the same service path. Keep `workerJvmSlot` capacity 1.
- **Test scenarios:**
  - OT lab grade: one borrow/open for reflect+batch (mock factory/cache counts).
  - No-OT lab: no early borrow; parser uses separate path.
  - Happy-path scoring unchanged for a fixture with OT (AE7).
- **Verification:** orchestration unit tests; existing OT grader tests still green.

### U3. Parallelism-aware transport wait

- **Goal:** Host wait matches in-worker parallelism.
- **Requirements:** R4
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/WorkerSessionHandle.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/WorkerSessionHandleLocalInfraTest.java` (or new focused test)
- **Approach:** Implement KTD4; pass effective pool size into wait math (same value as batch `batchParallelism` after resolve). Keep 30s slack. No heartbeat protocol.
- **Test scenarios:**
  - `transportWaitSeconds(6, 3, pool=2) == 3*3 + 30` (ceil(6/2)=3).
  - `pool=1` matches today’s `itemCount * τ + 30` shape.
  - Existing hang AE still times out on infinite loops.
- **Verification:** unit formula tests + `IsolatedWorkerAeTest` smoke.

### U4. Dirty-bit and demand + 5s warm park

- **Goal:** Clean local workers warm-park briefly / on demand; dirty and sandbox never park.
- **Requirements:** R5, R6, R9
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/SharedLocalWorkerCache.java`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/WorkerSessionHandle.java` (dirty flag / mark on abort)
  - `backend/src/main/resources/application.properties` (default `worker-idle-ttl-ms=5000`)
  - `backend/src/main/resources/application-desktop.yml` (ensure no conflicting `0` override unless intentional — same policy)
  - `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/SharedLocalWorkerCacheTest.java`
- **Approach:** Implement KTD5/KTD6. `release(factory, handle, slot)` or inject `Semaphore` for `hasQueuedThreads`. Mark dirty when session aborts/respawns after TIMED_OUT.
- **Test scenarios:**
  - Clean release with TTL>0 parks and second borrow skips open.
  - Dirty release closes; next borrow opens fresh.
  - Sandbox release always closes.
  - Queued waiters → park even when verifying demand branch (fake semaphore or test hook).
  - After TTL elapses, idle handle is not reused (`hasIdleLocal` false / borrow opens).
- **Verification:** `SharedLocalWorkerCacheTest` + related handle tests.

### U5. Docs and vocabulary parity

- **Goal:** Contracts match shipped behavior.
- **Requirements:** R1–R6 visibility; S1–S3 observability notes
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`
  - `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/AGENTS.md`
  - `docs/GRADING_WORKFLOWS.md` §9 / §14 wait formula if still serial
  - `CONCEPTS.md` (`Upload hot path`, `Isolated testcase worker`, `OT host lease`)
- **Approach:** Describe early lease, session reflect, `⌈T/P⌉` wait, dirty/demand park, default 5s TTL. Note Class-tab still one-shot.
- **Test scenarios:** N/A (docs).
- **Verification:** DOX pass on touched AGENTS.

---

## Verification Contract

| Gate | Command / check | Proves |
|---|---|---|
| Cache / park | `mvn test -Dtest=SharedLocalWorkerCacheTest` from `backend/` | U4 |
| Transport / handle | `mvn test -Dtest=WorkerSessionHandleLocalInfraTest,IsolatedWorkerAeTest` from `backend/` | U3 hang safety |
| Reflect / worker | targeted new reflect + existing `*Worker*` unit tests from `backend/` | U1 |
| Orchestration / OT | `mvn test -Dtest=*Testcase*,*Grading*` (narrow to existing suites that cover OT) from `backend/` | U2, AE7 |
| Full backend (pre-merge) | `mvn test` from `backend/` | regression |
| Manual optional | `app.grading.timing-log=true` OT lab upload; confirm single spawn + lower wait | S1, S2 |

---

## Definition of Done

- [x] U1–U5 complete
- [x] R1–R9 satisfied; AE1–AE7 covered by automated or documented manual checks
- [x] Worker JAR rebuilt after IPC changes
- [x] Default `worker-idle-ttl-ms=5000`; sandbox never parked; dirty never parked
- [x] `workerJvmSlot` capacity remains 1
- [x] No OT scoring / assertion semantic changes
- [x] AGENTS / CONCEPTS / GRADING_WORKFLOWS match behavior
