---
title: Grading Speed Optimization - Plan
type: perf
date: 2026-07-31
topic: grading-speed
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections only
---

# Grading Speed Optimization - Plan

## Goal Capsule

**Objective:** Reduce student upload-to-score wait time for a typical medium lab submission (4–8 challenges, several `.java` files each) to approximately **5 seconds**, while keeping the **final score in the same synchronous upload response**.

**Product authority:** This plan owns backend grading-pipeline performance for the existing upload flow (`DropZone` → `POST /api/submissions/{labId}/{attemptNumber}/upload`). Frontend score display, async polling UX, and weighted scoring model changes are not active scope unless noted as deferred context.

**Open blockers:** None.

## Product Contract

### Summary

Optimize the synchronous submission pipeline by eliminating redundant work (per-challenge rubric fetches, wasted `.mmd` I/O, SHA-256 overhead), processing challenges in parallel within the request, and caching lab rubrics in memory with documented invalidation. Keep Java Reflection as the comparison mechanism.

### Problem Frame

Today a student upload blocks on a single HTTP request that sequentially saves files, compiles Java per challenge, loads rubric data from PostgreSQL per challenge, reflects over compiled classes, persists results, and deletes temp files. For medium labs this stacks 4–8 compile passes and repeated rubric queries on top of reflection work. Students see only "Uploading..." until the entire pipeline finishes, so every backend second is perceived wait time.

### Key Decisions

- **Hot-path parallelization over async grading** — Governs R1, R2, R3. Chosen over return-early/poll because the student must receive the final score in the upload response. (session-settled: user-directed — chosen over async_ok: sync UX is non-negotiable.)
- **Rubric preload + documented in-memory cache** — Governs R4, R5. Chosen over per-request-only optimization because deadline traffic will hit the same lab rubric repeatedly; optional shared cache (e.g., Redis) may be documented for multi-instance deployments.
- **Direct attribute comparison over SHA-256** — Governs R7. Chosen for speed; hashing added no product value once weighted scoring is handled at the score-aggregation layer, not the element-match layer.
- **Keep Java Reflection** — Governs R8. Chosen over bytecode introspection because the team is familiar with reflection and the bigger wins are compile parallelism and rubric caching.

### Requirements

**Pipeline latency**

- R1. A typical medium submission (4–8 challenges, several `.java` files per challenge) completes upload through final score in approximately **5 seconds** under normal single-student load on the deployed backend.
- R2. The upload API continues to return the **final score** in the same synchronous response; no poll-or-notify step is introduced for students.
- R3. Challenge folders within one submission may be compiled, reflected, and compared **in parallel** within the request, using a **bounded** concurrency limit documented for operators.

**Rubric access**

- R4. The full rubric for the submitted lab is loaded **once per grading request** (all challenges, classes, fields, methods, constructors, and parameters needed for comparison) instead of repeated per-challenge database round-trips.
- R5. A **documented in-memory rubric cache** keyed by lab reduces repeat database access across submissions; cache invalidation rules are documented and triggered when rubric data for that lab changes.

**I/O and comparison efficiency**

- R6. `.mmd` files are **not required for current grading** and must not block or delay compile-and-grade work; the pipeline must leave a clear extension point for near-future MMD persistence without reintroducing sequential bottlenecks.
- R7. Element attribute matching uses **direct comparison** of normalized attribute tuples instead of per-check cryptographic hashing.
- R8. Student class structure is extracted via the existing **Java Reflection** path (`ReflectionClassParser` behavior preserved at the product level).

**Operability**

- R9. Any new infrastructure (thread pools, cache stores, optional Redis) is **documented** with configuration knobs, defaults, and invalidation behavior suitable for deployment (e.g., `backend/DEPLOY_RENDER.md` or equivalent ops doc).
- R10. Grading correctness and score semantics for the current rubric model remain unchanged unless a separate weighted-scoring initiative explicitly scopes scoring-rule changes.

### Key Flows

- F1. **Optimized synchronous upload-to-score**
  - **Trigger:** Student drops a folder in `DropZone`; frontend POSTs multipart files with auth.
  - **Steps:** Authenticate → group files by challenge → for each challenge in parallel (bounded): write `.java` sources, compile to `classes/`, reflect and compare against preloaded rubric → aggregate challenge scores → persist submission results → delete temp submission folder → return `SubmissionUploadResponse` with score.
  - **Covers R1, R2, R3, R4, R6, R7, R8.**

- F2. **Rubric cache lifecycle**
  - **Trigger:** First submission for a lab after process start or after cache invalidation.
  - **Steps:** Load full lab rubric from database → store in memory cache → subsequent submissions for that lab read from cache → on rubric admin mutation (or documented TTL fallback), invalidate that lab's cache entry.
  - **Covers R5, R9.**

### Acceptance Examples

- AE1. **Medium lab within target**
  - **Given:** A lab with 6 challenges and 2–3 `.java` files per challenge, rubric already cached.
  - **When:** A student uploads the folder via `DropZone`.
  - **Then:** The HTTP response returns within ~5 seconds with a numeric score; no second request is required.
  - **Covers R1, R2.**

- AE2. **Cold cache still sync**
  - **Given:** Backend restarted; rubric cache empty.
  - **When:** First student submits that lab.
  - **Then:** Response still includes final score synchronously; cold-cache path may exceed 5s but must not require polling.
  - **Covers R2, R5.**

- AE3. **MMD present but not on critical path**
  - **Given:** Upload includes `.mmd` and `.java` files for each challenge.
  - **When:** Grading runs.
  - **Then:** Compile-and-grade completes without reading `.mmd`; `.mmd` handling does not serialize challenge processing ahead of compilation.
  - **Covers R6.**

- AE4. **Rubric edit invalidates cache**
  - **Given:** Lab rubric cached from prior submissions.
  - **When:** Lecturer changes rubric data for that lab (per documented invalidation trigger).
  - **Then:** Next submission grades against updated rubric, not stale cache.
  - **Covers R5, R10.**

### Success Criteria

- p50 upload-to-score ≤ **5 seconds** for medium submissions with warm rubric cache on reference hardware (to be baselined during planning/verification).
- No regression in grading outcomes for a fixed set of sample submissions compared to pre-optimization behavior (per R10).
- Operator documentation lists concurrency limits, cache behavior, and optional multi-instance cache notes (per R9).

### Scope Boundaries

**In scope**

- Backend upload pipeline: `SubmissionStorageService`, `JavaCompilerService`, `GradingService`, `ReflectionClassParser` integration points.
- Rubric loading strategy and cache contract.
- Documented runtime configuration for parallelism and caching.

**Deferred for later**

- MMD-based grading or mandatory `.mmd` archival before response (near-future; extension point only in this work).
- Weighted challenge or per-element scoring model (current simple average across challenges remains unless separately scoped).
- Async job queue with poll-for-score UX.
- Frontend surfacing upload score in `DropZone` / `StudentDashboard` (response already contains score; UI wiring is separate).
- Bytecode-parser replacement for reflection.

**Outside this product's identity**

- Changing what is graded (e.g., class relations, inheritance) for speed.

### Dependencies / Assumptions

- Backend runs on a **JDK** (compilation required).
- PostgreSQL rubric schema remains the source of truth; cache is a performance layer only.
- Typical concurrent load is moderate (classroom deadline, not massive national-scale burst) unless planning discovers otherwise.
- Current scoring is a simple average across graded challenges; future weighting will be a separate product change.

### Outstanding Questions

**Resolved in planning**

- OQ1 → KTD2: default `app.grading.parallelism=4`, overridable; cap at `min(config, availableProcessors)`.
- OQ2 → KTD5: skip `.mmd` disk write on the hot path for v1; collect via `MmdPersistenceHook` no-op implementation with interface ready for post-grade persistence.
- OQ3 → KTD3: v1 uses in-process cache with 30-minute TTL per lab plus explicit `invalidate(labId)` for future lecturer rubric mutations; Redis documented as multi-instance follow-up.
- OQ4 → Verification Contract: manual benchmark on local JDK backend with a fixed 6-challenge sample folder; log milestone timings.

### Sources / Research

- `backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java` — synchronous upload → grade → respond → cleanup.
- `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionStorageService.java` — sequential per-challenge compile; `.mmd` written but unused in grading.
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java` — per-challenge rubric DB queries; SHA-256 `hash()` per element.
- `backend/src/main/java/com/eiu/capstone/backend/grading/ReflectionClassParser.java` — `URLClassLoader` per challenge.
- `frontend/src/components/ui/DropZone.jsx` — single blocking fetch until full pipeline completes.
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` — pipeline and scoring contracts.

## Planning Contract

### Technical approach

Refactor the upload handler into three phases with clear boundaries:

1. **Prepare** (single-threaded, fast): auth, rubric snapshot from cache/DB, group multipart files by challenge.
2. **Process** (parallel, CPU-bound): per challenge — write `.java`, compile, reflect, compare against in-memory rubric slice. No database access in this phase.
3. **Persist** (single-threaded, transactional): upsert submission + result rows, update student progress, delete temp folder, return response.

This split keeps `@Transactional` off the parallel phase (per KTD1) and avoids connection-pool contention during `javac` and reflection.

### Key technical decisions

- KTD1. **Split CPU work from DB transaction** — Governs R3, R10. `GradingService` exposes a pure `gradeAgainstSnapshot(...)` that returns result DTOs; a separate `@Transactional persistGradingResults(...)` writes them. Chosen because parallel grading inside one JPA transaction risks lazy-loading and connection issues.
- KTD2. **Bounded fixed thread pool for challenges** — Governs R3, R9. Default parallelism **4**, property `app.grading.parallelism`, effective value `min(property, Runtime.getRuntime().availableProcessors())`. Chosen over unbounded `ForkJoinPool.commonPool()` to protect Render free-tier memory when 8 `javac` tasks would otherwise stack.
- KTD3. **In-process rubric cache with TTL** — Governs R4, R5. `LabRubricService` loads a `LabRubricSnapshot` (immutable graph keyed by challenge number) via batched repository calls; `LabRubricCache` stores snapshots by `labId` with 30-minute TTL and `invalidate(UUID labId)`. Chosen as simplest v1; document Redis only in ops notes for horizontal scale.
- KTD4. **Direct tuple comparison** — Governs R7. Replace `hash(String...)` with private `attributesMatch(expected, actual)` using case-normalized string equality on the same tuple fields. Remove `MessageDigest` usage from the hot path.
- KTD5. **MMD off hot path via hook interface** — Governs R6. Introduce `MmdPersistenceHook` with `NoOpMmdPersistenceHook` default; `SubmissionStorageService` stops calling `file.transferTo` for `.mmd` during `processChallenge`. Files remain in multipart payload if a future hook persists them after grading. Chosen because MMD is not needed now and disk writes were serializing challenge processing.
- KTD6. **Instrumentation milestones** — Governs R1. Log `upload_ms`, `rubric_ms`, `process_ms`, `persist_ms`, `total_ms` at INFO when `app.grading.timing-log=true` (default true in dev profile only).

### Sequencing

```
U1 LabRubricSnapshot + LabRubricService
  → U2 LabRubricCache
  → U3 Parallel SubmissionStorageService.processUpload
  → U4 GradingService split + direct compare
  → U5 MmdPersistenceHook
  → U6 Timing logs
  → U7 Ops docs + AGENTS.md updates
```

U3 and U4 can be developed together in one PR since grading depends on snapshot shape from U1.

### Risks and mitigations

| Risk | Mitigation |
|---|---|
| Parallel `javac` exhausts memory on small instances | Default parallelism 4; document `JAVA_OPTS` and `app.grading.parallelism` in deploy guide |
| Stale rubric cache after DB edit | TTL 30m + `invalidate`; wire invalidation when lecturer rubric API exists |
| Race on double-submit same attempt | Unchanged — `requestId` folder isolation already handles overlapping uploads |
| Correctness regression from compare change | Hash and direct compare are equivalent for current tuple fields; verify with fixed sample submissions (AE1, R10) |

## Implementation Units

### U1. Lab rubric snapshot loader

**Covers R4.** Add immutable `LabRubricSnapshot` (and nested types) under `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/` holding everything `gradeChallenge` needs per challenge number: classes with scope/declaringType, fields, methods, constructors, ordered parameter types.

Add `LabRubricService` with `LabRubricSnapshot loadForLab(Lab lab)`:

- `challengeRepository.findByLabOrderByChallengeNumberAsc(lab)`
- Batch-fetch all `ClassEntity` rows for those challenges (`findByChallengeWithAttributes` in one query via new `findByChallengeInWithAttributes(List<Challenge>)` on `ClassEntityRepository`)
- Batch-fetch fields, methods, constructors, parameters using existing `findByClassEntityIn*` repository methods once per entity type

**Pattern:** Follow existing `JOIN FETCH` queries in `ClassEntityRepository`, `FieldRepository`, etc.

**Files:** `grading/rubric/LabRubricSnapshot.java`, `grading/rubric/LabRubricService.java`, `repository/ClassEntityRepository.java` (new batch query)

### U2. In-memory rubric cache

**Covers R5, R9.** Add `LabRubricCache` wrapping `LabRubricService`:

- Key: `lab.getId()`
- Value: `LabRubricSnapshot`
- TTL: 30 minutes (`app.grading.rubric-cache-ttl-minutes`, default 30)
- `invalidate(UUID labId)` public for future rubric-admin integration

Use `ConcurrentHashMap` + `Instant` expiry or Caffeine if already on classpath (check `pom.xml` first; prefer zero-dependency map if none).

**Files:** `grading/rubric/LabRubricCache.java`, `application.properties` keys

### U3. Parallel challenge processing

**Covers R1, R3, R6.** Refactor `SubmissionStorageService.processUpload`:

- Inject configured `ExecutorService` bean (`GradingExecutorConfig`) with fixed pool size from KTD2
- Replace sequential `for` over `byChallenge` with `CompletableFuture.supplyAsync` per challenge calling extracted `processChallenge(...)`
- Each challenge still writes only `.java`, compiles, counts `.class` files — no `.mmd` write (U5)
- Await all futures before returning `ProcessResult`; propagate first `SubmissionProcessingException`

**Files:** `service/SubmissionStorageService.java`, new `config/GradingExecutorConfig.java`

### U4. GradingService CPU/DB split and direct compare

**Covers R7, R8, R10.** Refactor `GradingService`:

- New method `GradingComputationResult computeScore(LabSubmission submission, LabRubricSnapshot rubric, List<ChallengeResult> folders)` — no `@Transactional`, no repository writes
- Lookup expected challenge by number from snapshot instead of `challengeRepository.findByLabAndChallengeNumber`
- Replace `hash(...)` calls with `attributesMatch` on the same fields documented in `grading/AGENTS.md`
- Keep `ReflectionClassParser.parseClasses` unchanged
- Existing `gradeSubmission` becomes thin orchestrator: load existing result maps → `computeScore` → `persistGradingResults` (`@Transactional`)

**Files:** `grading/GradingService.java`, optionally `grading/GradingComputationResult.java`

### U5. MMD persistence hook

**Covers R6.** Add interface:

```java
public interface MmdPersistenceHook {
    void onUploadComplete(String irn, String requestId, Map<String, List<MultipartFile>> mmdByChallenge);
}
```

Default `NoOpMmdPersistenceHook` bean. Remove `.mmd` `transferTo` from `processChallenge`. Document in `service/AGENTS.md` how a future implementation can persist after grading without blocking compile.

**Files:** `service/MmdPersistenceHook.java`, `service/NoOpMmdPersistenceHook.java`, `SubmissionStorageService.java`

### U6. Pipeline orchestration and timing

**Covers R1, R2, F1.** Update `SubmissionController.upload`:

1. `Instant start = ...`
2. `LabRubricSnapshot rubric = labRubricCache.get(lab)` (log `rubric_ms`)
3. `processUpload` parallel (log `process_ms`)
4. Save submission shell if needed
5. `gradingService.gradeSubmission(submission, rubric, result.challenges)` (log `persist_ms` inside persist phase)
6. Return response; `finally` delete folder (log `total_ms`)

**Files:** `controller/SubmissionController.java`

### U7. Operator documentation

**Covers R9.** Document in `backend/DEPLOY_RENDER.md` and `backend/AGENTS.md`:

| Property | Default | Purpose |
|---|---|---|
| `app.grading.parallelism` | `4` | Max concurrent challenge workers |
| `app.grading.rubric-cache-ttl-minutes` | `30` | In-process rubric cache TTL |
| `app.grading.timing-log` | `false` (prod) | Milestone timing logs |

Note: multi-instance deployments need shared cache (Redis) or accept per-instance TTL staleness until lecturer invalidation API exists.

Update `grading/AGENTS.md` and `service/AGENTS.md` for new pipeline shape.

## Verification Contract

No automated test suite exists (`backend/AGENTS.md`). Verify manually:

### Benchmark (R1, AE1)

1. Start backend with JDK: `npm start` or `./mvnw spring-boot:run` from `backend/`
2. Enable timing: `app.grading.timing-log=true`
3. Upload a **6-challenge** sample folder (2–3 `.java` per challenge) twice via `DropZone` or Swagger `POST /api/submissions/{labId}/1/upload`
4. **Warm cache run:** `total_ms` ≤ 5000 on developer hardware (record machine spec in PR notes)
5. **Cold cache run:** restart backend, first upload may exceed 5s but must return score synchronously (AE2)

### Correctness (R10, AE4)

1. Before changes: upload a known-good sample; record score and per-element results (DB or logs)
2. After changes: same upload must produce **identical score**
3. After manual `labRubricCache.invalidate(labId)` or TTL expiry, grading still succeeds (AE4 deferred until rubric admin API — TTL-only test for v1)

### Regression checks

- `mvn -q -DskipTests compile` from `backend/`
- Compile failure still returns `SubmissionProcessingException` to client
- Empty `.java` challenge still grades with 0% for missing classes

## Definition of Done

**Global**

- All U1–U7 complete
- Product Contract R1–R10 satisfied or explicitly deferred with documented reason
- `artifact_readiness: implementation-ready` on this plan
- AGENTS.md chain updated for touched packages

**Per unit**

| Unit | Done when |
|---|---|
| U1 | `LabRubricService.loadForLab` returns full graph in ≤5 DB round-trips for an 8-challenge lab |
| U2 | Second submission for same lab skips rubric DB queries (confirm via SQL log or timing) |
| U3 | 6-challenge upload uses parallel workers (thread names or timing drop vs sequential baseline) |
| U4 | No `MessageDigest` in `GradingService`; scores match pre-refactor sample |
| U5 | `.mmd` files not written to disk; hook interface present and wired |
| U6 | Milestone logs emitted when `app.grading.timing-log=true` |
| U7 | Deploy doc lists all new properties and parallelism guidance |
