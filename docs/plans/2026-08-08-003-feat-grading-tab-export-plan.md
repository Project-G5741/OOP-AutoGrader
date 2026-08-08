---
title: "feat: Lecturer Grading tab export (Excel, PDF, SVG)"
date: 2026-08-08
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
---

# feat: Lecturer Grading tab export (Excel, PDF, SVG) - Plan

## Goal Capsule

**Objective:** Let lecturers export the full cross-lab grade matrix from the **Grading** nav tab as Excel, PDF, or SVG — matching the existing Dashboard export UX and including every student, not just the current page.

**Product authority:** Session brainstorm decisions (no requirements-only artifact was written). This plan bootstraps the Product Contract from that dialogue.

**Stop conditions:** Do not add backend export endpoints, per-lab filters, or richer PDF/SVG table layout beyond what `exportRoster.js` already provides. Do not change grade-overview scoring logic.

---

## Product Contract

### Actors

- A1. **Lecturer** — signed-in user with `LECTURER` role viewing the Grading tab (`activeNav === 'grading'`).

### Requirements

- R1. The Grading tab header exposes an **Export** control alongside the existing Refresh control.
- R2. Clicking Export opens a picker with exactly three formats: **Excel**, **PDF**, and **SVG** — same labels and order as Dashboard export.
- R3. Export includes **all students** in the grade matrix, not only the rows visible on the current paginated table view.
- R4. Exported columns match the on-screen table: **Student**, **IRN**, **Total Score**, then one column per lab (lab name as header).
- R5. Score and text formatting in the export match table display rules: percentages via `formatPercent` (`--` for null/undefined), names/IRN via `formatText`.
- R6. When there are no students to export, Export is **disabled** (no download of an empty file).
- R7. Export uses the lecturer's existing session token (`Authorization: Bearer` from `sessionStorage`).

### Key Flows

- F1. **Export all grades** — Lecturer opens Grading tab → clicks Export → selects format → browser downloads a file named for the grade overview.
- F2. **Empty roster** — Grading tab loads with zero students → Export button is disabled.

### Acceptance Examples

- AE1. Lecturer with 12 students (paginated 5 per page) exports Excel → file contains 12 data rows plus headers for Student, IRN, Total Score, and every lab column shown in the table.
- AE2. Lecturer exports PDF from Grading tab → file downloads with title "Cross-lab Grade Overview" (or equivalent) and one line per student using the same values as the table.
- AE3. Lecturer exports SVG → `.svg` file downloads with the same row content as PDF.
- AE4. No students in grade overview → Export control is disabled; clicking is not possible.
- AE5. Student with no submission for a lab → that lab column shows `--` in the export, matching the table.

### Scope Boundaries

**In scope:** `ExportMenu` on Grading tab, client-side pagination to collect all grade-overview rows, shared export helpers in `exportRoster.js`, `LecturerDashboard.jsx` wiring, AGENTS.md updates.

**Deferred for later:** Backend `/grade-overview/export` endpoint, export filters (by lab/term), formatted PDF tables, loading spinner on Export during fetch.

**Outside this product's identity:** Student-facing export, challenge-drawer export changes, grade calculation changes.

### Key Decisions

- KD1. **All students, not current page** — session-settled: chosen over current-page-only export because lecturers need a complete roster for records.
  Governs R3.

- KD2. **Reuse existing frontend export stack** — `ExportMenu` + `exportRoster.js` `exportDataset`, chosen over a new backend endpoint for consistency and zero API surface change.
  Governs R1, R2.

- KD3. **Disable Export when empty** — chosen over downloading an empty file for clearer UX.
  Governs R6.

---

## Planning Contract

### Summary

Wire `ExportMenu` into the Grading `DashboardSection` actions in `LecturerDashboard.jsx`. Add a `fetchAllGradeOverview` helper that paginates `GET /api/lecturer/grade-overview` with `size=100` (backend max) until all pages are collected, maps rows to flat objects keyed by column headers, and calls a new `exportGradeOverview` wrapper in `exportRoster.js` that delegates to `exportDataset`.

**Product Contract preservation:** Unchanged — bootstrapped from session brainstorm; no separate requirements artifact to diff.

### Key Technical Decisions

- KTD1. **Client-side full fetch via paginated grade-overview** — mirror Dashboard's `exportOverview` pattern (`fetchAllLabSubmissions` + `exportRosterRows`). Backend caps `size` at 100 (`LecturerAnalyticsService.getGradeOverview`); export loop uses `size=100` and increments `page` until `page >= totalPages - 1`.
- KTD2. **Row builder lives in `exportRoster.js`** — export `buildGradeOverviewExportRows({ labs, students })` returning an array of plain objects; keeps `LecturerDashboard.jsx` as orchestration-only per lecturer component conventions.
- KTD3. **Lab column headers use `lab.labName`** — same source as `GradeOverviewTable` headers (`formatText(lab.labName)`); scores aligned by lab index in `student.labScores` array (same contract as table rendering).
- KTD4. **File base name `grade_overview_export`** — consistent, descriptive; formats append `.xlsx`, `.pdf`, `.svg` via existing helpers.
- KTD5. **Export disabled when `gradeOverviewPagination.total === 0`** or while `loadingGradeOverview` is true — pass `disabled` to `ExportMenu`; no toast/error needed for empty state.
- KTD6. **No new npm dependencies** — `xlsx` and `jspdf` already in `frontend/package.json`; dynamic imports unchanged.

### Assumptions

- Grade-overview API auth and CORS behave the same for export pagination as for the table fetch (no dedicated export permission).
- Cohort size stays within reasonable client-side fetch bounds (hundreds of students × 100/page = few requests).

### Risks & Dependencies

| Risk | Mitigation |
|---|---|
| Large cohort causes slow export | Acceptable for current scale; use `size=100` to minimize round trips |
| Export during active pagination shows stale total | Re-fetch all pages on export click rather than reusing cached `gradeOverview.content` only |
| Lab column order drift between fetch pages | Lab metadata returned on every page response; use labs from first successful page |

---

## Implementation Units

### U1. Grade overview export helpers

**Goal:** Add reusable row-building and export entry point for the cross-lab grade matrix.

**Requirements:** R4, R5

**Dependencies:** None

**Files:**
- `frontend/src/components/lecturer/exportRoster.js`

**Approach:**
1. Import or accept `formatPercent` and `formatText` from `utils/formatters` (or accept pre-formatted values from caller — prefer importing formatters in the helper for single source of truth).
2. Add `buildGradeOverviewExportRows({ labs, students })` that returns an array of row objects:
   - Fixed keys: `Student`, `IRN`, `Total Score`
   - Dynamic keys: one per lab using `formatText(lab.labName)` as the object key
   - Map each `student.labScores[index]` with `formatPercent`
3. Add `exportGradeOverview(format, { labs, students, fileBase })` that:
   - Calls `buildGradeOverviewExportRows`
   - Returns early if rows array is empty
   - Calls `exportDataset` with title `Cross-lab Grade Overview` and `fileBase` defaulting to `grade_overview_export`

**Patterns to follow:** Existing `exportRosterRows` and `exportChallengeBreakdown` in the same file.

**Test scenarios:**
- `buildGradeOverviewExportRows` with two labs and one student produces object with five keys and formatted percent values
- `buildGradeOverviewExportRows` with empty `students` returns `[]`
- `exportGradeOverview` with empty rows does not trigger download (early return)

**Test expectation:** none — pure functions with no test harness in frontend; covered by manual verification in U2.

**Verification:** Exported helper functions are importable; `npm run build` succeeds.

---

### U2. Wire Export on Grading tab

**Goal:** Lecturer can export all students from the Grading tab via `ExportMenu`.

**Requirements:** R1, R2, R3, R6, R7

**Dependencies:** U1

**Files:**
- `frontend/src/pages/LecturerDashboard.jsx`

**Approach:**
1. Import `exportGradeOverview` from `exportRoster.js` (alongside existing `exportRosterRows` import).
2. Add `fetchAllGradeOverview` async function:
   - Use `EXPORT_PAGE_SIZE = 100`
   - Loop `page` from 0 while `page < totalPages`
   - `GET /api/lecturer/grade-overview?page={page}&size=100` with `authHeaders()`
   - Accumulate `content` arrays; capture `labs` from first response
   - On non-OK response, return `{ labs: [], students: [] }`
3. Add `handleExportGradeOverview(format)`:
   - Call `fetchAllGradeOverview()`
   - If no students, return without download
   - Call `exportGradeOverview(format, { labs, students })`
4. In the Grading `DashboardSection` `actions` prop, render `ExportMenu` next to Refresh:
   - `onExport={handleExportGradeOverview}`
   - `disabled={loadingGradeOverview || gradeOverviewPagination.total === 0}`
5. Wrap actions in a flex container (`flex items-center gap-2`) matching Dashboard export layout.

**Patterns to follow:**
- `exportOverview` + `fetchAllLabSubmissions` in the same file (lines ~55–61, ~400–416)
- `ExportMenu` usage in Dashboard overview section (~612)

**Test scenarios:**
- Covers AE1. Export with more students than one table page produces full row count in downloaded file
- Covers AE4. With `total === 0`, Export button has `disabled` attribute
- Covers AE5. Student missing lab score shows `--` in export column
- Error path: grade-overview API returns 401/500 during export fetch → no file download, no uncaught rejection (optional: console error only)

**Test expectation:** none — no frontend test framework; manual verification required.

**Verification:** Manual flow per Acceptance Examples; `npm run build` passes.

---

### U3. Update lecturer component docs

**Goal:** DOX reflects Grading tab export behavior.

**Requirements:** (documentation traceability)

**Dependencies:** U2

**Files:**
- `frontend/src/components/lecturer/AGENTS.md`
- `frontend/src/pages/AGENTS.md`

**Approach:**
1. In `lecturer/AGENTS.md`, add bullet under Data source: Grading tab export uses `ExportMenu` → `exportGradeOverview` in `exportRoster.js` (Excel, PDF, SVG; all students via paginated grade-overview fetch).
2. Update Composition diagram to show `ExportMenu` on Grading section.
3. In `pages/AGENTS.md`, note Export on `grading` section in the lecturer table row.

**Test expectation:** none — docs only.

**Verification:** AGENTS.md accurately describes new export path.

---

## Verification Contract

| Gate | Command / action | Expect |
|---|---|---|
| Build | `npm run build` from `frontend/` | Exit 0 |
| Manual — full export | Log in as lecturer → Grading tab → Export → Excel | `.xlsx` with all students and lab columns |
| Manual — formats | Repeat for PDF and SVG | Files download with consistent row content |
| Manual — empty | Grade overview with 0 students (or mock empty response) | Export disabled |
| Manual — parity | Compare one student's exported values to table row | Percentages and text match |

No automated test suite exists in the frontend tier.

---

## Definition of Done

**Global:**
- [ ] Export button visible on Grading tab with Excel / PDF / SVG picker
- [ ] Export includes all students across paginated API
- [ ] Export disabled when no students
- [ ] `npm run build` succeeds
- [ ] AGENTS.md updated on affected paths

**Per unit:**
- U1: `buildGradeOverviewExportRows` and `exportGradeOverview` exported from `exportRoster.js`
- U2: `ExportMenu` wired in Grading section; manual AE1–AE5 pass
- U3: Lecturer and pages AGENTS.md mention Grading export

---

## Appendix

### Sources & Research

- `frontend/src/components/lecturer/ExportMenu.jsx` — existing format picker (excel, pdf, svg)
- `frontend/src/components/lecturer/exportRoster.js` — `exportDataset`, `exportRosterRows`
- `frontend/src/pages/LecturerDashboard.jsx` — `exportOverview`, `fetchGradeOverview`, Grading section (~676–704)
- `frontend/src/components/lecturer/GradeOverviewTable.jsx` — column shape reference
- `backend/src/main/java/com/eiu/capstone/backend/analytics/service/LecturerAnalyticsService.java` — `size` capped at 100
- `frontend/AGENTS.md` — data fetching stays in `LecturerDashboard.jsx`

### Alternatives Considered

- **Backend export endpoint** — rejected for this iteration; higher carrying cost, diverges from client-side export pattern used on Dashboard roster.
- **Export current page only** — rejected in brainstorm; does not meet lecturer need for full class records.
