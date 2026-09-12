---
title: Upload Hot-Path Neon Round-Trip Cut - Plan
date: 2026-09-12
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Upload Hot-Path Neon Round-Trip Cut - Plan

## Goal Capsule

**Objective:** Cut student-visible `POST …/upload` wait by collapsing serial Neon round-trips on the hot path, targeting about **1s** off a warm ~2.4s upload, without changing scores, `lab_result` shape, attempt numbering, or access-control outcomes.

**Product authority:** Session 2026-09-12. User directed all three measures (session-settled: user-directed — chosen over async-every-query / WebFlux, and over shrinking upload JSON: those do not shorten the browser wait while the controller still joins Neon before `return`). Do not commit in this run.

**Open blockers:** None.

## Product Contract

### Summary

The labeled `[timing] Upload` stages (`rubric`/`compile`/`grade`/`plagiarism`) summed to ~335ms while `total` was 2409ms. The gap is sequential Postgres transactions (auth, lab, current term, enrollment, `MAX(attempt)`, insert-at-0, score update, count, progress load/save) plus challenge-score UPSERT. This work keeps auth-before-compile and a durable `lab_submission` row before the HTTP response; it batches those writes and overlaps independent reads.

### Actors

- A1. Student — uploads a lab folder and waits for scores + Class/MMD/Testcase cache from `lab_result`.

### Requirements

- R1. One database round-trip loads the uploader, the lab with term, and whether they are enrolled in that term when it is current. Same HTTP statuses and student-facing deny messages as today on the happy-path error cases (401 unknown user, 404 missing lab, 403 inactive / not enrolled / lab not in current quarter / lab not open).
- R2. Compile does not start until that access check succeeds (do not compile untrusted sources first).
- R3. `MAX(attempt_number)` runs concurrently with compile. Attempt numbers stay `MAX+1` (path `{attemptNumber}` still unused).
- R4. Assign `lab_submission.id` in memory before grade. Grade compute uses that id. Persist happens **after** compute: one transaction inserts the row **with the final score**, UPSERTs `submission_challenge_result`, writes the parsed snapshot, and UPSERTs `student_lab_progress`. No insert-at-0 then score update. No extra `COUNT(lab_submission)` for `totalSubmissions` (use the assigned attempt number).
- R5. Detail member/testcase UPSERT stays off-thread and must not start until the submission row is committed (FK).
- R6. `[timing] Upload` prints `access`, `compile`, `attempt`, `grade`, `persist`, `plagiarism`, `total` when `app.grading.timing-log=true`.
- R7. Upload JSON (`challengeResult`, `lab_result`, scores, attempt, `latestSubmission`) stays equivalent. Students still receive a durable `submissionId`.

### Acceptance Examples

- AE1. Warm re-upload of the same 8-challenge lab: `[timing] Upload` `total` drops by about 1s vs the 2409ms baseline; `access` is one query; `attempt` is ≪ a Neon RTT when compile is slower than `MAX`; `persist` covers insert+scores+progress together; `plagiarism` remains ~0 when disabled.
- AE2. First attempt on a lab still gets `attemptNumber`/`totalSubmissions` = 1.
- AE3. Inactive, out-of-term, hidden, or unknown-lab uploads still return the same 401/403/404 families before compile.
- AE4. Immediate Class tab after upload still waits on the detail persist gate and shows member marks.

### Key Decisions

- KD1. Keep a durable insert on the request thread. (session-settled: user-directed — chosen over returning a UUID before insert: crash would show a grade that never landed.)
- KD2. Do not defer challenge-score UPSERT off-thread. (session-settled: user-approved — refresh sidebar reads `submission_challenge_result`; keep it in the persist transaction.)
- KD3. Do not shrink `lab_result`. (session-settled: user-directed — student dashboard caches the bundle; assemble was 0ms.)

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | `UserAccount LEFT JOIN Lab ON id LEFT JOIN term` plus enrollment `COUNT` subquery, mapped in `StudentTermAccessService.requireUploadAccess` | One happy-path RTT; preserves 401 vs 404 by left-joining from the user. |
| KTD2 | Assigned UUID on `LabSubmission` (`setId` + `@PrePersist` fallback; drop `@GeneratedValue`) | Grade can run before insert; JDBC UPSERT needs the id; Hibernate assigned ids are reliable. |
| KTD3 | `UploadPersistService` `@Transactional` after compute: save submission, `saveChallengeScores`, snapshot file, progress | One connection/commit for the writes the student waits on. JdbcTemplate joins the Spring TX. |
| KTD4 | `scheduleDetailPersist` in `afterCommit` | Avoids racing the `lab_submission` FK. |
| KTD5 | `CompletableFuture.supplyAsync` for `findMaxAttemptNumber` started immediately before `processUpload` | Hides the MAX RTT behind compile; must not use `compileExecutor` (busy). |
| KTD6 | `totalSubmissions = assignedAttempt` | Equal to `COUNT` after a successful insert of `MAX+1`. |

### Technical Design

Upload: JWT email present → `requireUploadAccess` → start MAX future → compile → join MAX → new `LabSubmission` with UUID + attempt → `gradeSubmission` compute+assemble only → `UploadPersistService.persist` → plagiarism (flag) → sidecars + cache invalidate → response.

### Scope Boundaries

- Do not make auth/access async past the response.
- Do not switch to WebFlux/R2DBC.
- Do not change plagiarism, grading math, or GET tab assembly.
- Other controllers keep `requireCanSubmit` (multi-query) unless they are later folded in.

### Assumptions and Risks

- A1. Hibernate 6 accepts `LEFT JOIN Lab l ON l.id = :labId` in the access query; native SQL is the fallback.
- A2. Warm Neon RTT ~150–250ms; five fewer transactions ≈ 1s. Cold compute wake can still spike.

## Implementation Units

### U1. One-query upload access

Governs: R1 R2 AE3

Files: `UserAccountRepository.java`, `StudentTermAccessService.java`, `StudentTermAccessServiceTest.java`, `service/AGENTS.md`

### U2. Assigned id + one persist transaction

Governs: R4 R5 AE4

Files: `LabSubmission.java`, `GradingService.java`, `GradingOutcome.java`, `UploadPersistService.java`, `SubmissionController.java`, `grading/AGENTS.md`

### U3. Overlap MAX with compile + timing

Governs: R3 R6 R7 AE1 AE2

Files: `SubmissionController.java`, `backend/AGENTS.md`, `CONCEPTS.md`, `docs/HOW_IT_RUNS.md`, `docs/GRADING_WORKFLOWS.md`

## Verification Contract

- `StudentTermAccessServiceTest` covers 401/404/403 mapping from one access row (no extra term/enrollment mocks on the happy deny paths).
- Persist: submission is saved with final score before challenge UPSERT; details scheduled only after commit (unit with mocks).
- `mvn test` from `backend/` for touched tests.
- Manual: upload with `app.grading.timing-log=true`; confirm new Upload labels and ~1s `total` drop on a warm repeat.
