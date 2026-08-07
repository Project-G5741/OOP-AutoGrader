# Lecturer Components

## Purpose

Grading dashboard widgets used by `LecturerDashboard.jsx`.

## Ownership

| File | Role |
|---|---|
| `DashboardSection.jsx` | Main grading overview layout |
| `LecturerOverviewCard.jsx` | Summary stat cards |
| `SubmissionTable.jsx` | Enrolled-student roster table |
| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |

## Local Contracts

### Data source

- `LecturerDashboard.jsx` fetches overview, lab statistics, and enrolled-student roster (`GET /api/labs/{labId}/submissions`)
- Roster pagination counts **unique enrolled students** for the lab's term (`term_enrollment`), not submission attempts
- `SubmissionTable` renders one row per enrolled student; non-submitters show placeholders (`--`, `0`)
- `ReportsPanel` renders analytics from `/api/analytics/dashboard` via `Reports.jsx`

### Composition

```
LecturerDashboard
  → AppShell + NavBar
  → DashboardSection (activeNav === 'dashboard')
       → LecturerOverviewCard
       → SubmissionTable (student roster)
```

User management and submission management are separate pages (`UserManagement`, `SubmissionManagement`), not in this folder.

## Work Guidance

- Data fetching stays in `LecturerDashboard.jsx`; keep table/card components presentational
- `exportOverview` paginates through `/submissions` until all enrolled students are exported
- `UploadPanel.jsx` is dead code; remove or wire up when lecturer upload flow is defined

## Verification

- Manual: log in as lecturer, confirm roster row count matches enrolled students for the lab term

## Child DOX Index

No child docs.
