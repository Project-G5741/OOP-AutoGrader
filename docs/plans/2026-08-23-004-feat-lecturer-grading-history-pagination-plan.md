---
title: "Lecturer grading submission history pagination - Plan"
date: 2026-08-23
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Lecturer grading submission history pagination - Plan

## Goal Capsule

**Objective:** On the lecturer Grading tab submission history panel, show 10 submissions per page with clear page navigation, while keeping existing lab filter and column sort behavior over the selected student's full in-scope list.

**Product authority:** Session request (2026-08-23): paginate lecturer-grading Submission History at 10 rows per page. Fills the deferred history-panel pagination called out in `docs/plans/2026-08-08-004-feat-grading-row-submission-history-plan.md` and `docs/plans/2026-08-19-001-feat-student-history-pagination-plan.md`.

**Open blockers:** None.

**Stop conditions:** Do not change `GET /api/analytics/student/{studentId}`, grade-overview matrix pagination, or student `my-history`. Do not add a page-size control.

---

## Product Contract

### Summary

Paginate the lecturer Grading tab inline **Submission history** panel at a fixed 10 rows per page. Lab filter and column sort still apply to the selected student's full matching list; only the current page of that ordered list is shown. Prev/next (or equivalent) controls show the current page.

### Problem Frame

The Grading tab history panel currently renders every matching submission for the selected student after client filter/sort. Students with many attempts across labs produce long tables that are hard to scan. Student Submission History already pages at 10; lecturer history was deferred and now needs the same bounded view.

### Actors

- A1. **Lecturer** — signed-in user with `LECTURER` role on `/lecturer-grading`, viewing a selected student's submission history panel.

### Requirements

- R1. The submission history panel shows at most **10** submissions on one page.
- R2. Pagination applies to the **already filtered and sorted** history for the selected student (lab filter and column sort first; then page).
- R3. The UI shows which page the lecturer is on and provides previous/next (or equivalent) navigation between pages.
- R4. Changing the lab filter or column sort resets to the first page.
- R5. Selecting a different student resets history pagination to the first page for that student.
- R6. When the filtered list has 10 or fewer rows, pagination controls are hidden or inactive; the empty and error states stay as they are today.
- R7. Page size is fixed at 10 in v1 — no lecturer-facing page-size control.

### Key Flows

- F1. Lecturer selects a student with more than 10 matching submissions → panel shows the first 10 of the filtered/sorted list and page controls for the remaining pages.
- F2. Lecturer clicks next → panel shows the next page of the same filtered/sorted list without clearing selection, filter, or sort.
- F3. Lecturer changes lab filter or sort → list reorders/rescopes, page resets to 1, and the first 10 matching rows show.
- F4. Lecturer selects another student → history reloads for that student starting on page 1.

### Acceptance Examples

- AE1. Selected student has 25 submissions, All Labs, default newest-first: page 1 shows 10 rows; controls indicate more pages exist.
- AE2. Same student on the last page: remaining rows (≤10) show; next is disabled or unavailable.
- AE3. Lab filter narrows to 3 submissions: all 3 show on one page; pagination is hidden or inactive.
- AE4. After sorting on page 2, the panel returns to page 1 of the newly ordered full filtered list.
- AE5. Switching students from a student on page 3 to another student starts the new history on page 1.

### Scope Boundaries

**In scope:** Pagination UX and paging behavior for `GradeOverviewSubmissionHistory` on the lecturer Grading tab; reset rules for filter, sort, and student selection; AGENTS.md contract updates for the panel.

**Deferred for later:** Server-side paging of the analytics student report, configurable page size, infinite scroll, export from the history panel, Attempts column, date-range filtering.

**Outside this product's identity:** Student Submission History (already paginated), grade-overview student-matrix pagination, roster pagination, scoring rules.

### Key Decisions

- KD1. **Fixed page size 10** — session-settled: user-directed — chosen over configurable size: match student history and the stated "10 submissions for 1 page" request.
  Governs R1, R7.

- KD2. **Page after filter and sort** — chosen so lecturers browse the same ordered full list they already get, in chunks of 10.
  Governs R2, R4.

- KD3. **Reset on filter, sort, or student change** — standard pagination continuity; avoids empty mid-pages after scope changes.
  Governs R4, R5.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns **lecturer Grading tab submission history pagination** only.

- Completes deferred work from `docs/plans/2026-08-08-004-feat-grading-row-submission-history-plan.md` (history panel shipped without pagination).
- Mirrors the student product rule from `docs/plans/2026-08-19-001-feat-student-history-pagination-plan.md` (10 per page) — **Shares** page-size expectation; **Can proceed independently of** student history APIs.
- Does not reopen grade-matrix layout work in `docs/plans/2026-08-23-002-feat-lecturer-grading-table-layout-plan.md`.

**Product Contract preservation:** Product Contract unchanged.

---

## Planning Contract

### Assumptions

- A1. History stays loaded via existing `GET /api/analytics/student/{studentId}` (`submissionHistory`); pagination slices the client-filtered/sorted list. Server-side paging of that report remains deferred (Product Contract).

### Key Technical Decisions

- KTD1. **Client-side offset pagination (page size 10)** — slice `filteredGradeStudentHistoryRows` after filter/sort; do not change the analytics endpoint in v1. Chosen over server-side analytics paging because the Product Contract defers server paging and the panel already loads full student history (see A1).
  Instantiates R1, R2, R7 / KD1, KD2.

- KTD2. **Reuse existing prev/next footer pattern** — match `SubmissionTable` / `GradeOverviewTable` / `StudentHistoryPage` (`Page N of M`, Previous/Next, show when `totalPages > 1` or `total > size`).
  Instantiates R3, R6.

- KTD3. **Page state owned by `LecturerDashboard`** — zero-based `historyPage`; reset to `0` when student selection, lab filter, or history sort changes; clamp if the filtered total shrinks below the current page.
  Instantiates R4, R5.

### Technical Design

`LecturerDashboard` keeps producing the full filtered/sorted row array. Derive `pageRows = rows.slice(page * 10, page * 10 + 10)` and pass `pageRows` plus pagination metadata into `GradeOverviewSubmissionHistory`. The panel renders the page rows and the shared footer; empty/loading/error paths stay unchanged.

### Sequence

1. U1 — Wire page state, slice, and reset rules in `LecturerDashboard`.
2. U2 — Render pagination footer in `GradeOverviewSubmissionHistory` and update AGENTS.md.

---

## Implementation Units

### U1. History page state and slice in LecturerDashboard

**Goal:** Track history page index; expose only the current page of filtered/sorted rows; reset and clamp per R4–R5.

**Requirements:** R1, R2, R4, R5, R7; KTD1, KTD3.

**Files:** `frontend/src/pages/LecturerDashboard.jsx`

**Approach:** Add `historyPage` state (default `0`). When `selectedGradeStudent` changes, lab filter changes, or `handleHistorySort` runs, set `historyPage` to `0`. From `filteredGradeStudentHistoryRows`, compute `total`, `totalPages = ceil(total / 10)`, clamp `historyPage` into range, and `pageRows = slice(page * 10, page * 10 + 10)`. Pass `pageRows`, pagination object `{ page, size: 10, total, totalPages }`, and `onPageChange` into the history panel.

**Test scenarios:**
- Happy: 25 filtered rows → page 0 has 10; page 2 has 5.
- Edge: filter to 3 rows while on page 2 → page resets or clamps to 0; all 3 visible.
- Edge: sort while on page 1 → page becomes 0.
- Integration: select student B after paging student A → history starts at page 0.

### U2. Submission history panel pagination UI

**Goal:** Show prev/next controls and only the rows for the current page.

**Requirements:** R3, R6; KTD2.

**Files:** `frontend/src/components/lecturer/GradeOverviewSubmissionHistory.jsx`, `frontend/src/components/lecturer/AGENTS.md`

**Approach:** Accept `pagination` and `onPageChange` props. Render table from `rows` (already the current page). Footer matches `SubmissionTable` markup/classes. Hide footer when not needed (`totalPages > 1` or `total > size`). Document: history panel pages client-side at 10 after filter/sort; matrix pagination unchanged.

**Test scenarios:**
- Happy: page label and Next/Previous enable/disable correctly.
- Edge: ≤10 rows → no pagination footer (or inactive).
- Error/empty: loading/error/empty states unchanged; no footer on empty.

**Test expectation:** none for automated tests — frontend has no test suite; cover via Verification Contract manual checks and `npm run build`.

---

## Verification Contract

- `npm run build` in `frontend/` succeeds.
- Manual on `/lecturer-grading`: select a student with 11+ submissions → 10 rows + pagination; Next shows remaining; lab filter / sort / other student resets to page 1; ≤10 matching rows hide pagination.

## Definition of Done

- History panel shows at most 10 rows per page with working prev/next.
- Filter, sort, and student change reset to the first page.
- Lecturer AGENTS.md documents the client-side 10-row history pagination.
- No analytics API or grade-matrix pagination changes.
