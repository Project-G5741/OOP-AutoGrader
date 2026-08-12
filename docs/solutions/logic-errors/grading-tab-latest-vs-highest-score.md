---
title: Grading tab showed latest attempt score instead of highest lab score
date: 2026-08-12
category: logic-errors
module: lecturer-analytics
problem_type: logic_error
component: service_object
symptoms:
  - "Lecturer Grading tab matrix shows 0 for a lab when submission history lists higher prior attempts"
  - "Per-lab total/average disagrees with best attempt visible in attempt history"
root_cause: logic_error
resolution_type: code_fix
severity: medium
tags:
  - grade-overview
  - highest-score
  - lecturer-dashboard
  - student-lab-progress
related_components:
  - database
  - frontend_stimulus
---

# Grading tab showed latest attempt score instead of highest lab score

## Problem

On the lecturer **Grading** tab (`GET /api/lecturer/grade-overview`), each student's per-lab cell and total score reflected the **latest** `lab_submission` row, not their **best** score for that lab. After a student re-submitted with a lower grade, the matrix showed the lower value while submission history still listed earlier higher attempts.

## Symptoms

- Cross-lab grade matrix cell shows the newest attempt score (e.g. 0) when history shows 75 on prior attempts for the same lab.
- Score sort and export totals follow the wrong per-lab values.
- Overview **at-risk** count can be inflated when latest attempts are low but highest averages remain above the threshold.

## What Didn't Work

- No frontend bug — `GradeOverviewTable` renders API `labScores` directly.
- Lab **Student roster** was already fixed to use `student_lab_progress.highest_score` (see `docs/plans/2026-08-10-004-feat-lecturer-roster-highest-score-sort-plan.md`); the grade-overview queries were left on the old latest-submission join.

## Solution

Switch grade-overview SQL in `LecturerAnalyticsRepository` to read **`student_lab_progress.highest_score`**, gated with `last_submitted_at IS NOT NULL` so non-submitters stay null in the matrix.

Shared CTE (used for at-risk count and grade-overview student sort):

```sql
highest_scores AS (
    SELECT p.user_id, p.lab_id, p.highest_score AS score
    FROM student_lab_progress p
    WHERE p.last_submitted_at IS NOT NULL
)
```

Per-student lab scores for the matrix (`findLabScoresForStudents`):

```sql
SELECT p.user_id, p.lab_id, p.highest_score
FROM student_lab_progress p
WHERE p.user_id IN (:studentIds)
  AND p.last_submitted_at IS NOT NULL
```

Extract the CTE as `HIGHEST_SCORES_CTE` in Java to avoid duplicating the fragment between `AT_RISK_STUDENT_COUNT` and `findGradeOverviewStudents`.

**Files:** `backend/src/main/java/com/eiu/capstone/backend/analytics/repository/LecturerAnalyticsRepository.java`

**Docs:** `backend/AGENTS.md`, `frontend/src/components/lecturer/AGENTS.md`

**Plan:** `docs/plans/2026-08-12-003-fix-grading-tab-highest-score-plan.md`

## Why This Works

`student_lab_progress.highest_score` is maintained on each upload in `SubmissionController` when a new score exceeds the stored best. The buggy queries joined `lab_submission` on `MAX(attempt_number)`, which always returns the latest attempt's score regardless of quality.

Using progress rows aligns the Grading matrix with roster semantics and with what lecturers infer from attempt history MAX. Submission history (`GET /api/analytics/student/{studentId}`) intentionally remains per-attempt — only the aggregate matrix uses highest scores.

Total score rule unchanged: sum of per-lab highest scores divided by lab count, with missing labs as 0.

## Prevention

- When adding lecturer score surfaces, decide explicitly: **latest attempt** (student dashboard) vs **highest lab score** (lecturer roster, grade matrix). Document the rule in the owning `AGENTS.md` API bullet.
- If fixing one lecturer view (roster), grep for parallel queries on `lab_submission` ordered by `attempt_number DESC` or `MAX(attempt_number)` in `LecturerAnalyticsRepository`.
- Prefer `student_lab_progress.highest_score` over per-request `MAX(score)` on submissions — the field is already maintained and cheaper to read.

## Related Issues

- `docs/plans/2026-08-10-004-feat-lecturer-roster-highest-score-sort-plan.md` — same highest-score rule for lab roster
- `docs/plans/2026-08-08-004-feat-grading-row-submission-history-plan.md` — submission history panel; explicitly did not change grade-overview scoring at that time
