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
| `StudentDashboard.jsx` | Student shell: lab sidebar, upload, stats; toggles history; Operation Test when OT applies; [page-mascot](https://koboyo.com/page-mascot) fox at the header’s top-right on the submit view |
| `StudentHistory.jsx` | Thin wrapper → `StudentHistoryPage.jsx` |
| `NoAccessPage.jsx` | Signed-in landing for gated API 403 |
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
| `/no-access` | any signed-in user | API 403 landing (not a role-gate substitute for URLs) |

Dual-role users land on `/lecturer-dashboard` after login; student routes remain reachable by URL. Wrong-role access redirects to the user's default dashboard. Active students not in the current term land on `/student-history` and cannot open the submit dashboard.

### Lecturer in-dashboard sections (`activeNav`)

| Value | Renders | API |
|---|---|---|
| `dashboard` | Grading overview, challenge tabs, `SubmissionTable`, export drawers | Live `/api/lecturer/overview`, `/api/labs/{id}/statistics`, `/api/labs/{id}/submissions` (includes `plagiarismFlagged` + `plagiarismRole`), `/api/labs/{id}/challenges/{id}/students`, `GET /api/lecturer/plagiarism/flags`, `GET /api/lecturer/labs/{labId}/students/{studentId}/plagiarism` |
| `grading` | Cross-lab `GradeOverviewTable` + Export + row-click submission history | Live `GET /api/lecturer/grade-overview`, `GET /api/lecturer/plagiarism/flags`, `GET /api/analytics/student/{studentId}` |
| `users` | `UserManagement` | Live `/api/users/*` |
| `terms` | `TermManagement` | Live `/api/lecturer/terms` create/set current/delete/enroll; `GET /{id}/roster`; Excel import `POST /api/lecturer/terms/{id}/students/import` |
| `projects` | `SolutionManagement` | Live API (`/api/lecturer/labs/*`, `PATCH /api/lecturer/labs/{labId}/deadline` and `PATCH /api/lecturer/labs/{labId}/student-access` for the selected lab, `/api/lecturer/labs/{labId}/challenges/{challengeId}/testcases`, `/api/master-data?category=SCOPE|DECLARING_TYPE|RELATION_TYPE`, `/api/terms`); challenge / class / MMD / testcase weights persist on structure save; labs have no weight |
| `reports` | `Reports.jsx` | Live `/api/analytics/dashboard` |

### Student in-dashboard sections

| State | Renders | API |
|---|---|---|
| `showHistory === false` | Main dashboard (left lab list + right upload/stats/results) | Full-page spinner waits only for `GET /api/labs` (`inCurrentTerm`). That payload includes per-lab `challenges` and `totalSubmissions` / `latestSubmission`, so Challenges and stats populate without follow-up `/challenges` or `/stats` unless those fields are missing. Also `GET /api/submissions/my-labs?scope=current` for notifications; **after upload, challenges/stats/my-labs are not refetched for that lab** — scores and attempt counts come from the upload payload. **Current Grade** and challenge scores + class/MMD detail only after upload in session; success **Toast** on grading complete |
| `showHistory === true` | `StudentHistoryPage` | Live `my-history` + `my-labs` (all quarters, `termLabel` on each lab/submission) |

### Header commands (`Header.jsx` → `onCommand`)

Shared: `home`, `history`, `changePassword` (opens `ChangePasswordModal`). Lecturer shell hides **History** in the avatar menu (`hideHistory`); student shell may hide **Home** when out of term (`hideHome`).

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
| `GET /api/labs` | `StudentDashboard.jsx` (student JWT; current-term labs only, with embedded challenges + attempt stats; skipped when out of term) |
| `GET /api/students/term-access` | `StudentDashboard.jsx` |
| `GET /api/lecturer/terms` | `TermManagement.jsx` |
| `GET /api/lecturer/terms/{termId}/roster` | `TermManagement.jsx` — enrolled + available students |
| `POST /api/lecturer/terms/{termId}/students/import` | `TermManagement.jsx` — body `{ rows: [{ studentCode, email, fullName }] }` parsed from Excel; warning popup + **Show details** uses `notFoundStudents` and `alreadyInTermStudents` |
| `DELETE /api/lecturer/terms/{termId}` | `TermManagement.jsx` — delete non-current quarter (must have no labs) |
| `GET /api/labs/{labId}/challenges?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/stats?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{id}/class?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{id}/mmd?studentId=` | `StudentDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{id}/testcases?studentId=` | `StudentDashboard.jsx` |
| `GET /api/lecturer/overview` | `LecturerDashboard.jsx` |
| `GET /api/lecturer/grade-overview` | `LecturerDashboard.jsx` (`activeNav === 'grading'`) |
| `GET /api/labs/{labId}/statistics` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/submissions` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/students/{studentId}/attempts` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{challengeId}/students` | `LecturerDashboard.jsx` |
| `GET /api/labs/{labId}/challenges/{challengeId}/class?studentId=` | `LecturerDashboard.jsx` (drawer) |
| `GET /api/labs/{labId}/challenges/{challengeId}/mmd?studentId=` | `LecturerDashboard.jsx` (drawer) |
| `GET /api/analytics/student/{studentId}` | `LecturerDashboard.jsx` (Grading tab row selection) |
| `PATCH /api/lecturer/labs/{labId}/deadline` | `SolutionManagement.jsx` — **Save deadline** / **Clear deadline** for the selected lab (date picker does not persist until Save) |
| `PATCH /api/lecturer/labs/{labId}/student-access` | `SolutionManagement.jsx` — **Visible to students** toggle, optional **Release date**, **Save student access** / **Clear release date** |

Upload (`POST /api/submissions/{labId}/{attemptNumber}/upload`) is called from `DropZone.jsx`, not directly from pages.

## Work Guidance

- Pages compose `AppShell` (layout), child components, and local state
- `LoginUI.jsx` shows field validation after a Sign In attempt or after a field loses focus (`touchedFields`); auth API failures use `readFriendlyAuthError` from `frontend/src/utils/apiError.js` (never raw backend `detail` text). Google 403 opens first-time setup; Google 423 is inactive and stays on the login form.
- `ForgotPasswordUI.jsx` and `ResetPasswordUI.jsx` use the same touched/submit gating as `LoginUI.jsx` for inline field errors
- Persist actions (users, terms, lab structure, testcases, deadline, student access, change password, first-time setup, forgot/reset password) show a shared **Toast** via `useToast()`: success (`Saved successfully.` or a specific save line) or fail (friendly `toFriendlyError`). Do not use `window.alert` for these.
- `UserManagement.jsx` normalizes backend field names (`fullName`/`fullname`, `studentCode`/`irn`); Add/Edit modal shows field errors only after blur or save attempt; Lecturer or dual-role users collect Lecturer ID only (no Student IRN field)
- When replacing mock data, update the relevant page and its child component docs
- Student history: `GET /api/submissions/my-history` and `GET /api/submissions/my-labs` via `StudentHistoryPage.jsx`
- Term Excel import: `frontend/src/utils/studentImport.js` finds Student ID / IRN / IRD, Email, and optional Fullname columns anywhere in the sheet; Terms drop zone accepts drag/drop or click; `isSpreadsheetFile` lives in that util; import results that skip unknown students keep a **Details** list on the Terms page
- Lecturer suspend/restore: `POST /api/users/{id}/suspend` and `POST /api/users/{id}/unsuspend` from `UserManagement.jsx` (student-only row action) and `TermManagement.jsx` roster

## Verification

- Manual role-based navigation after login
- Lecturer user CRUD round-trip
- Student lab sidebar list populated from API; click selects the lab for upload/results
- Lecturer Terms: create year + term, set current, enroll/remove students, import Excel by IRN or email (drag/drop or click); import warnings open a popup with **Show details** (not in system vs already enrolled); search available and enrolled rosters; suspend/restore student-only accounts from the roster
- Lecturer Users: suspend/restore student-only accounts; suspended students cannot log in
- Lecturer Solution Management: pick a lab in Structure, choose a date in the Flatpickr calendar (`DatePicker`), click **Save deadline** (or **Clear deadline**); requires backend CORS `PATCH`.

## Child DOX Index

No child docs. Component details live under `src/components/*/AGENTS.md`.
