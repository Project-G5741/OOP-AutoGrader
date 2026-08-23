---
title: Lecturer Grading Table Layout - Plan
type: feat
date: 2026-08-23
topic: lecturer-grading-table-layout
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Lecturer Grading Table Layout - Plan

## Goal Capsule

- **Objective:** Redesign the lecturer Grading grade matrix so Student / IRN / Total Score stay always visible in a left panel while lab scores scroll in a right panel, show overlap % next to the plagiarism mark when flagged, and keep score + mark on one aligned line.
- **Product authority:** Product Contract below. Per-lab plagiarism investigation UIs and plagiarism detection rules stay as they are unless cited.
- **Open blockers:** None.
- **Product Contract preservation:** Product Contract unchanged.

---

## Product Contract

### Summary

Replace the single wide grading matrix with two overview-style panels: identity on the left (Student, IRN, Total Score) and labs on the right. Lab cells center the score first, then append a display-only plagiarism mark and overlap percentage on the same line when that student-lab pair is already flagged.

### Problem Frame

With many labs, lecturers lose Student / IRN / Total Score while scrolling the grade matrix horizontally. Plagiarism is only a boolean triangle today, so high file overlap is hard to judge at a glance, and the mark sometimes wraps off the score line—making dense rows harder to scan.

### Key Decisions

- **Two separate panels (overview-style gutter, matching surfaces)** — Governs R1, R2, R3. (session-settled: user-directed — chosen over frozen columns in one table and over roster+detail cards: clearer “two sites,” matches Grading overview.)
- **No horizontal scroll on identity; dark scrollbar on labs only** — Governs R2, R4. (session-settled: user-directed — chosen over scrollbars on both panels / system white track.)
- **Score first (column-centered), plagiarism appended to the right** — Governs R5, R6. (session-settled: user-directed — chosen over centering the score+mark group as one unit.)
- **Display-only mark + overlap %** — Governs R7, R8. (session-settled: user-approved — chosen over click-to-open match details: keep deep review on existing per-lab views.)
- **Synced vertical scroll, equal row heights, shared pagination, keep sorting** — Governs R3, R9, R10.

### Actors

- A1. Lecturer viewing the Grading (grade overview) matrix

### Key Flows

- F1. Scan grades with many labs
  - **Trigger:** Lecturer opens Grading with enough labs that lab columns overflow the viewport.
  - **Actors:** A1
  - **Steps:** Identity panel stays fully visible; lecturer scrolls labs horizontally; vertical scroll keeps student rows aligned across both panels.
  - **Outcome:** Lecturer can read Student / IRN / Total Score while browsing any lab column.
  - **Covered by:** R1, R2, R3, R4

- F2. Spot high overlap on a lab cell
  - **Trigger:** A student-lab pair is already plagiarism-flagged.
  - **Actors:** A1
  - **Steps:** Lecturer sees score centered in the lab column; `▲` and overlap % appear to the right of that score on one line; no click required for this information.
  - **Outcome:** Lecturer judges severity from the % without leaving the matrix.
  - **Covered by:** R5, R6, R7, R8

### Requirements

**Layout**

- R1. The Grading matrix presents two separate panels with matching panel backgrounds and a soft gutter between them (no hard divider line inside one shared box).
- R2. The left panel always shows Student, IRN, and Total Score in full with no horizontal scrollbar; the right panel holds lab columns and is the only panel that scrolls horizontally when labs overflow.
- R3. Vertical scrolling of student rows stays synchronized between panels; row heights match so identity and lab cells for the same student stay aligned.
- R4. Horizontal scrollbar chrome on the labs panel matches the dark panel background (no high-contrast white track).

**Scores and plagiarism**

- R5. Total Score values are centered under their header and aligned down the column.
- R6. In each lab cell, the score (or `--`) is placed first as the column-centered component; when a plagiarism mark is shown, it and the overlap percentage sit to the right of that score on the same line without shifting the score off the shared center.
- R7. Overlap percentage appears only for student-lab pairs that are already plagiarism-flagged; unflagged cells show score (or `--`) alone.
- R8. The plagiarism mark and overlap % are display-only in this matrix (no new click-to-details interaction required).

**Existing behaviors retained**

- R9. Column sorting remains available on identity and lab headers as today.
- R10. Pagination remains a single shared control for the student list (not separate pagers per panel).

### Acceptance Examples

- AE1. Many labs overflow
  - **Covers:** R1, R2, R4
  - **Given:** Enough lab columns that they do not fit in the viewport
  - **When:** The lecturer views Grading
  - **Then:** Left panel shows all three identity columns with no horizontal scroll; right panel scrolls horizontally with dark scrollbar chrome

- AE2. Flagged lab cell alignment
  - **Covers:** R5, R6, R7
  - **Given:** Student A has score 71 and is flagged on Cake Shop with 94% overlap; Student B has score 40 and is not flagged
  - **When:** Both rows are visible in the Cake Shop column
  - **Then:** 71 and 40 share the same horizontal center; `▲ 94%` sits to the right of 71 on one line; B shows 40 only

- AE3. Vertical sync
  - **Covers:** R3, R10
  - **Given:** More students than fit vertically on one page
  - **When:** The lecturer scrolls either panel vertically or changes page
  - **Then:** Student rows stay paired across panels; one shared pagination control advances both

### Success Criteria

- Lecturers can keep Student / IRN / Total Score visible while scanning any lab column.
- Flagged cells show overlap % on the same line as the score without wrapping or mis-centering the score.
- Sorting and pagination still work without separate left/right controls.

### Scope Boundaries

**Deferred for later**

- Clicking the plagiarism mark to open match details (peer, which check fired) from the matrix
- Changing plagiarism detection rules or the existing flag threshold

**Outside this product's identity**

- Student-facing plagiarism notifications or UI
- Redesigning Grading overview (lab selector + per-lab tabs) beyond the grade matrix layout pattern it inspires

### Dependencies / Assumptions

- Overlap % is available from existing plagiarism similarity data when a pair is flagged; planning wires that value into the matrix without inventing a new detection threshold.
- Visual direction follows the locked sketch behavior (two panels, score-first alignment, dark labs scroll), not pixel-perfect styling from the probe HTML.

### Outstanding Questions

**Resolve Before Planning**

- None.

**Deferred to Planning**

- (Resolved in Planning Contract) Flags feed carries max hash similarity per student-lab; vertical sync via paired scroll handlers on page-sized tables (~10 rows).

### Sources / Research

- Current matrix: `frontend/src/components/lecturer/GradeOverviewTable.jsx` (single scrollable table; boolean `PlagiarismDangerMark`).
- Domain rule: `CONCEPTS.md` — Plagiarism check (file-hash Jaccard flags above 90%).
- Flags API: `PlagiarismService.lecturerFlags()` / `PlagiarismFlagsDTO` — lab/student IDs only today; `hashSimilarity` already on matches and `PlagiarismMatchDTO`.
- Global scrollbar theming already in `frontend/src/index.css` (thin thumb, transparent track); labs panel should set track to `bg-surface` so nested OS chrome does not flash white.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Extend `GET /api/lecturer/plagiarism/flags` with per student-lab overlap** — Add `Map<UUID, Map<UUID, BigDecimal>> overlapByStudentAndLab` (outer student, inner lab → max `hashSimilarity` among flagged matches involving that student for that lab). Prefer this over N×`GET /labs/{labId}/plagiarism` from the Grading tab. (session-settled product: display-only % when flagged — Governs R7.) When flagged only via git/metadata and hash similarity is 0, still emit `0` and let the UI show `▲` with `0%` or omit zero — prefer show integer percent from max hashSimilarity when > 0, else `▲` alone.
- KTD2. **Two sibling tables in `GradeOverviewTable`, not sticky columns** — Matches settled two-panel UX; shared row height (`h-10` / fixed padding); `overflow-x-hidden` on identity; `overflow-x-auto` on labs; gutter `gap-4`. Sync `scrollTop` between the two body scroll containers.
- KTD3. **Score-first cell layout** — Centered score anchor; plagiarism mark + % absolutely positioned to the right of the score so flagged/unflagged scores share one column centerline (R5, R6).
- KTD4. **Keep `PlagiarismDangerMark` for the icon** — Compose mark + optional percent text beside it; do not add click handlers (R8).

### Assumptions

- Page size stays 10; syncing two short scroll regions is enough without virtualization.
- Export (`exportGradeOverview`) stays tabular as today — no two-panel requirement for Excel/PDF/SVG.
- Existing global scrollbar CSS remains; labs panel may set `scrollbar-color` / webkit track to surface token for R4.

### Sequencing

1. U1 backend flags payload  
2. U2 `GradeOverviewTable` layout + cells  
3. U3 wire dashboard state  
4. U4 docs  

---

## Implementation Units

### U1. Flags API: overlap by student and lab

- **Goal:** Lecturers' flags response includes max file-hash similarity per flagged student-lab pair so the matrix can show %.
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/DTO/plagiarism/PlagiarismFlagsDTO.java`, `backend/src/main/java/com/eiu/capstone/backend/plagiarism/PlagiarismService.java`, `backend/src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md` (optional note)
- **Patterns:** Extend `lecturerFlags()` loop already loading flagged matches; reuse `hashSimilarity` on `SubmissionPlagiarismMatch`.
- **Test scenarios:**
  - When two flagged matches exist for the same student-lab with different similarities, the map stores the max.
  - When a student is flagged on a lab, both student IDs in the pair get that lab entry (same as boolean map behavior).
  - Unflagged students omit overlap entries.
- **Verification:** `mvn test` for any existing plagiarism tests; manual Swagger check of flags JSON shape.

### U2. Two-panel `GradeOverviewTable` with score-first cells

- **Goal:** Render identity and labs as two matching surface panels; sync vertical scroll; show mark + % per R5–R8.
- **Files:** `frontend/src/components/lecturer/GradeOverviewTable.jsx`, `frontend/src/components/lecturer/PlagiarismDangerMark.jsx` (helper for overlap lookup if useful)
- **Patterns:** Match Grading overview dual-panel spacing; reuse `SortableTableHeader`, `PlagiarismDangerMark`, `formatNumber`/`formatText`; equal `h-10` rows; score-anchor + plag-aside CSS as locked in brainstorm.
- **Test scenarios:**
  - Loading and empty states span both panels / shared message.
  - Pagination footer remains one control under both panels (R10).
  - Horizontal scroll only on labs panel; identity has no H-scroll (R2).
  - Flagged cell: score centerline matches unflagged neighbor; `%` to the right of mark when overlap > 0.
- **Verification:** `npm run build` in `frontend/`; manual Grading tab with many labs.

### U3. Wire overlap map in `LecturerDashboard`

- **Goal:** Consume new flags field and pass into `GradeOverviewTable`.
- **Files:** `frontend/src/pages/LecturerDashboard.jsx`
- **Patterns:** Extend existing flags fetch (~`/api/lecturer/plagiarism/flags`); store `overlapByStudentAndLab` as nested maps keyed by string IDs.
- **Test scenarios:**
  - Flags fetch failure still loads grade overview without overlap % (marks may be empty).
  - Refresh flags after grading tab mount still works.
- **Verification:** Manual: flagged student shows %; unflagged does not.

### U4. Lecturer component DOX

- **Goal:** Document two-panel matrix and overlap display contract.
- **Files:** `frontend/src/components/lecturer/AGENTS.md`
- **Patterns:** Update `GradeOverviewTable` / plagiarism bullets only.
- **Test scenarios:** N/A (docs).
- **Verification:** DOX pass on changed paths.

---

## Verification Contract

| Check | Command / method | Applies to |
|---|---|---|
| Frontend build | `npm run build` from `frontend/` | U2, U3 |
| Backend unit tests | `mvn test` from `backend/` (plagiarism-related) | U1 |
| Manual Grading UX | Lecturer login → Grading → many labs + flagged cell | U2, U3 |

---

## Definition of Done

- [ ] R1–R10 satisfied on the Grading tab
- [ ] Flags API returns overlap map; UI shows integer % when > 0 beside `▲`
- [ ] Vertical scroll sync and shared pagination work
- [ ] `npm run build` succeeds; lecturer AGENTS.md updated
- [ ] No click-to-details on matrix plagiarism mark
