---
title: Grading Result Persist Speed - Plan
date: 2026-08-27
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Grading Result Persist Speed - Plan

## Goal Capsule

**Objective:** Cut upload `grade` wall time by writing challenge scores on the hot path with PostgreSQL `INSERT … ON CONFLICT`, and flushing per-member / per-assertion rows after the HTTP response, without changing scores or Class-tab content once the flush finishes.

**Product authority:** Session 2026-08-27. User directed both (1) bulk UPSERT and (2) deferred detail persist (session-settled: user-directed — chosen over Hibernate `saveAll` on the request thread and over on-demand persist only when a tab opens: keep full durable rows, return the upload response without waiting on them).

**Open blockers:** None.

## Product Contract

### Summary

Students still get a synchronous upload score and in-memory `lab_result`. Postgres still stores the same unique-keyed result rows. Detail rows may land milliseconds to seconds after the response; Class / MMD / Testcase GETs wait for that flush.

### Requirements

- R1. Persist `submission_challenge_result` (and snapshot files) before the upload response.
- R2. Persist field/method/constructor/relation/testcase/assertion rows via `INSERT … ON CONFLICT` on existing unique keys (`submission_*_result_key`), not Hibernate `saveAll`.
- R3. Do not load existing result entities before compute; UPSERT replaces the merge-by-id path.
- R4. Schedule detail UPSERT after scores are committed; do not block the upload HTTP thread on it.
- R5. `GET …/class`, `GET …/mmd`, `GET …/testcases` wait until that submission’s detail flush completes (timeout, then proceed).
- R6. Challenge sidebar scores use stored challenge-result scores when present (so post-upload refresh does not wait on details).
- R7. Re-upload of the same attempt still updates in place (no duplicate-key 500, no delete-all).
- R8. Grading scores and pillar semantics unchanged.

### Acceptance Examples

- AE1. Large lab upload: `[timing] Grade submission` `save` is challenge scores + snapshot only; detail UPSERT runs off-thread.
- AE2. Immediate Class tab after upload shows correct member marks (GET waited or details already flushed).
- AE3. Second drop of the same attempt updates `is_correct` / testcase status; no unique-constraint error.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | JDBC `unnest` + `ON CONFLICT ON CONSTRAINT` via `JdbcTemplate` | Same keys as entities; UUID ids via `gen_random_uuid()` on insert; Neon round-trips per table not per row. (session-settled: user-directed — chosen over Hibernate batch `saveAll`) |
| KTD2 | Dedicated `persistExecutor` (2 threads), not `gradingExecutor` | Avoid pooling nested work on the challenge pool. |
| KTD3 | In-process `CompletableFuture` gate keyed by submission id | Class/MMD/testcase reads join the flush; no extra table. (session-settled: user-directed — chosen over persist-only-on-tab-open) |
| KTD4 | Sidebar reads `submission_challenge_result` first | Unblocks dashboard refresh; stored score is the source of truth. |

### Implementation Units

- **U1.** `GradingResultJdbcWriter` — UPSERT helpers for challenge rows and detail tables (testcase RETURNING id for assertions).
- **U2.** `SubmissionDetailPersistGate` + `persistExecutor` — register future, run details, await on Class-tab reads.
- **U3.** `GradingResultStore` / `GradingService` — hot path scores+snapshot; skip `loadExisting`; schedule details.
- **U4.** `ChallengeService.getChallengesForLab` — stored challenge scores when rows exist.
- **U5.** Tests + `grading/AGENTS.md` persist contract.

## Verification Contract

- Update `GradingResultStoreReuploadRegressionTest` for the JDBC writer (twice, no delete).
- Unit-test empty payloads skip SQL.
- `mvn test` from `backend/`.
