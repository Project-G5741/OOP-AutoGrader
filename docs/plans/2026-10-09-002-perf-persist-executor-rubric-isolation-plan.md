---
title: Persist executor rubric isolation - Plan
date: 2026-10-09
type: perf
topic: persist-executor-rubric-isolation
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Persist executor rubric isolation - Plan

## Goal Capsule

**Objective:** Keep upload rubric-cache overlap (`labRubricCache.get` / `rubricFuture.join`) off the shared `persistExecutor` queue so it cannot wait behind plagiarism inspect or other long persist work.

**Product authority:** Session 2026-10-09 brainstorm. Detail-gate waits, detail multi-commit UPSERT, and Hikari/Neon keepalive are not active scope for this plan.

**Open blockers:** None.

## Product Contract

### Summary

Introduce a dedicated executor used only for upload-time rubric cache load that overlaps compile. Leave `persistExecutor` for detail UPSERT, plagiarism inspect, sidecars, and temp delete. Never schedule this work on `gradingExecutor`.

### Key Decisions

- **Upload critical path is the finish line** — success is `rubricFuture.join()` not queued behind inspect/long persist work. (session-settled: user-directed — chosen over also protecting detail-gate waits) Governs R1, R5.
- **`gradingExecutor` stays free of persist work** — no nesting of rubric, detail, inspect, or sidecars on the challenge pool. (session-settled: user-directed — chosen over nesting on gradingExecutor) Governs R2.
- **Dedicated rubric-overlap executor** — separate small pool for upload rubric load only. (session-settled: user-directed — chosen over split critical/background pools or raise-size-only) Governs R3, R4.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns only isolating upload rubric overlap from `persistExecutor` starvation.

The broader Neon / DB-slowness checklist from the 2026-10-09 analysis is the current understanding, not a committed roadmap:

- `persistExecutor` detail-gate / Class-tab waits behind inspect
  - Can proceed independently; deferred (user chose upload path only)
- Detail UPSERT multi-commit Neon round trips
  - Can proceed independently; deferred
- Hikari / Neon suspend keepalive and socket timeouts
  - Can proceed independently; deferred
- Plagiarism Neon connection release during compare (`docs/plans/2026-10-09-001-perf-plagiarism-neon-connection-release-plan.md`)
  - Can proceed independently; already planned/shipped; shortens inspect hold time but does not remove queue contention with rubric overlap

### Problem Frame

`persistExecutor` is a fixed two-thread pool shared by rubric overlap, detail UPSERT, plagiarism inspect, sidecars, and temp delete. When both threads are busy with long inspect (or similar), the next upload’s `rubricFuture.join()` blocks the HTTP critical path even if the rubric is already in memory. That is thread-pool starvation in front of Neon, not a slow query plan.

### Requirements

**Isolation**

- R1. Upload-time rubric cache load that overlaps compile must not share an executor queue with plagiarism inspect, detail UPSERT, sidecars, or temp-folder delete.
- R3. A dedicated executor exists solely for that upload rubric-overlap work (not for post-persist background tasks).
- R4. Concurrent uploads may still contend on the dedicated rubric executor if it has few threads; that is acceptable as long as inspect never shares it.

**Safety invariants**

- R2. Rubric overlap, detail UPSERT, plagiarism inspect, sidecars, and temp delete must not run on `gradingExecutor` (no nested work on the challenge pool).
- R6. Upload HTTP success path, grading semantics, and plagiarism detection semantics stay unchanged aside from reduced queue wait for the rubric join.

**Boundaries**

- R5. This work does not require Class/MMD/testcase detail-gate waits to stop queuing behind inspect; that remains deferred.
- R7. This work does not change detail UPSERT transaction batching or Hikari/Neon keepalive settings.

### Key Flows

- F1. Upload with busy persist pool
  - **Trigger:** One or more plagiarism inspect (or other persist) tasks occupy `persistExecutor` while another student uploads.
  - **Steps:** Rubric load is scheduled on the dedicated rubric-overlap executor overlapping compile; upload joins that future after compile.
  - **Outcome:** Rubric join does not wait for inspect to finish on `persistExecutor`.
  - **Covered by:** R1, R3

- F2. Normal upload, quiet pools
  - **Trigger:** Upload when both pools are idle.
  - **Steps:** Same overlap + join; scores and off-thread persist/inspect behave as today.
  - **Outcome:** Behavior unchanged except executor routing for rubric load.
  - **Covered by:** R6

### Acceptance Examples

- AE1. Covers R1, R3. Given `persistExecutor` threads are blocked in long inspect, When another upload runs rubric overlap, Then `rubricFuture.join()` completes without waiting for those inspect tasks to finish.
- AE2. Covers R2. Given the change is deployed, When upload and post-persist work run, Then none of rubric overlap / detail UPSERT / inspect / sidecars / temp delete are scheduled on `gradingExecutor`.
- AE3. Covers R6. Given a normal upload, When grading and persist complete, Then student scores and inspect scheduling still succeed as today (inspect still on `persistExecutor`).

### Success Criteria

- Rubric overlap for upload is structurally isolated from inspect/detail/sidecar/temp-delete queues (R1, R3).
- `gradingExecutor` remains free of that persist family of work (R2).
- No intentional change to detection or score semantics (R6).

### Scope Boundaries

**In scope:** Dedicated executor for upload rubric-cache overlap; wiring `SubmissionController` rubric `supplyAsync` to that executor; DOX for the new pool.

**Deferred for later:** Protecting `SubmissionDetailPersistGate` waits from inspect; raising shared `persistExecutor` size as the primary fix; moving detail UPSERT onto the rubric pool; detail multi-commit batching; Hikari keepalive / socket timeout.

**Out of scope:** Putting persist work on `gradingExecutor`; changing plagiarism detection rules; schema migrations.

### Dependencies / Assumptions

- Rubric overlap remains `labRubricCache.get(lab)` scheduled before compile and joined after compile (`SubmissionController`).
- `persistExecutor` remains the home for detail UPSERT, inspect, sidecars, and temp delete unless a later plan moves them.

### Sources / Research

- `backend/src/main/java/config/PersistExecutorConfig.java` — 2-thread `persistExecutor` (not CPU-capped).
- `backend/src/main/java/controller/SubmissionController.java` — `rubricFuture` on `persistExecutor` + `joinRubric`.
- `docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md` — detail UPSERT must not nest on `gradingExecutor`.
- `docs/plans/2026-10-09-001-perf-plagiarism-neon-connection-release-plan.md` — related Neon inspect work; does not fix rubric queue contention.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | New Spring bean `rubricOverlapExecutor`: fixed pool of **2** daemon threads named `rubric-overlap-worker`, **not** CPU-capped via `FixedExecutorFactory` | Same I/O posture as `persistExecutor` (Neon/cache wait). Size 2 allows two concurrent uploads without inventing priority; R4 still holds if more contend. Governs R3, R4. |
| KTD2 | Add the bean next to persist config (same class or a sibling `@Configuration`); inject into `SubmissionController` with `@Qualifier("rubricOverlapExecutor")` and use it **only** for the rubric `supplyAsync` | Smallest wiring change. Leave sidecars / inspect / temp delete / detail UPSERT on `persistExecutor`. |
| KTD3 | Do **not** use `gradingExecutor` or `compileExecutor` for rubric overlap | Honors R2; compile pool is CPU-capped and wrong for Neon-bound cache load. |
| KTD4 | Prove AE1 structurally: code review + DOX that rubric `supplyAsync` uses `rubricOverlapExecutor` exclusively. No multi-threaded integration harness required in this plan | Controller has no unit test for executor routing today; structural isolation is the product guarantee. |
| KTD5 | Update `PersistExecutorConfig` comment and AGENTS pool table so “rubric overlap” is no longer listed under `persistExecutor` | Prevents re-wiring onto the shared pool later. |

### Assumptions

- `LabRubricCache.get` remains safe to call from a background thread (already true today on `persistExecutor`).
- Two public `ExecutorService` beans remain distinguishable by `@Qualifier` (existing pattern for `persistExecutor`).

### High-Level Technical Design

Directional only — not implementation specification.

```text
Upload HTTP thread
  ├─ supplyAsync(labRubricCache.get, rubricOverlapExecutor)   // NEW pool
  ├─ compile (compileExecutor / request path as today)
  └─ joinRubric(rubricFuture)

persistExecutor (unchanged membership)
  ├─ detail UPSERT
  ├─ sidecars
  ├─ plagiarism inspect
  └─ temp delete

gradingExecutor — never hosts the above
```

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Third pool to reason about on 1-CPU Render | 2 daemon threads; I/O-wait friendly; document in AGENTS table |
| Concurrent uploads > 2 queue on rubric pool | Accepted (R4); still never behind inspect |
| Accidental reuse of bean for sidecars | DOX + qualifier name; KTD2 limits injection site |

### Open Questions

**Deferred (non-blocking):** None blocking.

## Implementation Units

### U1. Add `rubricOverlapExecutor` bean

- **Goal:** Provide a dedicated, non–CPU-capped pool for upload rubric overlap.
- **Requirements:** R2, R3, R4
- **Approach:** Add `@Bean(destroyMethod = "shutdown") ExecutorService rubricOverlapExecutor()` mirroring `persistExecutor` (fixed size 2, daemon threads `rubric-overlap-worker`). Prefer extending `PersistExecutorConfig` or a small sibling config class. Do not route through `FixedExecutorFactory` (that CPU-caps). Update the `persistExecutor` comment to drop “rubric overlap.” Honors KTD1, KTD5.
- **Files:** `backend/src/main/java/config/PersistExecutorConfig.java` (and/or new config class in the same package)
- **Patterns:** `PersistExecutorConfig.java` (fixed uncapped pool)
- **Test scenarios:**
  - Happy: application context still creates both `persistExecutor` and `rubricOverlapExecutor` beans (smoke via existing Spring tests if any load the context; otherwise review + compile).
- **Test expectation:** none dedicated unless an existing context test already loads executors — prefer DOX + compile; optional thin `@SpringBootTest` only if cheap and already patterned.
- **Dependencies:** None

### U2. Wire upload rubric `supplyAsync` to the new pool

- **Goal:** Stop scheduling upload rubric load on `persistExecutor`.
- **Requirements:** R1, R3, R6
- **Approach:** Inject `@Qualifier("rubricOverlapExecutor") ExecutorService` into `SubmissionController`. Change only the rubric `CompletableFuture.supplyAsync(..., executor)` call site. Leave sidecar, inspect, and temp-delete `runAsync` on `persistExecutor`. Honors KTD2, KTD3.
- **Files:** `backend/src/main/java/controller/SubmissionController.java`
- **Patterns:** Existing `@Qualifier("persistExecutor")` injection in the same controller.
- **Test scenarios:**
  - Happy / AE3: no behavioral change expected for grading outcomes; regression is “upload still compiles and joins rubric” covered by existing upload/integration paths if present; otherwise manual smoke.
  - Edge / AE1 structural: rubric `supplyAsync` argument is `rubricOverlapExecutor` (code review).
- **Execution note:** Characterization-first — confirm current call site uses `persistExecutor`, then switch.
- **Verification:** `mvn -f backend/pom.xml compile` (and any existing controller/upload tests that still apply)
- **Dependencies:** U1

### U3. DOX: document `rubricOverlapExecutor`

- **Goal:** Make the isolation rule durable so future edits do not re-merge rubric onto `persistExecutor`.
- **Requirements:** R1, R2, R3; supports AE1/AE2
- **Approach:** Update `backend/AGENTS.md` pipeline sentence and executor table: rubric overlap on `rubricOverlapExecutor`; `persistExecutor` lists detail/sidecars/inspect/temp delete only. Touch `docs/GRADING_WORKFLOWS.md` / `docs/HOW_IT_RUNS.md` / `grading/AGENTS.md` only where they still say rubric overlaps compile on `persistExecutor`.
- **Files:** `backend/AGENTS.md`; `docs/GRADING_WORKFLOWS.md` (if needed); optionally `backend/src/main/java/grading/AGENTS.md`, `docs/HOW_IT_RUNS.md`
- **Test expectation:** none — docs only
- **Dependencies:** U2

## Verification Contract

- `mvn -f backend/pom.xml compile` (minimum)
- Prefer existing backend tests that exercise upload if practical: `mvn -f backend/pom.xml test` when time allows
- AE1 checklist: `SubmissionController` rubric `supplyAsync` uses `@Qualifier("rubricOverlapExecutor")`; inspect/sidecars/temp delete still use `persistExecutor`
- AE2 checklist: no new scheduling of persist-family work onto `gradingExecutor`
- AE3: upload path unchanged aside from executor routing

## Definition of Done

- Upload rubric overlap runs on `rubricOverlapExecutor`, not `persistExecutor` (R1, R3).
- `gradingExecutor` still hosts no persist-family work (R2).
- `persistExecutor` membership documented without rubric overlap (KTD5).
- No change to detail UPSERT batching or Hikari settings (R7).
- Detail-gate waits behind inspect remain deferred (R5).
