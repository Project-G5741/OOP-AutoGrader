---
title: Detail UPSERT single transaction - Plan
date: 2026-10-09
type: perf
topic: detail-upsert-single-transaction
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Detail UPSERT single transaction - Plan

## Goal Capsule

**Objective:** Run all off-thread detail UPSERTs (member tables, testcase results, assertions) in one DataSource connection borrow and one explicit transaction on both Postgres and H2, so Neon readers and detail-gate waits pay one checkout cycle and never see a half-written detail set for that attempt.

**Product authority:** Session 2026-10-09 brainstorm. Hikari keepalive, upload `persistUpload` redesign, and executor changes are not active scope for this plan.

**Open blockers:** None.

## Product Contract

### Summary

Wrap `upsertDetails` so every non-empty member/testcase/assertion write shares one borrowed connection and commits once (rollback on any failure). Apply the same shape to Postgres and H2. Upload scores and HTTP 200 remain independent of detail success.

### Key Decisions

- **One borrow for all detail UPSERTs is the finish line** — not merely fewer separate checkouts. (session-settled: user-directed — chosen over fewer-but-separate commits) Governs R1, R3.
- **All-or-nothing detail rows** — mid-UPSERT failure rolls back that attempt’s detail writes; tabs must not see a partial set. (session-settled: user-directed — chosen over best-effort partial) Governs R2, R4.
- **Postgres and H2 both** — desktop writer matches the one-borrow / one-TX shape. (session-settled: user-directed — chosen over Postgres-only) Governs R5.
- **Explicit one-borrow TX** — not a mega-SQL CTE and not outer `@Transactional` as the primary mechanism. (session-settled: user-directed — chosen over CTE rewrite or declarative TX) Governs R1, R6.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns only collapsing detail UPSERT connection/transaction boundaries.

The broader Neon / DB-slowness checklist from the 2026-10-09 analysis is the current understanding, not a committed roadmap:

- Hikari / Neon suspend keepalive and socket timeouts
  - Can proceed independently; deferred
- Plagiarism Neon connection release (`docs/plans/2026-10-09-001-perf-plagiarism-neon-connection-release-plan.md`)
  - Independent; already planned/shipped
- Rubric overlap isolation (`docs/plans/2026-10-09-002-perf-persist-executor-rubric-isolation-plan.md`)
  - Independent; already planned/shipped
- Detail-gate still sharing `persistExecutor` with inspect
  - Deferred earlier; this plan shortens detail wall-clock but does not re-prioritize the queue

### Problem Frame

Today `upsertDetails` calls `withConnection` once per member table (and once for testcases, with assertions on that same borrow). Each release returns the Hikari connection; the next table pays another Neon round trip. Student scores do not wait on this path. Class/MMD/testcase GETs await `SubmissionDetailPersistGate` and therefore feel the multi-checkout cost. Partial commits also allow a mid-failure window where some detail tables exist and others do not.

### Requirements

**Connection and transaction**

- R1. A single detail-persist run borrows the DataSource connection once for all member, testcase, and assertion UPSERTs for that submission attempt (skipping empty tables still counts as one borrow when any work runs).
- R2. Those writes commit as one transaction: on any failure, none of that run’s detail writes remain committed.
- R3. Empty detail payloads (no members and no testcases) may no-op without a borrow.

**Failure and upload independence**

- R4. Failure of detail UPSERT must not roll back already-committed upload scores / challenge results / progress from `persistUpload`.
- R6. Upload HTTP success and scheduling of detail work on `persistExecutor` stay as today (off-request; failures must not fail the student score response). Detail-gate await must still unblock after the detail task finishes or fails (no hung 60s wait beyond today’s failure mapping).

**Coverage**

- R5. Both `PostgresGradingResultJdbcWriter` and `H2GradingResultJdbcWriter` implement R1–R2 for `upsertDetails`.

**Boundaries**

- R7. This work does not redesign the upload `persistUpload` CTE, change Hikari pool/keepalive settings, or move detail work off `persistExecutor`.

### Key Flows

- F1. Successful detail persist
  - **Trigger:** Off-thread `upsertDetails` after upload with member and/or testcase rows.
  - **Steps:** One borrow → disable autocommit → all UPSERTs → commit → release; gate completes.
  - **Outcome:** Tabs awaiting the gate see a complete detail set; one checkout cycle.
  - **Covered by:** R1, R2, R6

- F2. Detail UPSERT fails mid-batch
  - **Trigger:** A later table write throws after earlier tables ran in the same TX.
  - **Steps:** Rollback → release; gate still completes (today: `completeExceptionally`; `await` catches and logs).
  - **Outcome:** No partial detail rows from that run; upload scores untouched.
  - **Covered by:** R2, R4, R6

### Acceptance Examples

- AE1. Covers R1, R3. Given a payload with all four member tables and testcases, When `upsertDetails` runs, Then only one DataSource connection acquire/release cycle occurs for that run (or equivalent proven instrumentation).
- AE2. Covers R2, R4. Given a forced failure on the last table write, When the run aborts, Then no new detail rows from that run remain and upload score rows from `persistUpload` remain.
- AE3. Covers R5, R6. Given desktop H2 profile, When `upsertDetails` runs successfully, Then the same one-borrow / one-TX semantics hold and Class/MMD/testcase awaits still unblock.

### Success Criteria

- Detail persist uses one borrow + one commit per successful run (R1, R2).
- Partial detail visibility for a failed run is eliminated (R2).
- Postgres and H2 both comply (R5).
- Upload score path unchanged (R4, R6).

### Scope Boundaries

**In scope:** Refactor `upsertDetails` (and helpers) on Postgres and H2 to share one connection/TX; preserve gate completion; DOX for the contract.

**Deferred for later:** Hikari keepalive / socket timeout; further SQL consolidation into one CTE; changing `SubmissionDetailPersistGate` timeout or cross-instance behavior; raising `persistExecutor` size.

**Out of scope:** Changing student score calculation; schema migrations; moving detail onto `gradingExecutor`.

### Dependencies / Assumptions

- Detail work remains scheduled via `GradingResultStore.scheduleDetailPersist` → `persistGate.runAsync` → `persistExecutor`.
- `DataSourceUtils.getConnection` / `releaseConnection` remain the borrow API.
- H2 `persistUpload` already demonstrates explicit `setAutoCommit(false)` / `commit` / `rollback`.
- Gate already uses `completeExceptionally` on task failure and `await` swallows wait exceptions (R6 preserved without gate changes).

### Sources / Research

- `backend/src/main/java/grading/PostgresGradingResultJdbcWriter.java` — per-table `withConnection` in `upsertDetails`.
- `backend/src/main/java/grading/H2GradingResultJdbcWriter.java` — same pattern; transactional `persistUpload`.
- `backend/src/main/java/grading/GradingResultJdbcWriterSupport.java` — `withConnection` acquire/release.
- `backend/src/main/java/grading/SubmissionDetailPersistGate.java` — `completeExceptionally` + await catch.
- `docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md` — deferred detail UPSERT on `persistExecutor`.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Add `GradingResultJdbcWriterSupport.withTransaction(DataSource, String, ConnectionWork)` that borrows once, saves/restores autocommit, `commit` on success, `rollback` on `SQLException`/`RuntimeException`, then releases | Shared by Postgres and H2; mirrors H2 `persistUploadOnce` TX block. Governs R1, R2, R5. |
| KTD2 | Refactor member helpers to accept `Connection` (`upsertFlags(Connection, …)` / `mergeFlags(Connection, …)`); keep testcase+assertions already connection-scoped; `upsertDetails` calls them inside one `withTransaction` | Minimal SQL change; removes per-table `withConnection`. |
| KTD3 | Early-return before borrow when payload has no member rows and no testcases (R3); if only some tables are non-empty, still one TX covering those writes | Avoids useless checkout. |
| KTD4 | Do not change `SubmissionDetailPersistGate` — failure still `completeExceptionally`; `await` still logs and returns | Satisfies R6 without new gate behavior. |
| KTD5 | Prove AE1 with a counting/`getConnection` spy `DataSource` in unit tests (Postgres + H2 writers); prove AE2 with in-memory H2 or forced throw after first write + assert rollback | Existing `GradingResultJdbcWriterTest` / `H2GradingResultJdbcWriterTest` are the homes. |

### Assumptions

- Nested `withConnection` must not be called from inside `withTransaction` (would re-borrow); helpers take `Connection` only.
- Postgres `persistUpload` stays as today’s single-statement CTE (no TX wrapper required for this plan).

### High-Level Technical Design

Directional only — not implementation specification.

```text
upsertDetails(payload)
  if empty → return
  withTransaction(ds):
    upsertFlags(conn, field|method|constructor|relation)  // skip empty
    upsertTestcases(conn, …) → upsertAssertions(conn, …)
    commit
  on error → rollback
```

Same for H2 `merge*` helpers.

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Longer single connection hold | Still shorter wall-clock than 5 Neon RTTs; stays on `persistExecutor` |
| Helper still calls `withConnection` by mistake | Code review + AE1 connection-count test |
| Autocommit restore forgotten | Centralize in `withTransaction` finally block |

### Open Questions

**Deferred (non-blocking):** None.

## Implementation Units

### U1. Shared `withTransaction` helper

- **Goal:** One reusable borrow + explicit TX for detail writers.
- **Requirements:** R1, R2
- **Approach:** Add `withTransaction` beside `withConnection` in `GradingResultJdbcWriterSupport`: get connection, save autocommit, `setAutoCommit(false)`, run work, `commit`, on failure `rollback` and rethrow as `IllegalStateException` (same wrapping style as `withConnection`), restore autocommit, `releaseConnection`. Honors KTD1.
- **Files:** `backend/src/main/java/grading/GradingResultJdbcWriterSupport.java`
- **Patterns:** `H2GradingResultJdbcWriter.persistUploadOnce` TX block (~71–88)
- **Test scenarios:** Covered indirectly via U2/U3 writer tests; optional tiny unit test with mock `Connection` if cheap.
- **Dependencies:** None

### U2. Postgres `upsertDetails` single TX

- **Goal:** One borrow / one commit for Postgres detail UPSERT.
- **Requirements:** R1, R2, R3, R4, R6
- **Approach:** Change `upsertFlags` / `upsertTestcases` to take `Connection` (drop inner `withConnection`). `upsertDetails`: if all lists empty, return; else `withTransaction` and call helpers. Leave `persistUpload` unchanged. Honors KTD2, KTD3.
- **Files:** `backend/src/main/java/grading/PostgresGradingResultJdbcWriter.java`
- **Test scenarios:**
  - Happy / AE1: non-empty multi-table payload → `DataSource.getConnection` once.
  - Edge / R3: null/empty payload → no `getConnection`.
  - Error / AE2: second table throws → rollback invoked; no partial commit (mock or H2-backed if preferred for Postgres path characterization).
- **Files (tests):** `backend/src/test/java/unit/com/eiu/capstone/backend/grading/GradingResultJdbcWriterTest.java`
- **Verification:** `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.grading.GradingResultJdbcWriterTest" test`
- **Dependencies:** U1

### U3. H2 `upsertDetails` single TX

- **Goal:** Same one-borrow / one-TX on desktop.
- **Requirements:** R1, R2, R3, R5
- **Approach:** Same refactor for `mergeFlags` / `mergeTestcases` with `Connection`; wrap `upsertDetails` in `withTransaction`. Do not change H2 `persistUpload` TX (already correct). Honors KTD2, KTD5.
- **Files:** `backend/src/main/java/grading/H2GradingResultJdbcWriter.java`
- **Test scenarios:**
  - Happy / AE3: existing H2 upsertDetails test still green; add connection-count or commit-once assertion if not already present.
  - Error / AE2: mid-batch failure rolls back on real H2 in-memory DS.
- **Files (tests):** `backend/src/test/java/unit/com/eiu/capstone/backend/grading/H2GradingResultJdbcWriterTest.java`
- **Verification:** `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.grading.H2GradingResultJdbcWriterTest" test`
- **Dependencies:** U1

### U4. DOX: detail UPSERT one TX

- **Goal:** Document all-or-nothing one-borrow detail persist.
- **Requirements:** R1, R2, R5
- **Approach:** Update `grading/AGENTS.md` (and `docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md` if it implies per-table commits) to state detail UPSERT is one TX / one borrow on Postgres and H2; upload scores remain separate.
- **Files:** `backend/src/main/java/grading/AGENTS.md`; optionally `docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md`; `docs/GRADING_WORKFLOWS.md` only if it claims per-statement detail commits.
- **Test expectation:** none — docs only
- **Dependencies:** U2, U3

## Verification Contract

- `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.grading.GradingResultJdbcWriterTest,unit.com.eiu.capstone.backend.grading.H2GradingResultJdbcWriterTest" test`
- Prefer also `mvn -f backend/pom.xml "-Dtest=regression.com.eiu.capstone.backend.grading.GradingResultStoreReuploadRegressionTest" test` (still mocks writer; ensures schedule path untouched)
- AE1: connection acquire count = 1 for multi-table upsertDetails
- AE2: rollback on mid-failure; upload persist independent
- AE3: H2 path green; gate contract unchanged (code review of `SubmissionDetailPersistGate`)

## Definition of Done

- Postgres and H2 `upsertDetails` use one borrow + one commit (R1, R2, R5).
- Empty payload no-ops without borrow (R3).
- Upload `persistUpload` and gate scheduling unchanged (R4, R6, R7).
- Tests cover connection count and rollback; DOX updated.
