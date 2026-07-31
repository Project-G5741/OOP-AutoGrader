# Pages

## Purpose

Screen-level containers: authentication, role dashboards, and in-dashboard section switching.

## Ownership

| File | Role |
|---|---|
| `Login.jsx` | Thin wrapper → `LoginUI.jsx` |
| `LoginUI.jsx` | IRN/password login, Google OAuth, JWT decode |
| `FirstTimeSetupUI.jsx` | New Google user: set IRN + password via `/api/auth/google/upsert` |
| `LecturerDashboard.jsx` | Lecturer shell: `activeNav` section switching |
| `StudentDashboard.jsx` | Student shell: lab select, upload, stats; toggles history |
| `StudentHistory.jsx` | Thin wrapper → `StudentHistoryPage.jsx` |
| `UserManagement.jsx` | User CRUD (live API) |
| `SubmissionManagement.jsx` | Solution/lab upload management (mock local state) |

## Local Contracts

### Top-level navigation (no URL routes)

`App.jsx` gates by `sessionStorage` user roles. Pages do not use `<Routes>` or `useNavigate`.

### Lecturer in-dashboard sections (`activeNav`)

| Value | Renders | API |
|---|---|---|
| `dashboard` | Grading overview, `SubmissionTable` | Mock data |
| `users` | `UserManagement` | Live `/api/users/*` |
| `projects` | `SubmissionManagement` | Local mock |
| `reports` | Placeholder | None |

### Student in-dashboard sections

| State | Renders | API |
|---|---|---|
| `showHistory === false` | Main dashboard (labs, upload, stats) | `GET /api/labs`, upload via `DropZone` |
| `showHistory === true` | `StudentHistoryPage` | Mock `HISTORY` constant |

### Header commands (`Header.jsx` → `onCommand`)

Shared: `home`, `history`, `editProfile` (opens `ProfileEditModal`).

### API endpoints used from pages

| Endpoint | Page |
|---|---|
| `POST /api/auth/login` | `LoginUI.jsx` |
| `POST /api/auth/google` | `LoginUI.jsx` |
| `POST /api/auth/google/upsert` | `FirstTimeSetupUI.jsx` |
| `GET /api/users/getAllUser` | `UserManagement.jsx` |
| `POST /api/users/addUser` | `UserManagement.jsx` |
| `PUT /api/users/{id}` | `UserManagement.jsx` |
| `DELETE /api/users/{id}` | `UserManagement.jsx` |
| `GET /api/labs` | `StudentDashboard.jsx` |

Upload (`POST /api/submissions/{labId}/{attemptNumber}/upload`) is called from `DropZone.jsx`, not directly from pages.

## Work Guidance

- Pages compose `AppShell` (layout), child components, and local state
- `UserManagement.jsx` normalizes backend field names (`fullName`/`fullname`, `studentCode`/`irn`)
- When replacing mock data, update the relevant page and its child component docs
- Planned: `GET /api/submissions/mine?labId=...` for student history (commented in `StudentDashboard.jsx`)

## Verification

- Manual role-based navigation after login
- Lecturer user CRUD round-trip
- Student lab dropdown populated from API

## Child DOX Index

No child docs. Component details live under `src/components/*/AGENTS.md`.
