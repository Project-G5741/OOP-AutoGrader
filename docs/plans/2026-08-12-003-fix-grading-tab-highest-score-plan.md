---
title: Grading Tab Highest Score - Plan
type: fix
date: 2026-08-12
topic: grading-tab-highest-score
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
---

# Grading Tab Highest Score - Plan

## Goal Capsule

- **Objective:** Fix the lecturer **Grading** tab cross-lab matrix so each per-lab cell and the **total** column reflect the student's **highest lab score**, not the latest attempt score.
- **Product authority:** This plan owns `GET /api/lecturer/grade-overview` and the aligned at-risk total on `GET /api/lecturer/overview`. Lab roster highest-score behavior (2026-08-10 roster plan) is precedent, not active scope. Submission history panel continues to list every attempt with its own score.
- **Open blockers:** None — implementation is in the working tree; verification and commit remain.

## Product Contract

### Summary

The Grading tab grade matrix shows each student's score per lab. Lecturers expect those cells to match **best performance** for that lab — the same rule already applied to the lab Student roster. The API was still joining `lab_submission` on the latest attempt (`MAX(attempt_number)`), so a student who scored 75% then re-submitted for 0% appeared as 0% in the matrix while submission history correctly showed both attempts.

### Problem Frame

Lecturers use the grade matrix to scan class performance across labs. Latest-attempt semantics punish students who experiment or regress after a good score, and disagree with attempt-history MAX and `student_lab_progress.highest_score` maintained on upload.

### Actors and Entry Points

- **Primary actor:** Lecturer on `LecturerDashboard` → `activeNav === 'grading'` (route `/lecturer-grading`).
- **Entry points:** `GradeOverviewTable` cells, total column, score sort, grade-overview export (`exportGradeOverview`).

### Requirements

- R1. Each per-lab score in the grade matrix is the student's **highest lab score** for that lab when they have submitted at least once.
- R2. Students with no submission for a lab show the existing empty treatment (`—` / null), not zero.
- R3. The **total** column is the average of highest per-lab scores across all labs, with **missing labs counted as 0** (existing rule).
- R4. Server-side **score sort** orders by that same total (highest-score average), not latest-attempt average.
- R5. **At-risk student count** on lecturer overview uses the same highest-score total rule as R3 (threshold &lt; 70).
- R6. Grade-overview **export** reflects highest per-lab scores and totals from the API (no frontend change required).
- R7. Submission history panel (`GET /api/analytics/student/{studentId}`) is unchanged — each row shows the score for that specific attempt.

### Flows and State

- F1. Lecturer opens Grading tab → matrix loads → Lab 2 cell shows 75 when latest attempt is 0 and best is 75.
- F2. Lecturer sorts by score → ordering matches highest-score totals across the enrolled population.
- F3. Lecturer exports → exported per-lab and total columns match the matrix.

### Acceptance Examples

- AE1. Student with Lab 2 attempts 75, 75, 75, 75, 0 (latest) → matrix Lab 2 cell shows **75**; history still lists 0 on the newest row.
- AE2. Student with no Lab 3 submission → Lab 3 cell shows placeholder; total divides sum including 0 for Lab 3.
- AE3. Overview at-risk count does not spike when students have low latest attempts but acceptable highest averages.

### Key Decisions

- KTD1. **Read `student_lab_progress.highest_score`, not MAX over submissions** — Same source as lab roster fix (see `docs/plans/2026-08-10-004-feat-lecturer-roster-highest-score-sort-plan.md`). **Rejected:** latest-attempt join (bug) and per-request `MAX(score)` over `lab_submission` (extra cost, duplicates maintained field).
- KTD2. **Gate on `last_submitted_at IS NOT NULL`** — Only expose a numeric score when the student has submitted to that lab; matches roster `CASE WHEN latest_sub.id IS NOT NULL` semantics.
- KTD3. **Backend-only fix** — Frontend already renders API `labScores` and `totalScore`; no UI change unless verification finds a client-side override.

### Scope Boundaries

**In scope**

- `LecturerAnalyticsRepository`: `findLabScoresForStudents`, `findGradeOverviewStudents`, `AT_RISK_STUDENT_COUNT`.
- DOX: `backend/AGENTS.md`, `frontend/src/components/lecturer/AGENTS.md`.

**Out of scope**

- Lab roster (already highest-score).
- Challenge-tab rosters.
- Student dashboard (latest attempt by design).
- Recomputing or backfilling `highest_score` on upload.
- Changing submission history API or panel behavior.

### Success Criteria

- SC1. `GET /api/lecturer/grade-overview` per-lab `labScores` match `student_lab_progress.highest_score` for submitters.
- SC2. Total and score sort align with highest-score average.
- SC3. At-risk count uses highest-score totals.
- SC4. No regression in pagination, name sort, or export shape.

## Planning Contract

### Summary

Root cause is three SQL paths in `LecturerAnalyticsRepository` that built a `latest_scores` CTE from `lab_submission` ordered by `attempt_number`. Replace with `highest_scores` from `student_lab_progress` where `last_submitted_at IS NOT NULL`. `LecturerAnalyticsService.getGradeOverview` already maps `findLabScoresForStudents` into row DTOs — no service logic change required.

**Product Contract preservation:** Bootstrap from session bug report; no separate brainstorm artifact.

### Key Technical Decisions

- KTD4. **Single repository change** — All three query sites (`findLabScoresForStudents`, grade-overview student sort CTE, at-risk count CTE) switch in one commit to avoid mixed semantics.
- KTD5. **No new endpoint or DTO fields** — Existing `GradeOverviewStudentRowDTO` shape is correct; only query source changes.

### Assumptions

- `highest_score` is accurate on upload (same assumption as roster plan). Stale legacy rows are out of scope.

## Implementation Units

### U1. Backend — highest score in grade-overview queries

**Goal:** Grade-overview API and at-risk count use `highest_score` for per-lab and total calculations.

**Requirements:** R1, R2, R3, R4, R5.

**Dependencies:** None.

**Files:**

- `backend/src/main/java/com/eiu/capstone/backend/analytics/repository/LecturerAnalyticsRepository.java`

**Approach:**

1. Replace `latest_scores` CTE with `highest_scores` selecting `p.user_id`, `p.lab_id`, `p.highest_score AS score` from `student_lab_progress` where `last_submitted_at IS NOT NULL` in `AT_RISK_STUDENT_COUNT` and `findGradeOverviewStudents`.
2. Change `findLabScoresForStudents` to select from `student_lab_progress` with the same `last_submitted_at` gate.

**Patterns to follow:** `findLabStudentRosterInternal` score column (`CASE WHEN latest_sub.id IS NOT NULL THEN p.highest_score`) in the same repository; roster plan U1.

**Test scenarios:**

- Covers AE1. Student with latest 0 and prior 75 → `findLabScoresForStudents` returns 75 for that lab.
- Covers AE2. Student with no progress for a lab → no row for that lab in score map (null in matrix).
- Score sort query returns higher totals before lower when `sort=score,desc`.

**Verification:** `mvn compile -DskipTests` from `backend/`. Manual: restart backend, reload Grading tab for affected student.

**Status:** Applied in working tree (uncommitted).

### U2. DOX — document highest-score grade overview

**Goal:** API docs match behavior for lecturers and future implementers.

**Requirements:** R6 (export semantics documented).

**Dependencies:** U1.

**Files:**

- `backend/AGENTS.md`
- `frontend/src/components/lecturer/AGENTS.md`

**Approach:** Update grade-overview and at-risk bullets to say highest score, not latest submission. Note history panel still shows per-attempt scores.

**Test expectation:** none — documentation only.

**Verification:** AGENTS.md bullets consistent with repository queries.

**Status:** Applied in working tree (uncommitted).

### U3. Manual verification on Grading tab

**Goal:** Confirm end-to-end UX matches acceptance examples.

**Requirements:** R1–R7, SC1–SC4.

**Dependencies:** U1, U2.

**Files:** None (manual only).

**Approach:**

1. Start backend and frontend (`npm start` from repo root).
2. Log in as lecturer → Grading tab.
3. Select student from AE1 scenario → confirm Lab 2 matrix cell shows 75.
4. Toggle score sort and export → confirm totals match matrix.

**Test scenarios:**

- Covers F1, F2, F3 and AE1–AE3.

**Verification:** Screenshot or noted pass on matrix cell, sort order, and export row for test student.

## Verification Contract

| Check | Command / action |
|---|---|
| Backend compiles | `mvn compile -DskipTests` from `backend/` |
| Grade overview API | `GET /api/lecturer/grade-overview?page=0&size=5&sort=studentName,asc` with lecturer JWT — inspect `content[].labScores` vs DB `highest_score` |
| Grading UI | Manual on `/lecturer-grading` per U3 |

No automated test suite exists for analytics SQL; manual API spot-check is the gate.

## Definition of Done

- [ ] U1: All three repository query paths use `highest_scores` / `highest_score` (committed).
- [ ] U2: AGENTS.md updated (committed).
- [ ] U3: Manual verification passes for at least one student with latest &lt; highest on a lab.
- [ ] No mixed semantics (latest score anywhere in grade-overview path).

## Sources & Research

- Prior fix pattern: `docs/plans/2026-08-10-004-feat-lecturer-roster-highest-score-sort-plan.md`
- Grading history panel scope boundary: `docs/plans/2026-08-08-004-feat-grading-row-submission-history-plan.md` (explicitly did not change grade-overview scoring)
- Working tree diff: `LecturerAnalyticsRepository.java`, `backend/AGENTS.md`, `frontend/src/components/lecturer/AGENTS.md`
