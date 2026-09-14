---
title: Off-thread plagiarism inspect - Plan
date: 2026-09-14
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
---

# Off-thread plagiarism inspect - Plan

## Goal Capsule

**Objective:** Remove plagiarism inspect from the student upload HTTP wait. Snapshot fingerprint signals before the request ends; run compare/persist on `persistExecutor`. Detection semantics, lecturer APIs, and swallow-on-failure stay the same.

**Product authority:** Session 2026-09-14 (measured Render/local timing; user-directed — chosen over compile/JAVA_OPTS/Render upgrades). Eventual consistency of a few seconds for flags is acceptable; scores must not wait on inspect.

**Open blockers:** None.

## Product Contract

### Summary

Upload returns scores as soon as grade + persist finish. Multipart is snapshotted into `PlagiarismSignals` on the request thread. Inspect (fingerprint save, pairwise compare, score-gate, peer-side re-eval) runs on the existing 2-thread `persistExecutor`. Lecturer roster/flag reads are live SQL and show flags after inspect commits. Lab statistics cache is invalidated again after inspect so `plagiarismRate` is not frozen without the new flags.

### Requirements

- R1. Upload HTTP `plagiarism` timing is extract + schedule only (milliseconds), not Neon compare.
- R2. Inspect still runs after persist with unchanged detection/score-gate/latest-attempt rules.
- R3. Extract/inspect/schedule failures are swallowed; upload remains 200.
- R4. Signals are captured before the response; inspect must not read multipart or the temp submission folder.
- R5. Lecturer flags appear without a forever refresh: typically within the previous inspect duration (~1–3s), queued behind other `persistExecutor` work if the pool is busy. Document this lag.
- R6. No new tables; no Render/JAVA_OPTS/compile-parallelism changes.

### Acceptance Examples

- AE1. Upload timing block: `plagiarism` is snapshot/schedule; a `[timing] Plagiarism inspect` block logs off-thread `inspect` ms.
- AE2. First-time copy still flags; lecturer roster shows ORIGINAL/PLAGIARIZER after inspect completes.
- AE3. Forced inspect exception off-thread: student still gets 200 and scores.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | `persistExecutor`, not an after-commit event or new queue | Same off-request pool as sidecars, detail UPSERT, and temp delete (`docs/solutions/architecture-patterns/grading-result-jdbc-upsert-deferred-details.md`). Persist has already committed before inspect today. (session-settled: user-directed) |
| KTD2 | Snapshot `PlagiarismSignals` (hashes + git metadata) on the request thread | Multipart and temp folders are gone after the response. Signals are small and already what inspect compares. |
| KTD3 | `inspectUpload(submission, signals)` is the transactional work; files overload extracts then inspects for tests | Controller must call the Spring bean signals method so `@Transactional` applies off-thread (no self-invocation). |
| KTD4 | Invalidate lab statistics + lecturer overview after persist **and** after inspect | Immediate score refresh; second invalidate so `plagiarismRate` is not cached 120s without new flags. |
| KTD5 | No new table | JVM crash before inspect can drop flags for that attempt (same class of risk as sidecar JSON). Acceptable. |

### Failure / consistency

- Extract throws: log, skip schedule, student 200, no fingerprint for this attempt.
- Inspect throws off-thread: log, student already 200.
- Lecturer GET roster/flags: live SQL; flags appear on next load after inspect commits (~1–3s typical).
- Process death before inspect: that attempt has no fingerprint/matches until a later upload from the same student re-inspects their new attempt.

### Implementation Units

- **U1.** `PlagiarismService.inspectUpload(submission, signals)` + files convenience overload.
- **U2.** `SubmissionController` snapshot + `persistExecutor` schedule + off-thread timing + post-inspect cache invalidate.
- **U3.** Tests + DOX (`plagiarism/AGENTS.md`, `backend/AGENTS.md`, `grading/AGENTS.md`, `CONCEPTS.md`, `docs/GRADING_WORKFLOWS.md`, `docs/HOW_IT_RUNS.md`).

## Verification Contract

- Keep `PlagiarismServiceInspectTest` green (files overload still extracts then inspects).
- Add a signals-path test and a swallow-on-inspect-failure characterization if a controller-level test is practical; otherwise controller remains thin and tests stay on the service.
- `mvn -f backend/pom.xml "-Dtest=support.com.eiu.capstone.backend.plagiarism.*" test` then `mvn -f backend/pom.xml test` when time allows.

## Definition of Done

- Upload `plagiarism` line is snapshot/schedule only.
- Off-thread inspect timing is logged.
- Flag semantics unchanged; lecturer lag documented.
- DOX chain updated. No schema, no Render/JAVA_OPTS/compile changes.
