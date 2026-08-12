---
title: App-Wide Clickable Column-Header Table Sort with Shared SortableTableHeader and sort.js
date: 2026-08-12
category: design-patterns
module: frontend + lecturer analytics
problem_type: design_pattern
component: frontend_stimulus
severity: medium
applies_when:
  - Replacing toolbar sort buttons with spreadsheet-style clickable column headers across multiple tables
  - Mixing server-side sort on paginated lecturer tables with client-side sort on fully loaded lists in the same app
  - Extending backend sort resolvers and SQL ORDER BY for new columns while keeping pagination correct
  - API timestamps are formatted as dd/MM/yyyy strings and client comparators must parse them before localeCompare
  - Header clicks must not trigger row-select handlers on tables where rows are also clickable
tags:
  - table-sort
  - sortable-table-header
  - dual-chevron
  - server-side-sort
  - client-side-sort
  - grade-overview
  - lecturer-analytics
  - sort-js
---

# App-Wide Clickable Column-Header Table Sort

## Context

The OOP AutoGrader lecturer UI has several sortable tables: lab roster, challenge roster, grade overview matrix, attempt history drawer, and inline submission history. A shared pattern emerged across these screens:

| Layer | Responsibility |
|---|---|
| `SortableTableHeader.jsx` | Clickable `<th>` with dual chevrons, `aria-sort`, and `aria-label` on the sort button |
| `sort.js` | `toggleSortState`, `sortRows`, `parseDisplayTimestamp`, `buildServerSortParam`, `formatGradeOverviewSortParam` |
| Page orchestrator (`LecturerDashboard.jsx`) | Owns `sortState`, chooses server vs client sort, wires fetch timing |
| `LecturerAnalyticsRepository.java` | Server-side `ORDER BY` for paginated rosters and grade overview |

**Server sort** (paginated, large datasets): lab roster, challenge roster, grade overview. Sort state changes trigger a refetch with a `sort` query param.

**Client sort** (small in-memory lists): attempt history drawer, grade-overview submission history panel, user management. `sortRows` runs in a `useMemo` over already-fetched data.

Grade overview per-lab columns use composite field keys (`labScore:{uuid}`) on the frontend, encoded as `labScore,{uuid},{direction}` for the API. The backend binds `:sortLabId` and sorts via the shared `highest_scores` CTE.

Timestamps from lecturer analytics are formatted as `dd/MM/yyyy HH:mm` in `LecturerAnalyticsService` (`SUBMISSION_TIME_FORMAT` at `LecturerAnalyticsService.java:34-35`). That display format is what tables show and what client sort must understand.

## Guidance

### Shared utilities (`frontend/src/utils/sort.js`)

- **`toggleSortState(current, field)`** — Same field toggles `asc` ↔ `desc`; new field starts at `asc`.
- **`sortRows(rows, field, direction, getValue?)`** — Stable client sort via `compareValues`. Pass a custom accessor when a column needs coercion (e.g. `parseDisplayTimestamp` for `submittedAt`, or numeric `attemptNumber`).
- **`parseDisplayTimestamp(value)`** — Parses `dd/MM/yyyy` and `dd/MM/yyyy HH:mm` explicitly; falls back to `Date.parse` only for other strings. Returns epoch ms or `null`.
- **`compareValues(left, right)`** — Coerces to dates **only** when both operands match `isDateLikeString` (ISO date prefix or display timestamp regex). All other strings use `localeCompare` with `numeric: true`. Never blindly `Date.parse` every string.
- **`buildServerSortParam(sortState)`** — `{field},{direction}` for standard roster endpoints.
- **`formatGradeOverviewSortParam(sortState)`** — Maps `labScore:{uuid}` → `labScore,{uuid},{direction}`; default `studentName,asc`.

### Shared header (`SortableTableHeader.jsx`)

Props: `label`, `field`, `activeField`, `direction`, `onSort`, optional `sortable`, `stopRowClick`, `className`.

- Set `aria-sort` on `<th>` (`ascending` / `descending` / `none`).
- Set `aria-label` on the button: `"Sort by {label}"` or `"Sort by {label}, ascending|descending"` when active.
- Use `stopRowClick` when the header sits inside a clickable row (grade overview, history tables).

### Server vs client split — pick one fetch owner per table

| Table | Pattern | Why |
|---|---|---|
| Lab roster | Handler calls `fetchSubmissions` directly | Sort + page reset in one place; no `useEffect` on `rosterSort` |
| Challenge roster | Handler updates state only; `useEffect` fetches | Sort/page/tab drive fetch via deps |
| Grade overview | Handler updates state only; `useEffect` fetches when `activeNav === 'grading'` | Avoids double fetch on sort click |
| Attempt history / submission history | `useMemo` + `sortRows` only | Data already loaded; no API sort param |

**Rule:** Do not call `fetch*` in both the sort handler **and** a `useEffect` that depends on the same sort state. Either the handler fetches (roster) or the effect fetches (grade overview, challenge).

### Lab-change race guard (challenge tab)

When `selectedLabId` changes, the page resets `challenges` and `challengesLabId` before new challenges load. `activeTab` may still point at a challenge id from the previous lab.

Derive fetch target with `activeChallengeId`:

```javascript
const activeChallengeId = useMemo(() => {
  if (activeTab === 'overview' || !selectedLabId || challengesLabId !== selectedLabId) {
    return null;
  }
  return challenges.some((c) => c.id === activeTab) ? activeTab : null;
}, [activeTab, selectedLabId, challengesLabId, challenges]);
```

The challenge `useEffect` must depend on `activeChallengeId`, not raw `activeTab`. Fetch only when `selectedLabId && activeChallengeId`.

### Backend grade-overview per-lab sort

`findGradeOverviewStudents` and `findLabScoresForStudents` share `HIGHEST_SCORES_CTE`:

```sql
highest_scores AS (
    SELECT p.user_id, p.lab_id, p.highest_score AS score
    FROM student_lab_progress p
    WHERE p.last_submitted_at IS NOT NULL
)
```

Per-lab `ORDER BY` must use this CTE (with `:sortLabId`), not a raw join on `student_lab_progress` without the `last_submitted_at` filter. Display cells and sort order must use the same "has submitted" semantics.

`formatGradeOverviewOrderBy` binds `:sortLabId` only when `sortColumn === 'lab_score'` and `sortLabId != null`.

### Pitfalls fixed after code review

1. **`Date.parse` on `dd/MM/yyyy HH:mm`** — Returns `NaN` or wrong order. Use `parseDisplayTimestamp` in accessors or rely on `isDateLikeString` + `compareValues`.
2. **Duplicate fetches** — Handler and `useEffect` both calling the same fetch on sort change.
3. **Lab change race** — Challenge fetch with stale `activeTab` before `challengesLabId` matches `selectedLabId`.
4. **Mismatched grade-overview sort semantics** — Per-lab `ORDER BY` ignoring `last_submitted_at IS NOT NULL`.
5. **Over-broad date coercion** — `compareValues` running all strings through `Date.parse` (breaks IRNs, names, lab labels).
6. **Accessibility / API wiring** — Missing `aria-label` on sort buttons; per-lab sort missing bound `:sortLabId` param.

## Why This Matters

- **Correctness:** Wrong timestamp parsing makes "newest first" lie. Mismatched SQL semantics make sorted columns disagree with displayed scores.
- **Performance:** Duplicate fetches on every header click waste API calls and cause flicker.
- **Race safety:** Stale challenge ids after lab switch can 404 or show wrong-lab data.
- **Accessibility:** Screen readers need `aria-sort` and descriptive `aria-label` on sort controls.
- **Maintainability:** One header component and one sort utility keep roster, grade matrix, and drawers consistent.

## When to Apply

- Adding a new sortable lecturer table or column.
- Sorting any field formatted as `dd/MM/yyyy HH:mm` from the backend.
- Paginated data where sort must be server-side (roster, grade overview).
- Per-lab or composite sort keys that need extra query params (`labScore,{uuid},asc`).
- Resetting tab/challenge state when a parent selector (lab) changes.
- Any table header inside a clickable row (`stopRowClick`).

## Examples

### 1. Display timestamps — `Date.parse` vs `parseDisplayTimestamp`

**Before (broken client sort on `submittedAt`):**

```javascript
const sorted = [...rows].sort((a, b) => Date.parse(a.submittedAt) - Date.parse(b.submittedAt));
```

**After (`sort.js` + `LabAttemptHistoryDrawer.jsx`):**

```javascript
export function parseDisplayTimestamp(value) { /* parses dd/MM/yyyy[ HH:mm] */ }

if (sortState.field === 'submittedAt') return parseDisplayTimestamp(attempt.submittedAt);
```

### 2. Duplicate fetches — handler + `useEffect`

**Before:** `handleGradeOverviewSort` calls `fetchGradeOverview` while a `useEffect` on `gradeOverviewSort` also fetches.

**After:** Handler updates state only; `useEffect` is the single fetch owner when `activeNav === 'grading'`. Lab roster keeps the opposite pattern (handler fetches, no sort `useEffect`).

### 3. Lab change race — `activeTab` vs `activeChallengeId`

**Before:** Challenge `useEffect` depends on raw `activeTab`, which can still be a previous lab's challenge id when `selectedLabId` changes.

**After:** Guard with `challengesLabId === selectedLabId` and derive `activeChallengeId` from the loaded challenge list.

### 4. Grade overview per-lab `ORDER BY` — `highest_scores` CTE alignment

**Before:** `ORDER BY` subquery on `student_lab_progress` without `last_submitted_at IS NOT NULL`.

**After:** Sort subquery reads from `highest_scores` CTE; `query.setParameter("sortLabId", sortLabId)` when sorting by lab column.

### 5. `compareValues` — date coercion scope

**Before:** All strings passed through `Date.parse`.

**After:** Date coercion only when both operands match `isDateLikeString`; otherwise `localeCompare` with `numeric: true`.

### 6. `aria-label` on sort buttons

`SortableTableHeader` sets `aria-label` on the sort button and `aria-sort` on `<th>`.

## Related

- [Grading tab showed latest attempt score instead of highest lab score](../logic-errors/grading-tab-latest-vs-highest-score.md) — shared `highest_scores` CTE semantics; score-sort ordering assumes highest-score values from that fix
- Plan: `docs/plans/2026-08-12-004-feat-column-header-sort-plan.md`
