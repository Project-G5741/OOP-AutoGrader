---
title: Student Dashboard Conditional Grade - Plan
type: feat
date: 2026-08-10
topic: student-dashboard-conditional-grade
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Student Dashboard Conditional Grade - Plan

## Goal Capsule

- **Objective:** Hide the student dashboard Current Grade card until the student has submitted at least once for the selected lab; always show Total Submissions and Latest Submission for that lab.
- **Product authority:** This plan owns the stats row on the student main dashboard (`StudentUI` stats cards). Challenge sidebar scores, history page stats, lecturer views, and backend stats API semantics are not active scope.
- **Open blockers:** None — ready for implementation.

## Product Contract

### Summary

The student dashboard stats row will show only Total Submissions and Latest Submission when the selected lab has no submissions yet. After the first submission for that lab, the Current Grade card appears and the row expands to three equal columns. Empty values continue to use the existing `--/--` placeholders inside visible cards.

### Problem Frame

Students opening a lab they have not submitted yet see a prominent green Current Grade card displaying `--/--`. That card implies a grade exists or is pending when the student has not uploaded anything for the lab. The attempts and latest-submission cards already communicate pre-submission state; the grade card adds noise and misleads about progress.

### Key Decisions

- **Hide the entire Current Grade card when `totalSubmissions` is absent or zero for the selected lab** — chosen over showing the card with `--/--`: avoids a false grade affordance before any upload. Governs R1, R2.
- **Any lab submission attempt counts as submitted** (`totalSubmissions > 0`) — chosen over requiring a non-null score: aligns with "submitted their project" even when grading yields no numeric score yet. Governs R1, R3.
- **Two equal columns before first submission; three equal columns after** (session-settled: user-directed — Option A from layout probe: two 50/50 cards pre-submission, three-column row once grade appears). Governs R4.
- **Frontend conditional render only** — no backend or API contract change; existing `GET /api/labs/{labId}/stats` already returns null stats when no submissions.

### Actors

- A1. **Student** — selects a lab on the main dashboard, uploads project files, and returns on later sessions to view lab-scoped stats.

### Requirements

- R1. When the selected lab has no submissions for the student (`totalSubmissions` is null or zero), the Current Grade card is not rendered.
- R2. Total Submissions and Latest Submission cards remain visible for the selected lab regardless of submission state.
- R3. When `totalSubmissions > 0`, the Current Grade card is rendered. If `currentGrade` is null, the card shows the existing `--/--` placeholder inside the card.
- R4. The stats row uses a two-column grid when the grade card is hidden and a three-column grid when it is shown (large breakpoints; existing single-column stack on small screens may remain).
- R5. Lab switching updates visibility from fetched stats for the newly selected lab — no stale grade card from a prior lab.
- R6. After a successful upload in the current session, stats update immediately and the grade card appears when `totalSubmissions` from the upload response is greater than zero.

### Key Flows

- F1. **No submission yet**
  - **Trigger:** Student selects a lab with zero prior submissions.
  - **Actors:** A1
  - **Steps:** Dashboard loads stats from API; only Total Submissions and Latest Submission cards render with `--/--`; row lays out as two equal columns.
  - **Covered by:** R1, R2, R4, R5

- F2. **Returning student with submissions**
  - **Trigger:** Student selects a lab they submitted before (new session).
  - **Actors:** A1
  - **Steps:** Stats API returns submission count and grade; all three cards render; grade shows numeric score or `--/--` inside the card.
  - **Covered by:** R2, R3, R4, R5

- F3. **First upload in session**
  - **Trigger:** Student uploads project files for the selected lab.
  - **Actors:** A1
  - **Steps:** Upload response updates stats state; grade card appears; row expands to three columns; grade value reflects upload score when present.
  - **Covered by:** R3, R4, R6

### Acceptance Examples

- AE1. Lab with zero submissions → two cards visible (Total Submissions, Latest Submission), no green Current Grade card.
- AE2. Lab with six submissions and score 100 → three cards visible; Current Grade shows `100 / 100`.
- AE3. Switch from a submitted lab to an unsubmitted lab → grade card hides for the unsubmitted lab.
- AE4. First upload for a lab in session → grade card appears without full page reload.

### Scope Boundaries

**In scope**

- Stats row conditional rendering and responsive grid in `frontend/src/components/student/StudentUI.jsx`.
- Props/state already supplied by `frontend/src/pages/StudentDashboard.jsx` (no new API fields required).

**Out of scope**

- Challenge sidebar score reveal rules (session-upload gating).
- Student history page stat cards.
- Lecturer dashboards.
- Backend `StatsService` or `StatsDTO` changes.
- Copy or label changes beyond visibility and layout.

### Success Criteria

- SC1. Students with no lab submissions never see an empty Current Grade card for that lab.
- SC2. Students with submissions always see Current Grade alongside attempts and latest submission.
- SC3. Pre-submission layout uses two equal columns; post-submission uses three equal columns on large screens.

### Outstanding Questions

None — trigger (`totalSubmissions > 0`) and layout (Option A) confirmed in brainstorm.

### Sources / Research

- `frontend/src/components/student/StudentUI.jsx` — stats row renders three cards unconditionally; `hasValue` helper at line 74; grid uses `lg:grid-cols-3`.
- `frontend/src/pages/StudentDashboard.jsx` — `fetchStats` loads `GET /api/labs/{labId}/stats`; `handleUploadComplete` updates stats from upload response.
- `backend/src/main/java/com/eiu/capstone/backend/service/StatsService.java` — returns all-null stats when `submissionCount == 0`; `totalSubmissions` null when zero.
- `frontend/src/components/student/AGENTS.md` — student component ownership.

---

## Planning Contract

### Key Technical Decisions

- **KTD1. Derive visibility in `StudentUI` from `stats.totalSubmissions`** — `showGradeCard = hasValue(stats.totalSubmissions) && stats.totalSubmissions > 0`. No new props from `StudentDashboard`. Governs U1.
- **KTD2. Dynamic Tailwind grid classes on the stats row** — `grid-cols-1` base; `lg:grid-cols-2` when grade hidden, `lg:grid-cols-3` when shown. Matches brainstorm Option A. Governs U1.
- **KTD3. No backend changes** — `StatsService` already returns null `totalSubmissions` for zero attempts; upload response already supplies updated counts. Governs scope.
- **KTD4. Keep in-card `--/--` for null `currentGrade` when card is visible** — only the card container is conditional, not the inner placeholder logic. Governs U1.

### Technical Design

Add a derived flag near the stats row in `StudentUI`:

```javascript
const hasLabSubmissions =
  hasValue(stats.totalSubmissions) && Number(stats.totalSubmissions) > 0;
```

Stats row container:

```jsx
<div className={`grid grid-cols-1 gap-4 mb-6 ${hasLabSubmissions ? 'lg:grid-cols-3' : 'lg:grid-cols-2'}`}>
  {hasLabSubmissions && ( /* Current Grade card — existing markup */ )}
  {/* Total Submissions + Latest Submission — always rendered */}
</div>
```

`StudentDashboard` state updates on lab change (`fetchStats`) and upload (`handleUploadComplete`) already drive `stats.totalSubmissions`; no parent changes required.

### Assumptions

- `totalSubmissions` from API and upload response is a reliable proxy for "student submitted for this lab" (matches backend `lab_submission` row count).
- Mobile single-column stack is unchanged; only large-breakpoint column count toggles.

### Sequencing

U1 only — single frontend unit.

---

## Implementation Units

### U1. Conditional grade card and responsive stats grid

**Covers:** R1–R6, F1–F3, AE1–AE4

**Files:**
- `frontend/src/components/student/StudentUI.jsx`
- `frontend/src/components/student/AGENTS.md` (note stats row visibility rule)

**Work:**
- Add `hasLabSubmissions` derived from `stats.totalSubmissions`.
- Wrap the green Current Grade card in `{hasLabSubmissions && (...)}`.
- Switch stats row grid from fixed `lg:grid-cols-3` to conditional `lg:grid-cols-2` / `lg:grid-cols-3`.
- Leave Total Submissions and Latest Submission cards and their `--/--` fallbacks unchanged.

**Test scenarios (manual — no frontend automated test suite today):**
- Unsubmitted lab: two cards, no green grade card, two-column layout on wide viewport.
- Submitted lab (return visit): three cards, grade shows numeric value or `--/--` inside card.
- Lab switch unsubmitted → submitted: grade card appears after stats fetch.
- First upload in session: grade card appears without reload when upload response sets `totalSubmissions > 0`.

---

## Verification Contract

| Check | Command / action |
|---|---|
| Frontend build | `npm run build` from `frontend/` |
| Manual — no submissions | Log in as student, select lab with zero attempts; confirm two stats cards only |
| Manual — with submissions | Select lab with prior uploads; confirm three cards and correct grade |
| Manual — lab switch | Toggle between unsubmitted and submitted labs; visibility follows each lab |
| Manual — first upload | Upload to fresh lab; grade card appears after grading completes |

---

## Definition of Done

- [ ] Current Grade card hidden when `totalSubmissions` is null or zero for selected lab (R1)
- [ ] Total Submissions and Latest Submission always visible (R2)
- [ ] Grade card shows when `totalSubmissions > 0`, including `--/--` inner state when score null (R3)
- [ ] Two-column / three-column grid on large screens per submission state (R4)
- [ ] Lab switch and post-upload stats refresh behave correctly (R5, R6)
- [ ] `npm run build` passes
- [ ] `frontend/src/components/student/AGENTS.md` updated with stats visibility contract
