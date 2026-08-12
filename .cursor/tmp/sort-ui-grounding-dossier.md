# Table Column Header Sorting UI — Grounding Dossier

Generated for requirements brainstorm: chevron up/down on headers, remove separate sort buttons.

---

## Icon library

- **`lucide-react`** (`^1.16.0`) — sole icon library per `frontend/package.json` and `frontend/AGENTS.md`
- Sort UI already uses `ChevronUp` / `ChevronDown` from lucide on sort **buttons** (not headers)
- Other chevrons in repo are for expand/collapse accordions, dropdowns, navigation — not column sort

---

## Shared table / sort components

**No shared sortable table or sort-header primitive exists.**

| Component | Path | Sort in component? |
|---|---|---|
| `UserTable` | `frontend/src/components/UserTable.jsx` | Toolbar button only (`onSort`, `sortDirection` props); `<th>` static |
| `SubmissionTable` | `frontend/src/components/lecturer/SubmissionTable.jsx` | No sort; static headers |
| `GradeOverviewTable` | `frontend/src/components/lecturer/GradeOverviewTable.jsx` | No sort; static headers |
| `GradeOverviewSubmissionHistory` | `frontend/src/components/lecturer/GradeOverviewSubmissionHistory.jsx` | Text toggle button ("Newest first" / "Oldest first"); static headers |

`frontend/src/components/ui/` has Button, Modal, Select, etc. — **no Table or SortControl**.

Sort state and handlers live in page containers (`LecturerDashboard.jsx`, `UserManagement.jsx`).

---

## All sort functionality (frontend)

### 1. User Management — client-side, name only

- **Page:** `frontend/src/pages/UserManagement.jsx` (L66–114, L311–312)
- **UI:** `UserTable.jsx` toolbar button "Sort by name" with ChevronUp/Down (L36–43)
- **Behavior:** Client-side `.sort()` on filtered users by `fullname`; toggles asc/desc; resets page to 1
- **Backend:** None — fetches all users via `GET /api/users/getAllUser`

### 2. Lecturer roster (overview tab) — server-side

- **Page:** `frontend/src/pages/LecturerDashboard.jsx`
  - State: `rosterSort` `{ field, direction }` default `studentName,asc` (L121)
  - Handlers: `handleRosterSort` (L389–398), `fetchSubmissions` with `sort` query (L221–227)
  - UI: Two toolbar buttons above `SubmissionTable` — "Sort by name", "Sort by score" (L747–778)
  - Active button: purple highlight + ChevronUp (asc) or ChevronDown (desc/non-asc)
- **Table:** `SubmissionTable` — headers not clickable (Student, ID, Score, Attempt, Submitted At, Action)

### 3. Grade overview matrix (grading tab) — server-side

- **Page:** `LecturerDashboard.jsx`
  - State: `gradeOverviewSort` default `studentName,asc` (L151)
  - Handlers: `handleGradeOverviewSort` (L435–442), `fetchGradeOverview` (L313+)
  - UI: Same two-button pattern above `GradeOverviewTable` (L861–893)
- **Table:** `GradeOverviewTable` — Student, IRN, Total Score, dynamic lab columns; no sort UI in table

### 4. Submission history panel (grading tab, row click) — client-side

- **Page:** `LecturerDashboard.jsx` — `historySortDirection` (L150), client sort in `filteredGradeStudentHistoryRows` (L481–499)
- **UI:** `GradeOverviewSubmissionHistory.jsx` — text button "Newest first" / "Oldest first" (L35–41), **no chevrons**
- **Data:** From `GET /api/analytics/student/{studentId}`; sort applied client-side on `submittedAt`

### 5. Challenge submissions tab — server-side, no UI controls

- **Page:** `LecturerDashboard.jsx` — `fetchChallengeSubmissions` hardcodes `sort=submittedAt,desc` (L261)
- **Table:** `SubmissionTable` — no sort buttons; backend supports more fields via `sort` param

### 6. Reports / analytics student overview — backend only

- **Backend:** `GET /api/analytics/student-overview?sort=overallAverage&direction=desc` (`AnalyticsController.java` L55–65)
- **Frontend:** No sort UI found in `Reports.jsx`

---

## Current pattern summary

| Pattern | Where used |
|---|---|
| **Toolbar sort buttons** (bordered, chevron + label) | UserTable, LecturerDashboard roster, LecturerDashboard grade overview |
| **Text toggle button** (no icon) | GradeOverviewSubmissionHistory date sort |
| **Header-click sort** | **Nowhere** — all `<th>` elements are static |
| **Hardcoded server sort, no UI** | Challenge submissions (`submittedAt,desc`) |

Duplicated button markup in `LecturerDashboard.jsx` (roster block ~L747–778 and grade overview ~L861–893) — candidate for extraction if moving to header sort.

Chevron semantics today:
- Shown only when that field is the active sort field
- `asc` → ChevronUp; otherwise ChevronDown (including first click on inactive field which sets asc but inactive fields show no chevron)

---

## Backend sort parameters

### Comma-separated `sort=field,direction` (lecturer analytics)

| Endpoint | Controller | Default | Supported fields (via `LecturerAnalyticsService`) |
|---|---|---|---|
| `GET /api/labs/{labId}/submissions` | `LabController.java` L59–66 | `studentName,asc` (service) | `studentName`, `score`, `attempt`, `studentCode`, `submittedAt` |
| `GET /api/labs/{labId}/submissions/export` | `LabController.java` L69–72 | same | same |
| `GET /api/lecturer/grade-overview` | `LecturerAnalyticsController.java` L28–33 | `studentName,asc` | `studentName`, `score` |
| `GET /api/labs/{labId}/challenges/{id}/students` | `ChallengeController.java` L99–105 | `studentName,asc` (service) | `studentName`, `score`, `attempt(s)`, `studentCode`, `submittedAt` |

Resolution logic: `LecturerAnalyticsService.java` — `resolveLabSort` (L415–430), `resolveGradeOverviewSort` (L433–445), `resolveChallengeSort` (L397–412).

### Separate `sort` + `direction` query params

| Endpoint | Controller | Default sort | Notes |
|---|---|---|---|
| `GET /api/analytics/student-overview` | `AnalyticsController.java` L55–65 | `overallAverage`, `desc` | `AnalyticsRepository.findStudentOverview` supports `completedLabs`, `studentName`, etc. |

---

## Tables without sort (reference)

- `SubmissionTable` — roster + challenge views
- `GradeOverviewTable` — cross-lab matrix
- Student history tables (`StudentHistoryPage.jsx`, `StudentUI.jsx`) — no column sort
- Lecturer structure editors — no data table sort

---

## Implications for header-click + chevron redesign

1. **Three UI surfaces** to migrate off toolbar buttons: UserTable, LecturerDashboard roster, LecturerDashboard grade overview (+ optionally history date sort on "Submitted At" header).
2. **Sort props must move into table components** or a thin wrapper — today `SubmissionTable` / `GradeOverviewTable` have no sort callbacks.
3. **Backend already supports** server sort for roster/grade overview; User Management is client-only; challenge tab could expose sort if UI added.
4. **Per-lab columns** in grade overview are not backend-sortable today — only `studentName` and `score` (total).
5. **Reuse lucide-react** ChevronUp/ChevronDown — already established visual language on sort buttons.
