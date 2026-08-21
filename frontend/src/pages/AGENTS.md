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
| `StudentDashboard.jsx` | Student shell: lab sidebar, upload, stats; toggles history |
| `StudentHistory.jsx` | Thin wrapper → `StudentHistoryPage.jsx` |
| `UserManagement.jsx` | User CRUD (live API) |
| `TermManagement.jsx` | Lecturer term year create, current-term flag, student enrollment, Excel import |
| `SubmissionManagement.jsx` | Solution/lab structure + operational testcase authoring (`SolutionManagement.jsx` → `/api/lecturer/labs`) |

## Local Contracts

### Top-level navigation (URL routes)

`App.jsx` gates by `sessionStorage` user roles and React Router paths:

| Path | Role required | Screen |
|---|---|---|
| `/` | — | Login (redirects if already signed in) |
| `/lecturer-dashboard` | LECTURER | Lecturer grading overview |
| `/lecturer-grading` | LECTURER | Grade overview matrix |
| `/lecturer-users` | LECTURER | User Management |
| `/lecturer-terms` | LECTURER | Term management (year, current term, enroll students) |
| `/lecturer-solution` | LECTURER | Solution Management |
| `/lecturer-report` | LECTURER | Reports |
| `/student-dashboard` | STUDENT | Student main |
| `/student-history` | STUDENT | Student history |

Dual-role users land on `/lecturer-dashboard` after login; student routes remain reachable by URL. Wrong-role access redirects to the user's default dashboard. Active students not in the current term land on `/student-history` and cannot open the submit dashboard.

### Lecturer in-dashboard sections (`activeNav`)

| Value | Renders | API |
|---|---|---|
| `dashboard` | Grading overview, challenge tabs, `SubmissionTable`, export drawers | Live `/api/lecturer/overview`, `/api/labs/{id}/statistics`, `/api/labs/{id}/submissions` (includes `plagiarismFlagged`), `/api/labs/{id}/challenges/{id}/students`, `GET /api/lecturer/plagiarism/flags` |
| `grading` | Cross-lab `GradeOverviewTable` + Export + row-click submission history | Live `GET /api/lecturer/grade-overview`, `GET /api/lecturer/plagiarism/flags`, `GET /api/analytics/student/{studentId}` |
| `users` | `UserManagement` | Live `/api/users/*` |
| `terms` | `TermManagement` | Live `/api/lecturer/terms` create/set current/enroll; `GET /{id}/roster`; Excel import `POST /api/lecturer/terms/{id}/students/import` |
| `projects` | `SolutionManagement` | Live API (`/api/lecturer/labs/*`, `/api/lecturer/labs/{labId}/challenges/{challengeId}/testcases`, `/api/master-data?category=SCOPE|DECLARING_TYPE|RELATION_TYPE`, `/api/terms`); challenge / class / MMD weights persist on structure save; labs have no weight |
| `reports` | `Reports.jsx` | Live `/api/analytics/dashboard` |

### Student in-dashboard sections

| State | Renders | API |
|---|---|---|
| `showHistory === false` | Main dashboard (left lab list + right upload/stats/results) | `GET /api/labs` only when `inCurrentTerm`; `GET /api/labs/{id}/stats` on login/lab change for attempts + latest timestamp only; **Current Grade** and challenge scores + class/MMD detail only after upload in session; success **Toast** on grading complete |
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
| `POST /api/users/{id}/suspend` | `UserManagement.jsx`, `TermManagement.jsx` — student-only; blocks login |
| `POST /api/users/{id}/unsuspend` | `UserManagement.jsx`, `TermManagement.jsx` — restores login |
| `GET /api/labs` | `StudentDashboard.jsx` (student JWT; current-term labs only; skipped when out of term) |
| `GET /api/students/term-access` | `StudentDashboard.jsx` |
| `GET /api/lecturer/terms` | `TermManagement.jsx` |
| `GET /api/lecturer/terms/{termId}/roster` | `TermManagement.jsx` — enrolled + available students |
| `POST /api/lecturer/terms/{termId}/students/import` | `TermManagement.jsx` — body `{ rows: [{ studentCode, email }] }` parsed from Excel |
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
- `LoginUI.jsx` shows field validation after a Sign In attempt or after a field loses focus (`touchedFields`); auth API failures use `readFriendlyAuthError` from `frontend/src/utils/apiError.js` (never raw backend `detail` text). Google 403 opens first-time setup; Google 423 is inactive and stays on the login form.
- `ForgotPasswordUI.jsx` and `ResetPasswordUI.jsx` use the same touched/submit gating as `LoginUI.jsx` for inline field errors
- `UserManagement.jsx` normalizes backend field names (`fullName`/`fullname`, `studentCode`/`irn`)
- When replacing mock data, update the relevant page and its child component docs
- Student history: `GET /api/submissions/my-history` and `GET /api/submissions/my-labs` via `StudentHistoryPage.jsx`
- Term Excel import: `frontend/src/utils/studentImport.js` finds Student ID / IRN / IRD and Email columns anywhere in the sheet; Terms drop zone accepts drag/drop or click; `isSpreadsheetFile` lives in that util
- Lecturer suspend/restore: `POST /api/users/{id}/suspend` and `POST /api/users/{id}/unsuspend` from `UserManagement.jsx` (student-only row action) and `TermManagement.jsx` roster

## Verification

- Manual role-based navigation after login
- Lecturer user CRUD round-trip
- Student lab sidebar list populated from API; click selects the lab for upload/results
- Lecturer Terms: create year + term, set current, enroll/remove students, import Excel by IRN + email (drag/drop or click); suspend/restore student-only accounts from the roster
- Lecturer Users: suspend/restore student-only accounts; suspended students cannot log in
- Active student not in the current term lands on History with Home hidden

## Child DOX Index

No child docs. Component details live under `src/components/*/AGENTS.md`.
