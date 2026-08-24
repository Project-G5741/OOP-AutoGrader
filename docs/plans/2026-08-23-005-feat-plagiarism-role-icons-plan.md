---
title: Plagiarism Role Icons - Plan
type: feat
date: 2026-08-23
topic: plagiarism-role-icons
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
---

# Plagiarism Role Icons - Plan

## Goal Capsule

- **Objective:** On lecturer plagiarism marks, distinguish the **original** (earlier submission in a flagged match) from the **plagiarizer** (later) with different icons.
- **Product authority:** Product Contract below. Detection rules stay unchanged.
- **Open blockers:** None (multi-role and lab-level defaults locked after confirmation timeout).

---

## Product Contract

### Summary

Replace the single shared warning triangle on **student** plagiarism marks with role-specific icons derived from submission timestamps. Lab-level “this lab has plagiarism” marks stay a generic presence indicator.

### Problem Frame

Today every flagged student looks the same. Lecturers cannot tell who submitted first (source) versus who submitted later (likely copier) without manually comparing times.

### Key Decisions

- **Earlier first-submit in lab = victim, later = plagiarizer** — Governs R1. (session-settled: user-directed — compare earliest lab submission per student, not the matched attempt, so a victim's re-upload does not invert roles.)
- **Role icons on every student plagiarism mark** — Governs R2. (session-settled: user-directed — chosen over roster-only / roster+grading.)
- **Lab-level marks stay generic** — Governs R3. (inferred after timeout — a lab is not a person; role icons only on student surfaces.)
- **Roles are mutually exclusive** — Governs R4, R5. (session-settled: user-directed — never show both icons; conflicting matches prefer plagiarizer.)
- **Yellow warning = victim, red warning = plagiarizer** — Governs R6. (session-settled: user-directed — both parties at fault for sharing; colors distinguish roles.)

### Actors

- A1. Lecturer viewing flagged plagiarism marks on dashboard / grading

### Key Flows

- F1. Scan flagged students with role
  - **Trigger:** Lecturer opens a surface that already shows plagiarism marks.
  - **Actors:** A1
  - **Steps:** Student marks show either original (green shield) or plagiarizer (yellow warning); lab names keep a generic yellow warning.
  - **Outcome:** Lecturer can tell source vs copier at a glance.
  - **Covered by:** R1–R6

### Requirements

- R1. For each flagged match, compare each student's **earliest** `submittedAt` in that lab: earlier first-submit = **victim** (`ORIGINAL`); later = **plagiarizer**. A later re-upload by the victim must not flip their role.
- R2. Student roster rows, challenge-tab rows, and grade-overview student/lab cells use role icons instead of a single shared triangle when a role is known.
- R3. Lab-level marks (lab name / selected lab) remain a generic “lab has plagiarism” indicator (yellow warning badge).
- R4. Each student shows exactly one role icon. If they would be original in one match and plagiarizer in another, show **plagiarizer**.
- R5. If matched submissions have equal `submittedAt`, assign roles with a stable submission-id tie-break (never both icons).
- R6. Victim mark is a yellow-background warning badge; plagiarizer mark is a red-background warning badge; lab presence uses yellow. Icons have distinct accessible names.

### Scope Boundaries

**In scope**

- Role computation from flagged matches + submission timestamps
- API fields needed by lecturer UI
- Role icons on student plagiarism marks; generic mark on lab-level

**Out of scope**

- Changing plagiarism detection / match rules
- Student-facing plagiarism UI
- New match-detail investigation panel
- Changing SUMMARY plagiarism rate math

### Acceptance Examples

- AE1. A submits at 10:00, B at 11:00, flagged match → A shows original icon; B shows plagiarizer icon.
- AE2. C is original vs D and plagiarizer vs E → C shows plagiarizer only.
- AE3. Lab with any flagged match → lab name still shows generic yellow warning (not a person role).
- AE4. Equal timestamps on a flagged pair → one role each via submission-id tie-break (never both icons).

---

## Planning Contract

### Key Technical Decisions

- KTD1. Compute roles in `PlagiarismService` by loading both `LabSubmission`s for each flagged match and comparing `submittedAt`. Aggregate per student (and per lab for flags payload) as `ORIGINAL` | `PLAGIARIZER` only (mutually exclusive; conflicting matches prefer PLAGIARIZER; equal times tie-break by submission id).
- KTD2. Extend roster/challenge DTOs with `plagiarismRole` (nullable string) and set `plagiarismFlagged` from student involvement (role non-null), not only when the displayed `submissionId` is a match endpoint — so re-uploads still show role.
- KTD3. Extend `PlagiarismFlagsDTO` with `rolesByStudentAndLab` for grade-overview cells; keep `flaggedLabIds` for generic lab marks.
- KTD4. Frontend: `PlagiarismDangerMark` renders original (green shield) or plagiarizer/lab (yellow warning badge); never both.

### Assumptions

- Match involvement already loads submissions with timestamps via `findAllWithUserByIdIn`.
- Overlap % display beside marks stays as-is.

### Open Questions

None blocking.

### Implementation Units

### U1. Backend role computation + API fields

**Goal:** Expose plagiarism role for lecturer roster, challenge rows, and flags payload.

**Requirements:** R1, R4, R5, R7

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/plagiarism/PlagiarismService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/DTO/plagiarism/PlagiarismFlagsDTO.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/analytics/dto/SubmissionSummaryDTO.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/analytics/dto/ChallengeStudentRowDTO.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/analytics/service/LecturerAnalyticsService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md`

**Approach:**
- Add role aggregation helper: for each flagged match, compare timestamps; merge roles per student (and per student+lab).
- `lecturerFlags()` adds `rolesByStudentAndLab`.
- `withPlagiarismFlags` / `withChallengePlagiarismFlags` resolve role by `studentId` for the lab.

**Test scenarios:**
- Happy: earlier/later pair → ORIGINAL / PLAGIARIZER.
- Edge: equal times → stable id tie-break (still ORIGINAL vs PLAGIARIZER).
- Edge: student in two matches with opposite roles → PLAGIARIZER.

**Verification:** `mvn test` / compile from `backend/`.

### U2. Frontend role icons

**Goal:** Render distinct icons for original vs plagiarizer on student marks; keep lab marks generic.

**Requirements:** R2, R3, R6

**Files:**
- Modify: `frontend/src/components/lecturer/PlagiarismDangerMark.jsx`
- Modify: `frontend/src/components/lecturer/SubmissionTable.jsx`
- Modify: `frontend/src/components/lecturer/GradeOverviewTable.jsx`
- Modify: `frontend/src/pages/LecturerDashboard.jsx`
- Modify: `frontend/src/components/lecturer/AGENTS.md`

**Approach:**
- Role prop: `null`/`undefined` + `show` → generic lab yellow warning; `ORIGINAL` / `PLAGIARIZER` → single role icon.
- Wire roster `plagiarismRole`; grade cells from `rolesByStudentAndLab`.
- Lab name call sites unchanged (generic).

**Test scenarios:**
- Happy: role ORIGINAL vs PLAGIARIZER render different icons/labels.
- Lab header: yellow warning badge.

**Verification:** `npm run build` in `frontend/`.

---

## Verification Contract

- Backend: `mvn test` from `backend/` (or compile + existing plagiarism tests).
- Frontend: `npm run build` from `frontend/`.
- Manual: flagged pair with known times → earlier student original icon, later plagiarizer; lab name still generic.

## Definition of Done

- [x] U1 and U2 complete
- [x] Student marks distinguish original vs plagiarizer
- [x] Lab-level marks remain generic
- [x] Docs updated
- [x] Build / tests above pass
