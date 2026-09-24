---
title: "Lighting student operational testcases on upload"
date: 2026-09-25
category: architecture-patterns
module: grading
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "Flipping a scoring pillar from intentionally dark to student-facing"
  - "Reusing an isolated worker already used by lecturer dry-run for student upload"
  - "Keeping prior attempts unchanged while new uploads pick up a new pillar"
tags: [operational-testcase, student-upload, workerJvmSlot, scoreApplicability, is_hidden]
related_components:
  - grading
  - frontend
---

# Lighting student operational testcases on upload

## Context

Unit/Composition OT authoring and lecturer dry-run shipped while student upload hard-kept `testcaseApplicable = false`, skipped `TestcaseGrader`, returned empty `GET .../testcases`, and hid the Operation Test tab. Lecturers already set `testcase_weight` and `is_hidden` for a deferred student ship.

## Guidance

1. **Applicability from rubric presence** — `testcaseApplicable = !challengeRubric.testcases().isEmpty()`. Empty OT challenges stay Class (+ MMD) only; no per-lab enable switch.
2. **One worker session per upload batch** — When any uploaded challenge needs OT, acquire `workerJvmSlot`, open one `WorkerSession` on the submission root, pass the handle into every `gradeChallenge`, release in `finally`. Challenges without OT still skip the grader.
3. **Reuse `TestcaseGrader`** — Same invoke path as dry-run; do not invent a second runner.
4. **Student-safe mapping** — `TestcaseResultMapper` already nulls I/O for `is_hidden`; omit type/principle tags on student DTOs.
5. **Revisit from persisted rows** — `GET .../testcases` awaits the detail persist gate and maps only when submission testcase rows exist. Pre-ship attempts stay empty (R13 next-upload cutover).
6. **Frontend tab gate** — Show Operation Test when `scoreApplicability.testcase === true` **or** the challenge bundle has a non-empty `testcases` array; wire `fetchChallengeDetails` to parallel-fetch `/testcases`.

## Why this works

Pillar scoring (`PillarScoreAggregator`) and Example/Other UI already existed; darkness was a single hard-coded gate plus empty GET stub. Lighting flips those gates without redesigning authoring or I/O cards. Persisted-row revisit preserves old attempts without a migration.

## Related

- Plan: `docs/plans/2026-09-25-002-feat-student-operational-testcase-availability-plan.md`
- Prior dark decision: `docs/plans/2026-09-23-001-feat-operational-testcase-unit-composition-plan.md` (KTD2)
- Applicability pattern: `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md`
- I/O disclosure: `docs/plans/2026-08-11-002-feat-operation-test-io-card-plan.md`
