---
title: lab_result bundle mapped to wrong challenge tabs after upload
date: 2026-08-10
category: logic-errors
module: grading engine
problem_type: logic_error
component: service_object
symptoms:
  - "After upload, Class/MMD/Testcase tabs could show another challenge's grading data"
  - "Sidebar challenge score from lab_result did not match the selected challenge when challenge numbers were non-contiguous"
  - "Upload grade_ms included redundant per-challenge DB reads during lab_result assembly"
root_cause: wrong_api
resolution_type: code_fix
severity: high
tags:
  - lab-result
  - challenge-number
  - student-dashboard
  - lab-result-assembler
  - performance
  - n-plus-one
---

# lab_result bundle mapped to wrong challenge tabs after upload

## Problem

The grading engine rebuild returns an upload-time `lab_result` bundle so the student UI can render Class, MMD, and Testcase tabs without follow-up API calls. Two defects undermined that contract: the frontend indexed bundles by sidebar array position instead of rubric challenge number, and the backend re-queried rubric structure and submission results once per challenge during assembly.

## Symptoms

- Selecting a challenge after upload could show class/MMD/testcase detail belonging to a different challenge when challenge numbers were not contiguous `1..N` (for example rubric numbers `1`, `3`, `5`).
- Sidebar scores derived from cached bundles could attach to the wrong `challenge.id`.
- Upload timing logs showed a large `grade_ms` slice partly from `LabResultAssembler` doing per-challenge structure loads and `loadCorrectIds()` calls even though grading had just finished in memory.

## What Didn't Work

- Assuming API challenge list order always equals `challenge_1`, `challenge_2`, … keys in `lab_result`. Backend keys bundles by `challengeRubric.challengeNumber()` (`LabResultAssembler.java`), not list index.
- Re-reading persisted submission results from the database immediately after `GradingResultStore.save()` inside assembly. Correctness was fine after commit, but the round-trips were unnecessary when `GradingComputationResult` already held the outcomes.

## Solution

### 1. Expose challenge number on the challenges API

Add `challengeNumber` to `ChallengeDTO` and populate it in `ChallengeService`:

```java
public record ChallengeDTO(UUID id, int challengeNumber, String name, Integer score) {}
```

### 2. Index upload bundles by challenge number on the frontend

Replace array-index mapping with explicit number lookup:

```javascript
function indexLabResultByChallengeId(labResult, challenges) {
  if (!labResult) return {};
  const indexed = {};
  for (const challenge of challenges) {
    const challengeNumber = challenge.challengeNumber ?? challenge.challenge_number;
    if (challengeNumber == null) continue;
    const bundle = labResult[`challenge_${challengeNumber}`];
    if (bundle) {
      indexed[challenge.id] = bundle;
    }
  }
  return indexed;
}
```

(`frontend/src/pages/StudentDashboard.jsx`)

### 3. Batch rubric structure and reuse in-memory grading outcomes

Introduce `LabChallengeStructureBundle` loaded once via `ClassStructureService.loadChallengeStructures(challengeIds)` — one batched query round per entity type for all challenges in the lab.

`LabResultAssembler.assemble()` then:

1. Builds `SubmissionCorrectIds` from `GradingComputationResult` (`correctIdsFrom`) instead of calling `SubmissionResultLoader.loadCorrectIds()` per challenge.
2. Reuses the shared structure bundle for `buildClassData()` and `buildMmdData()` per challenge.
3. Accepts compile errors from the upload folder map (passed from `GradingService`) so the Class tab shows compile failures on first response before `SubmissionCompileErrorStore.save()` runs.

When `app.grading.timing-log=true`, assembly logs `grading_timing assemble_ms=... challenges=...` for profiling.

## Why This Works

**Mapping bug:** `lab_result` keys are `challenge_<N>` where `N` is the rubric's `challenge_number` column, not the zero-based index in `GET /api/labs/{id}/challenges`. Without `challengeNumber` on the DTO, the frontend had no stable join key except list position, which breaks when numbers have gaps or ordering assumptions drift.

**Performance:** Assembly only needs rubric shape (batched once) and which element IDs graded correct (already in `computed`). Per-challenge DB reloads duplicated work grading had just produced and multiplied query count by the number of challenges.

## Prevention

- When backend payloads are keyed by rubric ordinal (`challenge_<N>`), expose `N` on any list DTO the UI uses to join — do not infer from array index.
- After grading computes outcomes in memory, prefer assembling read models from `GradingComputationResult` plus one batched rubric load; reserve `SubmissionResultLoader` for historical/read endpoints outside the upload hot path.
- Watch `grading_timing assemble_ms` after changes to `LabResultAssembler` or `ClassStructureService`.

## Related Issues

- [In-memory per-challenge compile on the submission upload path](../architecture-patterns/in-memory-challenge-compile-path.md) — adjacent upload-path performance work
- `CONCEPTS.md` — **lab_result bundle**, **Grading pillar**
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` — upload `lab_result` contract
