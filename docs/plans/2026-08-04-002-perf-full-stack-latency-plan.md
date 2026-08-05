---
title: Full-Stack Latency Optimization - Plan
date: 2026-08-04
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections only
supersedes_upload_shape: docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md (R9 scores-only)
extends: docs/plans/2026-07-31-001-perf-grading-speed-plan.md (read path + login)
---

# Full-Stack Latency Optimization - Plan

## Goal Capsule

**Objective:** Cut end-to-end student-facing wait time on **local dev** to aggressive targets: login under 2 seconds, dashboard stats under 1 second, challenge Class tab under 2 seconds, and a medium lab upload (4–8 challenges) under 5 seconds — while keeping synchronous upload-with-score, Java Reflection grading, and PostgreSQL rubrics.

**Product authority:** Session brainstorm (2026-08-04), confirmed after synthesis. Supersedes the upload-response bundle requirement from `docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md` for performance. Extends `docs/plans/2026-07-31-001-perf-grading-speed-plan.md`, which already addressed most grading-pipeline optimizations.

**Open blockers:** None.

## Product Contract

### Summary

Eliminate algorithmic latency across login, student dashboard reads, challenge display, and upload by replacing N+1 database access with batched queries, caching stable lookup data, parallelizing independent frontend fetches, and returning scores-only from upload (class detail on demand). Grading remains synchronous with final score in the upload response.

### Problem Frame

Students wait minutes to use the app even on localhost. Login feels stuck for 10+ seconds before the dashboard appears. Stats cards take ~7 seconds. Clicking a challenge can take minutes when the rubric is large. Uploads block until the full pipeline finishes. The July 2026 grading plan fixed parallel compile/grade and rubric caching, but **read paths** (`ChallengeService`, `ClassStructureService`) still issue per-class and per-challenge query storms, the upload response builds class DTOs N times, and the frontend loads labs → challenges → class → stats sequentially.

### Actors

- A1. **Student** — logs in, views dashboard, switches challenges, uploads submissions; expects sub-second UI after initial load.
- A2. **Developer/operator** — runs locally via `npm start`; needs measurable latency targets to verify fixes.

### Requirements

**Latency targets (local dev, medium lab ~5 challenges, warm JVM)**

- R1. **Login to interactive dashboard** completes in under **2 seconds** from successful auth response to first paint of student shell (excluding Google OAuth widget latency outside app control).
- R2. **Stats cards** (current grade, total submissions, latest submission) populate in under **1 second** after lab is selected, measured from request start to rendered values.
- R3. **Challenge Class tab** loads in under **2 seconds** when a challenge is selected (cache miss path), for a medium challenge rubric.
- R4. **Upload to score** for a medium submission completes in under **5 seconds** synchronously, returning final lab score.

**Read-path efficiency**

- R5. Challenge sidebar scores for a lab are computed with **bounded database round-trips** — submission results for the latest attempt are loaded once per request, not repeated per challenge.
- R6. Class tab data for one challenge is built with **batched queries** for all classes, fields, methods, constructors, and parameters in that challenge — no per-class or per-constructor query loops.
- R7. **Master data** (scope/type labels) is served from an in-process cache with TTL or load-once semantics — not `findAll()` on every class request.
- R8. **Grading result reads** for display and re-upload avoid lazy-loading N+1 on field/method/constructor associations when loading existing submission results.

**Upload response shape**

- R9. Upload response returns **per-challenge scores** (keyed by challenge UUID) and overall score — **not** full class detail bundles. Class tab data is fetched via existing `/class` when the student selects a challenge.
- R10. Upload continues to return the **final score synchronously**; no async poll-for-score UX.

**Frontend loading**

- R11. Independent API calls after lab selection (challenges list, stats) run **in parallel** where they do not depend on each other.
- R12. Challenge detail fetch does not re-fire unnecessarily when the challenges list updates but the selected challenge is unchanged.

**Login efficiency**

- R13. Auth endpoints avoid **lazy-load N+1** for roles and other data needed to build the login response (e.g., eager fetch or DTO projection).
- R14. Google login path minimizes avoidable blocking work after token verification; external `tokeninfo` call remains acceptable if total login still meets R1.

**Correctness and scope preservation**

- R15. Grading outcomes and score semantics remain unchanged for the same inputs (per R10 and existing rubric model).
- R16. Latest-attempt display semantics for scores and class data remain as implemented — performance work must not revert to best-submission reads.

**Observability**

- R17. Milestone timing is measurable for login, dashboard load, challenge class fetch, and upload — via existing `app.grading.timing-log` or equivalent request-level timing — so regressions are detectable on local dev.

### Key Flows

- F1. **Fast dashboard load**
  - **Trigger:** Student lands on dashboard after login; lab already selected or auto-selected.
  - **Steps:** Parallel fetch challenges (with scores) + stats → render sidebar and stat cards → on challenge select, single batched `/class` fetch (or cache hit).
  - **Covers R2, R3, R5, R6, R11, R12.**

- F2. **Fast upload**
  - **Trigger:** Student drops folder in `DropZone`.
  - **Steps:** Auth → rubric cache → parallel compile/grade → persist results → return scores map + overall score (no class DTO build) → frontend refreshes challenges/stats in parallel → student opens challenge → `/class` batched fetch.
  - **Covers R4, R9, R10, R11.**

- F3. **Fast login**
  - **Trigger:** IRN/password or Google auth succeeds.
  - **Steps:** Single round-trip auth with eager-loaded user data → JWT + user id in response → mount dashboard.
  - **Covers R1, R13.**

### Acceptance Examples

- AE1. **Challenge click no longer minutes**
  - **Given:** Medium lab with 5 challenges, student has a latest submission with graded results.
  - **When:** Student selects a challenge and opens the Class tab.
  - **Then:** Class content appears within 2 seconds on local dev; server issues O(1) batched query groups, not hundreds of per-member queries.
  - **Covers R3, R6.**

- AE2. **Sidebar scores without query storm**
  - **Given:** Same lab and submission as AE1.
  - **When:** Dashboard loads challenge list with scores.
  - **Then:** List returns within 1 second alongside stats; submission field/method/constructor results are not re-queried separately for each challenge.
  - **Covers R2, R5.**

- AE3. **Upload returns scores only**
  - **Given:** Student uploads 4 challenge folders.
  - **When:** Upload completes.
  - **Then:** Response includes `challengeResult` with score per UUID and no `class` arrays; upload completes within 5 seconds on local dev; Class tab loads via `/class` when selected.
  - **Covers R4, R9.**

- AE4. **Login feels instant**
  - **Given:** Backend already running locally.
  - **When:** Student completes IRN login.
  - **Then:** Dashboard shell visible within 2 seconds of auth response; no extra seconds waiting on lazy role fetches.
  - **Covers R1, R13.**

- AE5. **No grading regression**
  - **Given:** Fixed sample submission folder used before optimization.
  - **When:** Re-uploaded after perf changes.
  - **Then:** Per-challenge and overall scores match pre-change values.
  - **Covers R15.**

### Key Decisions

- KD1. **Full-stack scope** over upload-only or read-only — login, dashboard, challenge display, and upload are all in scope. Governs R1–R4. (session-settled: user-directed — chosen over read-path-only or upload-only: pain spans the whole student journey.)
- KD2. **Aggressive local targets** — Governs R1–R4. (session-settled: user-directed — chosen over moderate or “fix minutes only”: local slowness proves algorithmic debt.)
- KD3. **Scores-only upload response** over bundled class data — Governs R9. Supersedes bundle requirement in `docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md`. (session-settled: user-directed — chosen over keep bundle: post-grade class DTO build is a major upload bottleneck.)
- KD4. **Batch-and-cache read path** over new combined mega-API as the first move — Governs R5–R8. Optional combined dashboard endpoint deferred unless batching alone misses R2.
- KD5. **Keep synchronous upload with final score** — Governs R10. Inherited from July 2026 grading plan; not revisited.

### Success Criteria

| Path | Target (local dev, medium lab) |
|---|---|
| Login → dashboard shell | p50 < 2s |
| Stats after lab select | p50 < 1s |
| Class tab on challenge select | p50 < 2s |
| Upload → score | p50 < 5s |

Verification uses repeatable manual timing or logged milestones (R17). Production deploy optimizations are out of scope unless local targets are met and surplus work is trivial.

### Scope Boundaries

**In scope**

- `ChallengeService`, `ClassStructureService`, `StatsService`, `SubmissionResolutionService`
- `SubmissionController` upload response shape (scores only)
- `GradingResultStore` read path for re-upload
- `MasterDataResolver` caching
- `AuthController` / auth query patterns
- `StudentDashboard.jsx`, `DropZone.jsx` fetch parallelism and effect dependencies
- Request-level timing for new hot paths

**Deferred for later**

- Combined single-call dashboard bootstrap API (revisit if R2 not met after batching)
- Production cold-start mitigation (Render keep-alive, Neon pool tuning beyond defaults)
- Redis or multi-instance shared caches
- MMD grading or MMD tab performance
- Async grading / poll UX
- Bytecode parser replacing reflection

**Deferred to Follow-Up Work**

- Frontend bundle-size audit for login shell (if R1 still missed after backend auth fixes)
- Database indexes beyond what batching requires (add only when profiling shows need)

**Outside this product's identity**

- Changing what is graded or scoring rules for speed
- Removing synchronous score from upload response

### Dependencies and Assumptions

- Assumption: Slowness reproduced on **local dev** (`npm start`, localhost backend) — not primarily production cold start.
- Assumption: July 2026 grading optimizations (parallel compile/grade, `LabRubricCache`, MMD off hot path) are already in the codebase.
- Assumption: Medium lab means ~4–8 challenges with multiple `.java` files each; “large” challenges with dozens of classes are stretch goals for R3 (document if exceeded).
- Dependency: PostgreSQL remains source of truth; in-process caches are performance layers only.

### Outstanding Questions

**Resolved in planning**

- OQ1 → KTD1: Reuse existing `findByChallengeInWithAttributes` / `findByClassEntityInWithDeclaration` batch repo methods from `LabRubricService` rather than inventing new query shapes.
- OQ2 → KTD3: Change `challengeResult` type to `Map<UUID, Integer>`; remove `ChallengeResultBundle` or reduce to score-only record; frontend drops class cache from upload.
- OQ3 → KTD6: Extend `app.grading.timing-log` to read-path controllers/services with `class_ms`, `challenges_ms` milestones (dev profile default true).

### How This Work Fits Together

- **`docs/plans/2026-07-31-001-perf-grading-speed-plan.md`** — Grading upload pipeline; largely implemented. This plan owns what that plan deferred (frontend score display, read paths).
- **`docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md`** — Challenge result wiring; **upload bundle shape superseded** by KD3/R9 for speed. Frontend cache-on-upload behavior should simplify to score merge + on-demand `/class`.

### Sources / Research

- Session brainstorm dialogue (2026-08-04)
- `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java` — per-class N+1 pattern
- `backend/src/main/java/com/eiu/capstone/backend/service/ChallengeService.java` — per-challenge repeated submission-result queries
- `backend/src/main/java/com/eiu/capstone/backend/service/MasterDataResolver.java` — uncached `findAll()`
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricService.java` — batch query pattern to reuse
- `backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java` — post-grade class bundle loop
- `frontend/src/pages/StudentDashboard.jsx` — sequential fetch waterfall

## Planning Contract

### Technical approach

Four backend layers plus frontend parallelism:

1. **Shared submission-result loader** — one JOIN FETCH load per submission for field/method/constructor/challenge results; consumed by `GradingResultStore`, `ChallengeService`, and `ClassStructureService`.
2. **Cached master data** — `MasterDataCache` mirroring `LabRubricCache` TTL semantics; `ClassStructureService` never calls `findAll()` per request.
3. **Batched rubric reads** — `ClassStructureService` and `ChallengeService` adopt the same 6–7 query batch pattern as `LabRubricService.loadForLab`, scoped to one challenge or all lab challenges respectively.
4. **Slim upload tail** — map `gradingOutcome.gradedChallenges()` to `Map<UUID, Integer>`; delete post-grade `buildClassDataForSubmission` loop.
5. **Frontend parallelization** — `Promise.all` for challenges + stats on lab select; narrow `useEffect` deps; always fetch `/class` on challenge select.

### Key technical decisions

- KTD1. **Reuse LabRubric batch repositories** — Governs R5, R6. `ClassEntityRepository.findByChallengeInWithAttributes`, `FieldRepository.findByClassEntityInWithDeclaration`, etc. already exist. Chosen over new SQL because grading loader already proved the pattern.
- KTD2. **SubmissionResultLoader service** — Governs R8. Centralize four JOIN FETCH repository methods into one loader returning `SubmissionCorrectIds` (sets of UUIDs per entity type). Chosen to dedupe identical loads in `ChallengeService` (N×) and `GradingResultStore`.
- KTD3. **`Map<UUID, Integer> challengeResult`** — Governs R9. Replace `ChallengeResultBundle` in `SubmissionUploadResponse`. Frontend merges scores into challenge list state; class arrays always from `GET /challenges/{id}/class`.
- KTD4. **`@EntityGraph(roles)` on auth lookups** — Governs R13. Add to `findByEmail` and `findByStudentCodeOrTeacherCode`. Chosen over DTO projection because auth response already uses entity fields.
- KTD5. **MasterDataCache with TTL** — Governs R7. Mirror `LabRubricCache`: `ConcurrentHashMap`, single global key, `app.master-data-cache-ttl-minutes` default 60. Chosen because master data is small and rarely changes.
- KTD6. **Read-path timing behind existing flag** — Governs R17. When `app.grading.timing-log=true`, log `challenges_ms`, `class_ms`, `stats_ms` at INFO. Reuse property name to avoid config sprawl.

### Sequencing

```
U1 SubmissionResultLoader + JOIN FETCH repos
  → U2 MasterDataCache
  → U3 ClassStructureService batch refactor
  → U4 ChallengeService batch refactor
U5 Upload scores-only (can land with U3/U4 in same PR)
U6 Auth eager roles (independent)
U7 StudentDashboard parallelism (after U5 response shape)
U8 Read-path timing + AGENTS.md
```

U3 depends on U1 and U2. U4 depends on U1. U7 depends on U5 for frontend response parsing.

### Risks and mitigations

| Risk | Mitigation |
|---|---|
| Score computation diverges after batch refactor | AE5 fixed sample; compare per-challenge scores before/after |
| Frontend breaks expecting `challengeResult[].class` | U5 + U7 ship together; grep for `ChallengeResultBundle` |
| Large challenge still slow after batching | Document stretch; defer pagination per Outstanding Questions |
| Master data stale after DB edit | TTL 60m; `invalidate()` hook for future admin API |

## Implementation Units

### U1. Submission result batch loader

**Covers R8.** Add JOIN FETCH repository methods:

- `SubmissionFieldResultRepository.findBySubmission_IdWithField`
- `SubmissionMethodResultRepository.findBySubmission_IdWithMethod`
- `SubmissionConstructorResultRepository.findBySubmission_IdWithConstructor`
- `SubmissionChallengeResultRepository.findBySubmission_IdWithChallenge`

Add `SubmissionResultLoader` (or extend `SubmissionResolutionService`) with `SubmissionCorrectIds loadCorrectIds(UUID submissionId)` returning four `Set<UUID>`.

Wire into:

- `grading/GradingResultStore.loadExisting` — replace lazy `getField().getId()` loops
- `ChallengeService` — load once before challenge loop (U4)
- `ClassStructureService` — use loader instead of three separate `findBySubmission_Id` calls (U3)

**Files:** `repository/Submission*ResultRepository.java`, `service/SubmissionResultLoader.java`, `grading/GradingResultStore.java`

### U2. Master data cache

**Covers R7.** Add `MasterDataCache` wrapping `MasterDataResolver`:

- Single cache entry for full scope/type map
- TTL: `app.master-data-cache-ttl-minutes` (default 60)
- `invalidate()` for future admin mutations
- Pattern: `grading/rubric/LabRubricCache.java`

Update `ClassStructureService` to inject cache instead of `masterDataResolver.loadAll()` per call.

**Files:** `service/MasterDataCache.java`, `service/MasterDataResolver.java`, `service/ClassStructureService.java`, `application.properties`

### U3. ClassStructureService batch build

**Covers R6, R3.** Refactor `buildClassDataForSubmission`:

1. Load challenge classes via `findByChallengeInWithAttributes(List.of(challenge))`
2. Batch fields, methods, constructors via `findByClassEntityInWithDeclaration`
3. Batch parameters via `ParameterRepository.findByConstructorEntityIn` / `findByMethodIn`
4. Group in memory by `classEntityId`
5. Use `SubmissionResultLoader` correct-ID sets (U1) and cached master data (U2)

Remove per-class `findByClassEntity_Id` loops and per-constructor parameter queries.

**Files:** `service/ClassStructureService.java` (primary), reuse existing repositories

### U4. ChallengeService single-pass scores

**Covers R5, R2.** Refactor `getChallengesForLab`:

1. Load all challenges for lab (1 query)
2. Resolve latest submission id once (1 query)
3. `SubmissionResultLoader.loadCorrectIds(submissionId)` once (4 JOIN FETCH queries)
4. Batch-load all classes for all challenges: `findByChallenge_IdIn` or `findByChallengeInWithAttributes`
5. Batch-load fields/methods/constructors for all class IDs
6. Group by challenge in memory; compute each score without additional DB calls

Optional fast path: read persisted `SubmissionChallengeResult` scores if already stored during grading — only if it simplifies without changing score semantics.

**Files:** `service/ChallengeService.java`, possibly `repository/ClassEntityRepository.java` (`findByChallenge_IdIn` if missing)

### U5. Upload scores-only response

**Covers R9, R4.** In `SubmissionController.upload`:

- Delete loop calling `classStructureService.buildClassDataForSubmission` per graded challenge
- Build `Map<UUID, Integer>` from `gradingOutcome.gradedChallenges()`
- Change `SubmissionUploadResponse.challengeResult` type to `Map<UUID, Integer>`
- Remove or deprecate `ChallengeResultBundle.java`

**Frontend (same unit or U7):**

- `StudentDashboard.handleUploadComplete` — merge integer scores into challenge state; remove class cache from upload payload
- Always fetch `/class` when student selects challenge after upload

**Files:** `controller/SubmissionController.java`, `DTO/SubmissionUploadResponse.java`, `DTO/ChallengeResultBundle.java` (delete), `frontend/src/pages/StudentDashboard.jsx`

### U6. Auth eager role fetch

**Covers R13, R1.** Add `@EntityGraph(attributePaths = "roles")` to:

- `UserAccountRepository.findByEmail`
- `UserAccountRepository.findByStudentCodeOrTeacherCode`

Ensure `AuthController` login/upsert paths use these methods. No change to JWT shape.

**Files:** `repository/UserAccountRepository.java`, verify `controller/AuthController.java`, `service/UserService.java`

### U7. StudentDashboard parallel fetches

**Covers R11, R12, F1.** Refactor `frontend/src/pages/StudentDashboard.jsx`:

- On `selectedLabId` change: `Promise.all([fetchChallenges, fetchStats])` in parallel
- On challenge select: fetch `/class` and stats in parallel where independent
- Remove `challenges` from detail `useEffect` dependency array; depend on `selectedChallengeId`, `selectedLabId`, `studentId` only
- Post-upload: parallel refresh of challenges + stats + `/class` for selected challenge
- Drop `challengeResultCache` class arrays; keep score merge only

**Files:** `frontend/src/pages/StudentDashboard.jsx`, `frontend/src/components/student/StudentUI.jsx` (if callback shape changes)

### U8. Read-path timing and docs

**Covers R17, R9 ops.** When `app.grading.timing-log=true`:

- Log `challenges_ms` in `ChallengeController` or `ChallengeService`
- Log `class_ms` in `ClassStructureService` or `ChallengeController`
- Log `stats_ms` in `StatsService`

Update `backend/AGENTS.md` (submission resolution + read-path batching), `frontend/AGENTS.md` (parallel fetch contract), and note supersession of bundle upload shape in `frontend/src/pages/AGENTS.md`.

Document new property `app.master-data-cache-ttl-minutes` alongside existing grading properties table.

**Files:** `service/*`, `backend/AGENTS.md`, `frontend/AGENTS.md`, `application.properties`

## Verification Contract

No automated test suite exists. Verify manually on local dev with `npm start`, `app.grading.timing-log=true`, and SQL logging optional (`spring.jpa.show-sql=true` for query-count spot checks).

### Benchmark — dashboard reads (R2, R3, AE1, AE2)

1. Log in as student with existing submission on a 5-challenge lab.
2. Select lab; time until stats cards render — target **< 1s**.
3. Time `GET /api/labs/{labId}/challenges?studentId=` — target **< 1s**; confirm ≤ ~10 SQL statements (not 40+).
4. Select a challenge; time Class tab — target **< 2s**; confirm batched queries (no per-constructor loops in SQL log).

### Benchmark — upload (R4, AE3)

1. Upload 4–6 challenge folder via `DropZone` with warm rubric cache.
2. Response `challengeResult` values are integers only (no `class` key).
3. `total_ms` ≤ 5000 in timing log (post U5 removal of class build tail).

### Benchmark — login (R1, AE4)

1. IRN login with backend warm.
2. Time from auth response to dashboard shell paint — target **< 2s**.
3. Confirm single user+roles query (no extra `role` SELECTs in SQL log).

### Correctness (R15, AE5)

1. Record per-challenge and overall scores for a fixed sample submission before changes.
2. After all units: re-upload; scores must match exactly.
3. Class tab correctness: field/method/constructor pass-fail indicators match pre-change for same submission.

### Build gates

- `mvn -q -DskipTests compile` from `backend/`
- `npm run build` from `frontend/`

## Definition of Done

**Global**

- All U1–U8 complete
- Product Contract R1–R17 satisfied or explicitly deferred with documented reason
- `artifact_readiness: implementation-ready` on this plan
- AGENTS.md chain updated for touched packages
- Aug 4 upload plan bundle shape documented as superseded in backend/frontend AGENTS.md

**Per unit**

| Unit | Done when |
|---|---|
| U1 | `GradingResultStore.loadExisting` uses JOIN FETCH; no lazy N+1 on re-upload |
| U2 | Second `/class` request in same JVM skips `master_data` full table scan |
| U3 | `/class` for medium challenge completes in < 2s; ≤ ~7 query groups in SQL log |
| U4 | `/challenges` loads all scores with one submission-result load, not N× |
| U5 | Upload response has `Map<UUID, Integer>` only; no `buildClassDataForSubmission` on upload path |
| U6 | Auth issues ≤ 2 queries including roles |
| U7 | Lab select fires parallel challenges+stats; detail effect does not re-run on score-only list refresh |
| U8 | Timing logs for read paths; AGENTS.md documents new cache property and scores-only upload |
