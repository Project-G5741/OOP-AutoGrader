---
title: Terms UI Import and Search - Plan
type: feat
date: 2026-08-23
topic: terms-ui-import-search
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Terms UI Import and Search - Plan

## Goal Capsule

- **Objective:** Refine the lecturer Terms page so Excel import and manual enrollment sit side-by-side, and both the available-student picker and enrolled roster support client-side search.
- **Product authority:** Product Contract below. Surrounding term-management behavior is unchanged unless cited.
- **Open blockers:** None.

---

## Product Contract

### Summary

Split the "Add students" card into a 1/3 Excel import column and a 2/3 manual-enrollment column with search and checkboxes. Add a search field above the enrolled-student table. All search is client-side over data already loaded from the roster API.

### Problem Frame

Lecturers managing term rosters currently see Excel import stacked above a native multi-select of every available student. With more than a handful of students, finding someone to add or verify in the roster requires scrolling long lists. The page already loads the full roster in one request; the gap is discoverability in the UI.

### Key Decisions

- **Side-by-side import layout (1/3 Excel, 2/3 manual)** — Governs R1, R2.
- **Searchable checkbox list for manual enrollment** — Governs R3, R4. (session-settled: user-directed — chosen over typeahead-with-chips: better for bulk manual adds.)
- **Client-side search only** — Governs R5, R6. Matches User Management pattern.
- **Responsive stack on narrow viewports** — Governs R1, R2.

### Actors

- A1. **Lecturer** — manages term rosters, imports students, manually enrolls, searches enrolled list.

### Requirements

- R1. The "Add students" card uses a horizontal split on large viewports: Excel ~1/3, manual ~2/3.
- R2. Below the large breakpoint, Excel stacks above manual enrollment at full width.
- R3. Manual enrollment uses checkboxes instead of native multi-select.
- R4. Available-student search filters by name, IRN, or email, case-insensitively.
- R5. Add enrolls all checked available students; button shows selected count when > 0.
- R6. When no available students, show existing empty state.
- R7. Excel import behavior unchanged.
- R8. Enrolled table columns and row actions unchanged.
- R9. Roster search filters enrolled rows by name, IRN, or email, case-insensitively.
- R10. No roster matches shows empty-filter message with headers preserved.

### Key Flows

- F1. Manual add with search — search narrows available list → check students → Add → roster refreshes.
- F2. Roster lookup — roster search filters enrolled table; clear restores full list.

### Acceptance Examples

- AE1. Desktop: Excel and manual side-by-side at ~1:2 width (R1).
- AE2. Typing in available search narrows checkbox list; Add enrolls only checked matches (R4, F1).
- AE3. Roster search filters enrolled rows; no-match shows empty-filter message (R9, R10).
- AE4. Narrow viewport stacks Excel above manual (R2).

### Scope Boundaries

**In scope:** Add students layout, checkbox picker, both search fields, responsive stack.

**Deferred:** Server-side search, creating students from Terms page, term sidebar/create changes.

### Dependencies / Assumptions

- Roster loads via existing `GET /api/lecturer/terms/{id}/roster`.
- User Management search pattern is the reference for fields and styling.

---

## Planning Contract

### Summary

Refactor `TermManagement.jsx` only: split Add students into `lg:grid-cols-[1fr_2fr]`, replace multi-select with filtered checkbox list + search, add roster table search toolbar. No backend changes. Verify with `npm run build`.

**Product Contract preservation:** Enriches in place; no scope change.

### Key Technical Decisions

- KTD1. **Filter helper inline in component** — match UserManagement's `useMemo` + lowercase haystack pattern; no new shared util for two call sites.
- KTD2. **Reuse `Search` icon + input classes from `UserTable.jsx`** — visual consistency.
- KTD3. **Clear available selections on term change** — existing `setSelectedStudentIds([])` in `loadTermStudents` covers this; also clear search strings when switching terms.
- KTD4. **Checkbox list in scrollable container** — `max-h` + `overflow-y-auto` so long rosters don't blow layout.
- KTD5. **No automated frontend tests** — verify via build + manual Terms page check per project convention.

---

## Implementation Units

### U1. Split Add students layout and checkbox manual picker

**Goal:** Side-by-side Excel/manual layout with searchable checkbox list for available students.

**Requirements:** R1–R7, F1, AE1, AE2, AE4

**Dependencies:** None

**Files:**
- `frontend/src/pages/TermManagement.jsx`

**Approach:**
1. Add `availableSearch` state; `filteredAvailable` useMemo matching name, `studentCode`, email.
2. Replace stacked layout with `grid grid-cols-1 lg:grid-cols-[1fr_2fr] gap-3`.
3. Keep Excel drop zone in left column; move helper text to card level or shorten for column width.
4. Right column: search input, scrollable checkbox list, Add button with `Add selected (N)` label when N > 0.
5. Toggle checkbox updates `selectedStudentIds`; preserve existing `handleEnroll`.
6. Reset `availableSearch` when term changes in `handleSelectTerm` / `loadTermStudents`.

**Patterns to follow:** `frontend/src/pages/UserManagement.jsx` (filter), `frontend/src/components/UserTable.jsx` (search input styling)

**Test scenarios:**
- Available search "minh" shows only matching students in checkbox list.
- Checking two students and clicking Add calls enroll with both IDs.
- Below lg breakpoint, Excel appears above manual section (code review / manual).
- All students enrolled shows empty message, no checkbox list.

**Verification:** `npm run build` succeeds; manual check on Terms page.

### U2. Enrolled roster search

**Goal:** Search field above enrolled student table filtering rows client-side.

**Requirements:** R8–R10, F2, AE3

**Dependencies:** U1 (same file; can ship together)

**Files:**
- `frontend/src/pages/TermManagement.jsx`

**Approach:**
1. Add `rosterSearch` state; `filteredStudents` useMemo over `students`.
2. Toolbar above table with search input (same styling as U1).
3. Render `filteredStudents` in tbody; empty-filter message when `students.length > 0` but filter empty.
4. Reset `rosterSearch` on term change.

**Test scenarios:**
- Roster search by partial IRN shows matching rows only.
- No matches shows "No students match your search" (or similar) with thead intact.
- Clear search restores all rows.

**Verification:** `npm run build`; manual roster search on Terms page.

---

## Verification Contract

| Check | Command / action | Pass criteria |
|---|---|---|
| Build | `npm run build` (from `frontend/`) | Exit 0 |
| Layout | Manual: Terms page, large viewport | Excel left ~1/3, manual right ~2/3 |
| Available search | Manual: type partial name | Checkbox list narrows |
| Roster search | Manual: type partial IRN | Table rows filter |
| Mobile stack | Manual: narrow viewport | Excel above manual |

---

## Definition of Done

- [x] U1 and U2 complete in `TermManagement.jsx`
- [x] `npm run build` passes
- [x] No backend or API changes
- [x] Excel import, suspend, remove behaviors unchanged
