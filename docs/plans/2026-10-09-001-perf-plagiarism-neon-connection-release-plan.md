---
title: Plagiarism Neon connection release - Plan
date: 2026-10-09
type: perf
topic: plagiarism-neon-connection-release
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Plagiarism Neon connection release - Plan

## Goal Capsule

**Objective:** Release the Neon connection during plagiarism inspect’s CPU pairwise compare so inspect cannot pin a pool connection while comparing fingerprints.

**Product authority:** Session 2026-10-09 brainstorm. Other Neon-checklist items and peer-fingerprint payload shrink are not active scope for this plan.

**Open blockers:** None.

## Product Contract

### Summary

Split off-thread plagiarism inspect into two short Neon phases around an in-memory compare: prepare and load, release the connection, compare, then write matches and reevaluate flags. Detection rules, score-gate, latest-attempt flagging, and “flags within a few seconds” stay as today.

### Key Decisions

- **Connection lifetime is the finish line** — success is Neon free during CPU compare, not smaller peer payloads. (session-settled: user-directed — chosen over shrink-peer-payload / do-both: proactive Neon pool optimization with a clear boundary) Governs R1, R7.
- **Detection rules frozen** — all other students’ lab fingerprints, score-gate, latest-attempt, few-seconds flag latency. (session-settled: user-directed — chosen over slower flags or changing who/what is compared) Governs R2, R5.
- **Two-phase inspect** — short prepare/load, CPU compare with no open connection, short write. (session-settled: user-directed — chosen over snapshot-DTO or chunked windows: smallest change that meets the finish line) Governs R3, R4.
- **Partial commit on compare failure is accepted** — prepare/load commits even if compare later fails; today one transaction rolls those writes back. (session-settled: user-approved — confirmed in scoping synthesis after call-out) Governs R6.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns only releasing the Neon connection during plagiarism inspect’s CPU compare.

The broader Neon / DB-slowness checklist from the 2026-10-09 analysis is the current understanding, not a committed roadmap:

- Peer fingerprint payload shrink (stop reloading full text blobs for compare)
  - Can proceed independently of this plan; deferred here by choice
- Rubric cache / grading persist / scheduler / analytics Neon pressure
  - Can proceed independently of this plan; deferred as separate problem areas
- Off-thread inspect already shipped (`docs/plans/2026-09-14-001-perf-off-thread-plagiarism-inspect-plan.md`)
  - Shares the same inspect entrypoint and detection contracts; this plan extends connection lifetime inside that path

### Problem Frame

Inspect already runs on `persistExecutor` after upload persist. The remaining Neon cost is holding a pooled connection for the whole inspect method while Java walks every peer fingerprint. Nothing broken was observed in production; this is a bounded pool-pressure optimization.

### Requirements

**Connection and phases**

- R1. While pairwise fingerprint comparison runs, inspect must not hold an open Neon / Hikari connection from the application pool.
- R3. Inspect uses two Neon-touching phases separated by in-memory compare: (1) save this attempt’s fingerprint, delete matches involving this submission, load peer fingerprints and any attempt data needed for score-gate / latest-attempt; (2) after compare, persist content matches and reevaluate flags for this uploader.

**Detection and latency**

- R2. Detection semantics stay unchanged: compare every other lab fingerprint (all prior attempts of other students), persist only content matches, apply score-gate and latest-attempt flagging as today.
- R5. Lecturer-visible flags still appear within a few seconds of upload under normal load (same class of lag as today’s off-thread inspect).

**Failure and existing contracts**

- R4. Upload HTTP behavior stays unchanged: inspect remains off-request on `persistExecutor`; extract/inspect failures stay swallowed; upload remains 200.
- R6. If compare or the write phase fails after the prepare/load phase committed, the fingerprint (and cleared prior matches for this submission) may remain without new match rows until a later successful inspect; this is accepted.

**Non-goals as requirements boundaries**

- R7. This work does not shrink peer fingerprint payloads, change compare algorithms or thresholds, add schema, or change lecturer plagiarism APIs.

### Key Flows

- F1. Successful inspect with peers
  - **Trigger:** Off-thread inspect runs after a persisted upload with peer fingerprints in the lab.
  - **Steps:** Prepare/load on Neon → release connection → pairwise compare in memory → write matches + flag reevaluation on Neon.
  - **Outcome:** Content matches and flags match today’s rules; Neon was not held during compare.
  - **Covered by:** R1, R2, R3, R5

- F2. Compare or write fails after prepare/load
  - **Trigger:** Runtime failure after prepare/load committed.
  - **Steps:** Failure is logged and swallowed for upload impact; prepare/load effects may already be visible.
  - **Outcome:** Student upload already 200; flags/matches for this attempt may be incomplete until a later inspect.
  - **Covered by:** R4, R6

### Acceptance Examples

- AE1. Covers R1, R3. Given inspect with many peer fingerprints, When pairwise compare runs, Then no application-pool Neon connection remains checked out for the duration of that compare.
- AE2. Covers R2, R5. Given a first-time content copy that should flag today, When inspect completes, Then ORIGINAL/PLAGIARIZER (or equivalent) still appear for lecturers within a few seconds under normal load.
- AE3. Covers R4, R6. Given prepare/load succeeded and compare throws, When inspect aborts, Then upload was already 200, and fingerprint/cleared-match state from prepare/load may persist without new matches until a later successful inspect.

### Success Criteria

- Neon pool connection is not held during inspect CPU compare (R1).
- Flag and match outcomes for ordinary successful uploads match pre-change detection rules (R2).
- No change to upload HTTP success path (R4).

### Scope Boundaries

**In scope:** Two-phase connection release around plagiarism inspect’s CPU compare; documenting the accepted partial-commit failure mode.

**Deferred for later:** Shrinking peer fingerprint load payloads; other Neon-checklist areas (rubric cache, persist writer, deadline email scheduler, analytics queries); chunked peer windows; ORM snapshot-DTO hard rule beyond what planning needs for R1.

**Out of scope:** Changing Jaccard / content-match thresholds; changing score-gate or latest-attempt product rules; schema migrations; lecturer API redesign; raising `persistExecutor` size as the primary fix.

### Dependencies / Assumptions

- Off-thread inspect on `persistExecutor` after persist remains the scheduling path (`docs/plans/2026-09-14-001-perf-off-thread-plagiarism-inspect-plan.md`).
- `spring.jpa.open-in-view=false` remains in effect so request threads do not keep sessions open for this work.
- Peer fingerprint volume still fits in memory for a full-lab load (chunking deferred).

### Sources / Research

- `docs/plans/2026-09-14-001-perf-off-thread-plagiarism-inspect-plan.md` — existing off-thread inspect contract.
- `docs/plans/2026-09-09-001-perf-neon-query-roundtrips-plan.md` — batched peer bests; multi-statement TX was out of that plan’s scope.
- `backend/src/main/java/plagiarism/AGENTS.md` — all-attempts compare, score-gate, latest-attempt, bean `inspectUpload` entrypoint.
- `docs/solutions/architecture-patterns/lab-clone-across-terms.md` — short Neon transactions / `TransactionTemplate` / `REQUIRES_NEW` precedent.
- `docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md` — shared `persistExecutor`.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Orchestrate `inspectUpload(submission, signals)` with **no outer `@Transactional`**; run prepare and write each inside `TransactionTemplate.execute` so the connection returns to the pool between phases | Matches `LabCloneService` short-TX Neon pattern (`docs/solutions/architecture-patterns/lab-clone-across-terms.md`). Satisfies R1/R3 without a Hikari spy. Governs R1, R3. |
| KTD2 | Use default `PROPAGATION_REQUIRED` on a dedicated `TransactionTemplate` (or two sequential `execute` calls on one template). Prepare must **complete and commit** before compare starts so R6 holds | Nested `REQUIRES_NEW` is unnecessary if the orchestrator itself is non-transactional. Avoid self-invocation of `@Transactional` helpers on `this`. |
| KTD3 | Keep `SubmissionController.schedulePlagiarismInspect` calling the Spring bean’s signals overload unchanged | Preserves R4 and the 2026-09-14 off-thread contract. |
| KTD4 | Prepare phase materializes peer `PlagiarismSignals` (or equivalent scalars) plus attempt maps needed for score-gate; write phase must not rely on lazy JPA proxies from the closed prepare session | `open-in-view=false`; re-eval peer-side matches load inside the write TX (`findByLabIdAndOtherSubmissionIdIn`). |
| KTD5 | Prove AE1 structurally: compare runs strictly between the two `execute` calls (code review + DOX). Automate detection (R2) and R6 characterization in unit tests; no Hikari/`getActiveConnections` integration test in this plan | Unit tests construct `new PlagiarismService(...)` today and never hit a real pool. |
| KTD6 | Document R6 partial-commit semantics in `plagiarism/AGENTS.md` (and pipeline docs as needed) | Product-accepted change from single-TX rollback. |

### Assumptions

- Injecting `PlatformTransactionManager` / building `TransactionTemplate` in `PlagiarismService` is acceptable (same pattern as `LabCloneService` / `TermService`).
- Attempt maps built in prepare can be reused for write-phase score-gate / latest-attempt if they only need detached scalars already loaded; otherwise reload attempts inside the write TX.
- Existing seven `PlagiarismServiceInspectTest` scenarios remain the detection regression suite; constructor gains a `TransactionTemplate` (real or stub that runs the callback inline).

### High-Level Technical Design

Directional only — not implementation specification.

```text
persistExecutor
  └─ inspectUpload(submission, signals)   [NO outer @Transactional]
        ├─ tx.execute(prepare)            // save fingerprint, delete matches,
        │                                 // load peers + attempts → InspectPrep
        ├─ compareInMemory(prep)          // no Session / no DataSource
        └─ tx.execute(write)              // saveAll matches + reevaluateMatchesAgainstUploader
```

Failure after prepare commit → fingerprint + cleared matches may linger (R6); upload already 200 (R4).

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Self-invocation skips TX on prepare/write helpers | Keep both phases inside `TransactionTemplate.execute` lambdas, or call through another Spring bean — never `this.prepare()` with only `@Transactional` |
| Wider window between delete-matches and new saveAll | Accepted under R6; same swallow-on-failure upload contract |
| Reusing managed entities across phases | Convert peers to signals/scalars in prepare; load peer-side matches in write TX |

### Open Questions

**Deferred (non-blocking):** None blocking. Implementer may choose inline `TransactionTemplate` stub vs Mockito for tests as long as prepare commits before compare runs in production code.

## Implementation Units

### U1. Two-phase `inspectUpload` orchestration

- **Goal:** Release the pool connection during CPU compare while preserving detection outcomes.
- **Requirements:** R1, R2, R3, R4, R6
- **Approach:** Remove whole-method `@Transactional` from `inspectUpload(LabSubmission, PlagiarismSignals)` (and update the method Javadoc that still describes a single transactional method). Inject `PlatformTransactionManager` and a `TransactionTemplate`. Prepare `tx.execute`: current save + `deleteInvolvingSubmission` + `findByLabIdAndUserIdNot` + `attemptsByUser` (and derived prior/best maps). Early-return when peers empty still runs only prepare. In-memory compare (no pool): existing compare loop building match list. Write `tx.execute`: `saveAll` + `reevaluateMatchesAgainstUploader`. Do not change `PlagiarismComparator` thresholds or score-gate helpers. Honors Key Decisions governing R1–R3, R6 and KTD1–KTD4.
- **Files:** `backend/src/main/java/plagiarism/PlagiarismService.java`; optionally a small package-private prep/result type in the same package if it clarifies detach safety.
- **Patterns:** `backend/src/main/java/service/LabCloneService.java` (`TransactionTemplate` + short Neon TX); existing inspect loop at `PlagiarismService` ~71–137.
- **Test scenarios:**
  - Happy: peers present, content match + score-gate → one flagged match persisted; attempts still one `findByLabIdAndUserIdIn` (or documented reload in write).
  - Happy: empty peers → fingerprint saved, delete called, no attempts query, no match save.
  - Edge: already-proven ability → content match row with `flagged=false`.
  - Edge: zero-score attempt → never flags.
  - Edge: peer-side promotion when uploader best rises → re-eval still runs in write phase.
  - Error / R6: write-phase `saveAll` throws after prepare would have committed → fingerprint/delete already durable; assert prepare-side repo calls happened.
  - Error / R6 (AE3): compare throws after prepare committed → no write `execute` / no new matches; prepare-side effects already verified.
- **Execution note:** Prefer keeping detection-locked tests green first, then add R6 orchestration assertion.
- **Verification:** `mvn -f backend/pom.xml "-Dtest=support.com.eiu.capstone.backend.plagiarism.PlagiarismServiceInspectTest" test`
- **Dependencies:** None

### U2. Inspect unit tests for two-phase + R6

- **Goal:** Keep detection coverage and characterize prepare-before-compare / R6 orchestration.
- **Requirements:** R2, R6; supports AE1 structurally (KTD5)
- **Approach:** Update `PlagiarismServiceInspectTest` construction for `TransactionTemplate` (stub that runs `callback.doInTransaction` immediately for each `execute`). Keep existing seven detection scenarios green (owned here; U1 lists the same outcomes for traceability). Assert two `execute` calls when peers are non-empty. Add R6 tests: write-phase throw after prepare; compare throw after prepare (AE3) with no new matches.
- **Files:** `backend/src/test/java/support/com/eiu/capstone/backend/plagiarism/PlagiarismServiceInspectTest.java`
- **Test scenarios:**
  - Happy: existing first-copy / signals-overload / empty-peers / one-query peer bests cases still pass.
  - Edge: two `execute` calls when peers non-empty.
  - Error: second phase throws after first phase ran.
- **Verification:** same Maven test command as U1
- **Dependencies:** U1

### U3. DOX: two-phase inspect + partial commit

- **Goal:** Document connection release and R6 so future edits do not re-wrap the whole inspect in one `@Transactional`.
- **Requirements:** R1, R3, R6; supports AE1/AE3 documentation
- **Approach:** Update `plagiarism/AGENTS.md` Work Guidance / Local Contracts: prepare TX → in-memory compare → write TX; bean entrypoint still required for any transactional helpers; R6 partial commit. Touch `backend/AGENTS.md` inspect bullet and `docs/GRADING_WORKFLOWS.md` / `docs/HOW_IT_RUNS.md` only where they claim a single transactional inspect.
- **Files:** `backend/src/main/java/plagiarism/AGENTS.md`; `backend/AGENTS.md`; `docs/GRADING_WORKFLOWS.md`; `docs/HOW_IT_RUNS.md` (only if those sentences are now wrong).
- **Test expectation:** none — docs only
- **Dependencies:** U1

## Verification Contract

- `mvn -f backend/pom.xml "-Dtest=support.com.eiu.capstone.backend.plagiarism.PlagiarismServiceInspectTest" test`
- Prefer also `mvn -f backend/pom.xml "-Dtest=support.com.eiu.capstone.backend.plagiarism.*" test`
- AE1 checklist: code review confirms compare sits between two completed `TransactionTemplate.execute` calls with no repository/Session use in between; DOX states the same.
- AE2: existing flagging tests + unchanged lecturer APIs (no new E2E required for this plan).
- AE3 / R6: unit characterization + DOX note.

## Definition of Done

- `inspectUpload` does not hold a Neon pool connection during pairwise compare (R1/R3; KTD1).
- Detection / score-gate / latest-attempt / peer-side re-eval outcomes unchanged for successful inspects (R2; existing tests green).
- Upload scheduling and swallow-on-failure unchanged (R4).
- R6 partial-commit behavior documented in `plagiarism/AGENTS.md`.
- No peer payload shrink, schema, lecturer API, or `persistExecutor` size change (R7).
