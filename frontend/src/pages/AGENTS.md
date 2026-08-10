# Pages

## Purpose

Screen-level containers: authentication, role dashboards, and in-dashboard section switching.

## Ownership

| File | Role |
|---|---|
| `Login.jsx` | Thin wrapper → `LoginUI.jsx` |
| `LoginUI.jsx` | Student/lecturer code + password login, Google OAuth, forgot-password entry, JWT decode |
| `ForgotPasswordUI.jsx` | Request reset link by school email |
| `ResetPasswordUI.jsx` | Set new password from `?resetToken=` query param |
| `FirstTimeSetupUI.jsx` | New Google user: set IRN + password via `/api/auth/google/upsert` |
| `LecturerDashboard.jsx` | Lecturer shell: `activeNav` section switching |
| `Reports.jsx` | Lecturer reports page (`/api/analytics/dashboard`) |
| `StudentDashboard.jsx` | Student shell: lab select, upload, stats; toggles history |
| `StudentHistory.jsx` | Thin wrapper → `StudentHistoryPage.jsx` |
| `UserManagement.jsx` | User CRUD (live API) |
| `SubmissionManagement.jsx` | Solution/lab upload management (mock local state) |

## Local Contracts

### Top-level navigation (URL routes)

`App.jsx` gates by `sessionStorage` user roles and React Router paths:

| Path | Role required | Screen |
|---|---|---|
| `/` | — | Login (redirects if already signed in) |
| `/lecturer-dashboard` | LECTURER | Lecturer grading overview |
| `/lecturer-grading` | LECTURER | Grade overview matrix |
| `/lecturer-users` | LECTURER | User Management |
| `/lecturer-solution` | LECTURER | Solution Management |
| `/lecturer-report` | LECTURER | Reports |
| `/student-dashboard` | STUDENT | Student main |
| `/student-history` | STUDENT | Student history |

Dual-role users land on `/lecturer-dashboard` after login; student routes remain reachable by URL. Wrong-role access redirects to the user's default dashboard.

### Lecturer in-dashboard sections (`activeNav`)

| Value | Renders | API |
|---|---|---|
| `dashboard` | Grading overview, challenge tabs, `SubmissionTable`, export drawers | Live `/api/lecturer/overview`, `/api/labs/{id}/statistics`, `/api/labs/{id}/submissions`, `/api/labs/{id}/challenges/{id}/students` |
| `grading` | Cross-lab `GradeOverviewTable` + Export + row-click submission history | Live `GET /api/lecturer/grade-overview`, `GET /api/analytics/student/{studentId}` |
| `users` | `UserManagement` | Live `/api/users/*` |
| `projects` | `SolutionManagement` | Live API (`/api/lecturer/labs/*`, `/api/master-data?category=SCOPE|DECLARING_TYPE|RELATION_TYPE`, `/api/terms`) |
| `reports` | `Reports.jsx` | Live `/api/analytics/dashboard` |

### Student in-dashboard sections

| State | Renders | API |
|---|---|---|
| `showHistory === false` | Main dashboard (labs, upload, stats) | `GET /api/labs`, `GET /api/labs/{id}/stats` on login/lab change for attempts + latest timestamp only; **Current Grade** and challenge scores + class/MMD detail only after upload in session |
| `showHistory === true` | `StudentHistoryPage` | Live `my-history` / `my-labs` APIs |

### Header commands (`Header.jsx` → `onCommand`)

Shared: `home`, `history`, `editProfile` (opens `ChangePasswordModal`).

### API endpoints used from pages

| Endpoint | Page |
|---|---|
| `POST /api/auth/login` | `LoginUI.jsx` |
| `POST /api/auth/forgot-password` | `ForgotPasswordUI.jsx` |
| `POST /api/auth/reset-password` | `ResetPasswordUI.jsx` |
| `POST /api/auth/google` | `LoginUI.jsx` |
| `POST /api/auth/google/upsert` | `FirstTimeSetupUI.jsx` |
| `GET /api/users/getAllUser` | `UserManagement.jsx` |
| `POST /api/users/addUser` | `UserManagement.jsx` |
| `PUT /api/users/{id}` | `UserManagement.jsx` — body: `roleNames`, `studentCode`, `teacherCode`, optional `password` |
| `DELETE /api/users/{id}` | `UserManagement.jsx` |
| `GET /api/labs` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/challenges?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/stats?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{id}/class?studentId=` | `StudentDashboard.jsx` |
| `GET /api/lecturer/overview` | `LecturerDashboard.jsx` |
| `GET /api/lecturer/grade-overview` | `LecturerDashboard.jsx` (`activeNav === 'grading'`) |
| `GET /api/labs/{labId}/statistics` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/submissions` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/students/{studentId}/attempts` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{challengeId}/students` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{challengeId}/class?studentId=` | `LecturerDashboard.jsx` (drawer) |
| `GET /api/labs/{labId}/challenges/{challengeId}/mmd?studentId=` | `LecturerDashboard.jsx` (drawer) |
| `GET /api/analytics/student/{studentId}` | `LecturerDashboard.jsx` (Grading tab row selection) |

Upload (`POST /api/submissions/{labId}/{attemptNumber}/upload`) is called from `DropZone.jsx`, not directly from pages.

## Work Guidance

- Pages compose `AppShell` (layout), child components, and local state
- `LoginUI.jsx` shows field validation after a Sign In attempt or after a field loses focus (`touchedFields`); auth API failures use `readFriendlyAuthError` from `frontend/src/utils/apiError.js` (never raw backend `detail` text)
- `UserManagement.jsx` normalizes backend field names (`fullName`/`fullname`, `studentCode`/`irn`)
- When replacing mock data, update the relevant page and its child component docs
- Student history: `GET /api/submissions/my-history` and `GET /api/submissions/my-labs` via `StudentHistoryPage.jsx`

## Verification

- Manual role-based navigation after login
- Lecturer user CRUD round-trip
- Student lab dropdown populated from API

## Child DOX Index

No child docs. Component details live under `src/components/*/AGENTS.md`.
