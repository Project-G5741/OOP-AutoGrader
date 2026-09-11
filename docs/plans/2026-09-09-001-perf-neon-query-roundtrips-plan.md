---
title: Neon Query Round-Trip Reduction - Plan
date: 2026-09-09
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Neon Query Round-Trip Reduction - Plan

## Goal Capsule

**Objective:** Cut backend-to-Neon round trips on the high- and medium-impact paths from the 2026-09-09 query audit, without changing student-visible scores, Class/MMD/Testcase tab content, plagiarism score-gate rules, history stats numbers, or who receives deadline emails.

**Product authority:** Session 2026-09-09 audit plus user direction to fix all high-to-medium impact items (session-settled: user-directed — chosen over index-only / pg_trgm / overview-SQL rewrites: those do not move latency at current row counts).

**Open blockers:** None.

## Product Contract

### Summary

Neon time is dominated by statement count, not sequential scans of tiny heaps. This work reuses the in-memory rubric cache on student result tabs, batches plagiarism peer-best queries, folds history aggregates, anti-joins deadline-email candidates, drops legacy challenge-roster EXISTS probes, and bulk-deletes a user's grading rows.

### Actors

- A1. Student — Class / MMD / Testcase tabs, history page, upload (plagiarism inspect).
- A2. Lecturer — challenge student roster, user hard-delete.
- A3. Scheduler — 72h / 24h deadline reminder emails.

### Requirements

- R1. `GET …/class` and `GET …/mmd` must assemble from `LabRubricCache` + `buildClassDataFromRubric` / `buildMmdDataFromRubric` + stored correct ids / snapshots / compile errors. They must not call `loadChallengeStructures` on the happy path.
- R2. `GET …/testcases` must resolve the challenge rubric from the same cache by challenge id (no `challenge.findById` + lazy `challenge.getLab()`).
- R3. Tab JSON (member marks, shell status, MMD attributes/relations, testcase cards) stays equivalent to the current from-rubric upload assemble path.
- R4. Plagiarism inspect loads peer lab-best scores in one `GROUP BY user_id` query, not one `MAX(score)` per peer. Score-gate rules unchanged.
- R5. `GET /api/submissions/my-history` stats (`labsAttempted`, `totalSubmissions`, `averageScore` scale-2 DOWN, `bestScore`) come from one aggregate query. Empty scope still returns zeros and null scores.
- R6. Deadline emails still go only to active enrolled students with an email who have no `lab_submission` for that lab and no ledger row for that threshold. Candidate selection is one anti-join, not per-student `COUNT` + `EXISTS`.
- R7. Challenge student roster (count + page) treats a submission as graded for a challenge iff `submission_challenge_result` exists for that pair. Legacy field/method/constructor EXISTS probes are removed. Students whose only evidence is pre-`submission_challenge_result` rows may disappear from that roster.
- R8. Lecturer hard-delete of a user removes progress, enrollments, ledger, tokens, plagiarism rows, grading result rows, and submissions without a per-submission delete loop.
- R9. Out of scope: extra btree indexes, `pg_trgm`, lecturer-overview at-risk SQL rewrite, skipping `requireActiveUser`, persistExecutor multi-statement TX, match-row fan-out per fingerprint.

### Acceptance Examples

- AE1. After a warm rubric cache, opening Class then MMD for a challenge does not issue the eight JPA structure queries (`class_entity` / field / method / constructor / params / relations).
- AE2. Two other students in the lab: upload inspect issues one grouped `MAX(score)` for both, not two.
- AE3. History page with mixed scores: stats match previous COUNT / COUNT DISTINCT / AVG / MAX values including DOWN rounding.
- AE4. Deadline window: students who already submitted or already got that threshold mail are skipped; others still receive one mail and one ledger row.
- AE5. Challenge roster lists students who have a `submission_challenge_result` for that challenge (deadline-aware as today).
- AE6. Delete user with N submissions: one bulk delete per result table, then submissions, then the user row.

### Key Decisions

- KD1. Reuse existing from-rubric mappers for GET tabs. (session-settled: user-approved — chosen over a new single JOIN SQL for structure: cache + mappers already exist for upload.)
- KD2. Drop legacy challenge-roster EXISTS. (session-settled: user-directed — chosen over keeping four OR probes for pre-score rows: 809 challenge_result rows vs 199 submissions; not worth the scale bomb.)

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Add `LabRubricCache.get(UUID labId)` via `LabRepository.getReferenceById` on miss; add `LabRubricSnapshot.challengeById` | Cache hit stays zero extra SQL; miss still uses `loadForLab`. GET paths already have `labId`. |
| KTD2 | `LabSubmissionRepository` grouped `MAX(score)` by `user.id` for a lab + `IN :userIds` | Same numbers as N calls to `bestScoreForUserAndLab`. Guard empty `userIds`. |
| KTD3 | One JPQL `COUNT` / `COUNT DISTINCT lab` / `AVG` / `MAX` with optional `labId` null-or-equal | Matches current filters (`AVG`/`MAX` skip null scores in SQL). Round AVG in Java `DOWN` 2. |
| KTD4 | Native anti-join on `TermEnrollmentRepository` for deadline candidates | Reuses the existing active-student enrollment SQL; adds NOT EXISTS submission and ledger. |
| KTD5 | Replace `CHALLENGE_GRADED_SUBMISSION_EXISTS` with `submission_challenge_result` only | Count and both LATERALs share the same predicate. |
| KTD6 | Bulk `DELETE … WHERE submission.user.id = :userId` (assertions before testcase results; matches involving either side before submissions) | Derived per-entity delete does not cascade assertions. |

### Technical Design

GET Class/MMD: resolve submission → persist gate → `labRubricCache.get(labId)` → `challengeById` → `loadCorrectIds` + filesystem snapshot/errors → existing from-rubric builders.

Plagiarism: `bestScoresForLabUsers` fills the map from one repository query; missing users stay `ZERO` as today.

Delete order in `UserService.purgeUserData`: progress / enrollment / ledger / tokens → plagiarism matches involving the user → fingerprints by user → assertion results → testcase results → field/method/constructor/challenge/relation results → `lab_submission` → caller deletes `user_account`.

### Assumptions and Risks

- A1. Every current graded attempt that lecturers care about on the challenge roster has `submission_challenge_result` (supported by live 809 vs 199 counts). Residual pre-score rows drop off that roster.
- A2. Hibernate bulk delete with `r.submission.user.id` is valid on this JPA version; if not, native SQL with a subquery is the fallback.

### Scope Boundaries

- Do not change grading math, plagiarism flag rules, or email copy.
- Do not add schema migrations.
- Leave `loadChallengeStructures` in place for lecturer structure GET / any remaining JPA bundle tests.

## Implementation Units

### U1. Student result tabs from rubric cache

Governs: R1 R2 R3 AE1

Files: `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricCache.java`, `LabRubricSnapshot.java`, `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`, `backend/src/test/java/support/com/eiu/capstone/backend/service/ClassStructureServiceShellDisplayTest.java`, `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`, `backend/src/main/java/com/eiu/capstone/backend/service/AGENTS.md`, `backend/AGENTS.md`

Test scenarios:

- TS1. `challengeById` returns the rubric whose `challengeId` matches and empty for unknown ids.
- TS2. Class GET path uses `buildClassDataFromRubric` (existing shell-display from-rubric tests remain green).

### U2. Batched plagiarism peer bests

Governs: R4 AE2

Files: `backend/src/main/java/com/eiu/capstone/backend/repository/LabSubmissionRepository.java`, `backend/src/main/java/com/eiu/capstone/backend/plagiarism/PlagiarismService.java`, `backend/src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md`, `backend/src/test/java/support/com/eiu/capstone/backend/plagiarism/PlagiarismServiceBestScoreTest.java`

Test scenarios:

- TS3. Empty peer set does not call the repository.
- TS4. Two peer ids produce one repository call and the returned MAX values (null → ZERO).

### U3. History stats one query

Governs: R5 AE3

Files: `LabSubmissionRepository.java`, `backend/src/main/java/com/eiu/capstone/backend/service/StudentHistoryService.java`, `backend/src/test/java/support/com/eiu/capstone/backend/service/StudentHistoryServiceTest.java`

Test scenarios:

- TS5. `getHistory` mocks a single stats row and still exposes labsAttempted / totalSubmissions / rounded average / best.
- TS6. Zero submissions still yields zeros and null scores without extra aggregate mocks.

### U4. Deadline email anti-join

Governs: R6 AE4

Files: `TermEnrollmentRepository.java`, `backend/src/main/java/com/eiu/capstone/backend/service/LabDeadlineEmailService.java`, `backend/src/test/java/support/com/eiu/capstone/backend/service/LabDeadlineEmailServiceTest.java`

Test scenarios:

- TS7. `sendForLabThreshold` loads candidate ids once, then `findAllById`; does not call `countByUser_IdAndLab_Id` or ledger `exists`.

### U5. Challenge roster EXISTS

Governs: R7 AE5

Files: `backend/src/main/java/com/eiu/capstone/backend/analytics/repository/LecturerAnalyticsRepository.java`, `backend/AGENTS.md`

Test scenarios: existing authorization/support tests still compile; no dedicated roster SQL test in repo — keep predicate identical in count and page SQL.

### U6. Bulk user purge

Governs: R8 AE6

Files: result repositories, `SubmissionPlagiarismMatchRepository.java`, `UserService.java`, `UserServiceTest.java`, `backend/AGENTS.md`

Test scenarios:

- TS8. Delete with no submissions still bulk-deletes result tables (idempotent) and does not loop `findByUser_Id`.
- TS9. Delete with submissions does not call per-submission `deleteBySubmission`.

## Verification Contract

- `mvn -f backend/pom.xml -Dtest=ClassStructureServiceShellDisplayTest,StudentHistoryServiceTest,UserServiceTest,LabDeadlineEmailServiceTest,PlagiarismServiceBestScoreTest test`
- If that slice is green, run `mvn -f backend/pom.xml test` when time allows (full suite is the backend contract).

## Definition of Done

- All six units implemented.
- Tab/history/plagiarism/email/delete contracts in AGENTS.md match the new query shapes.
- Named tests pass.
- No user-facing score or email-copy change.
