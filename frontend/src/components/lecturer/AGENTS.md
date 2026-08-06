# Lecturer Components

## Purpose

Grading dashboard widgets used by `LecturerDashboard.jsx`.

## Ownership

| File | Role |
|---|---|
| `DashboardSection.jsx` | Main grading overview layout |
| `LecturerOverviewCard.jsx` | Summary stat cards |
| `SubmissionTable.jsx` | Submission list table |
| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |

## Local Contracts

### Data source

- `LecturerDashboard.jsx` fetches overview, lab statistics, and submissions from live APIs
- `SubmissionTable` renders the submission page returned by `/api/labs/{labId}/submissions`
- `ReportsPanel` renders analytics from `/api/analytics/dashboard` via `Reports.jsx`

### Composition

```
LecturerDashboard
  → AppShell + NavBar
  → DashboardSection (activeNav === 'dashboard')
       → LecturerOverviewCard
       → SubmissionTable
```

User management and submission management are separate pages (`UserManagement`, `SubmissionManagement`), not in this folder.

## Work Guidance

- When wiring live submission data, change `LecturerDashboard.jsx` to fetch from API and pass results to these components
- Keep table/card components presentational — data fetching stays in the page
- `UploadPanel.jsx` is dead code; remove or wire up when lecturer upload flow is defined

## Verification

- Manual: log in as lecturer, confirm dashboard section renders with mock data

## Child DOX Index

No child docs.
