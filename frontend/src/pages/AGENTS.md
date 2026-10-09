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
| `StudentDashboard.jsx` | Student shell: lab sidebar, upload, stats; toggles history; Operation Test when OT applies; [page-mascot](https://koboyo.com/page-mascot) fox at the header’s top-right on submit and history views |
| `StudentHistory.jsx` | Thin wrapper → `StudentHistoryPage.jsx` |
| `NoAccessPage.jsx` | Signed-in landing for gated API 403 |
| `UserManagement.jsx` | User CRUD (live API) |
| `TermManagement.jsx` | Lecturer term year create, optional copy labs from current quarter, sync labs into selected quarter (from all other quarters), current-term flag, student enrollment, Excel import |
| `SubmissionManagement.jsx` | Solution/lab structure + operational testcase authoring (`SolutionManagement.jsx` → `/api/lecturer/labs`; Copy lab from previous current quarter) |

## Local Contracts

### Top-level navigation (URL routes)

`App.jsx` gates by stored user roles (`localStorage` via `readStoredUser`) and React Router paths:

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
| `dashboard` | Grading overview, challenge tabs, `SubmissionTable`, export drawers. Overview cards open `OverviewDetailDialog` from the overview payload | First paint: `GET /api/lecturer/bootstrap/dashboard` (fresh overview). Secondary: labs list, `/api/labs/{id}/statistics`, submissions, plagiarism flags |
| `score` | Cross-lab `GradeOverviewTable` + Export + row-click submission history | First paint: `GET /api/lecturer/bootstrap/score`. Sort/search/page: `GET /api/lecturer/grade-overview` |
| `grading` | Bulk grading (`BulkGradingPanel`) | First paint: `GET /api/lecturer/bootstrap/grading` (labs list) |
| `users` | `UserManagement` | First paint: `GET /api/lecturer/bootstrap/users` |
| `terms` | `TermManagement` | First paint: `GET /api/lecturer/bootstrap/quarters`; roster secondary |
| `projects` | `SolutionManagement` | First paint: `GET /api/lecturer/bootstrap/solution` (lookups + labs + default structure); loading copy **Loading data...** |
| `reports` | `Reports.jsx` | First paint: `GET /api/lecturer/bootstrap/reports` (fresh analytics; at-risk labs deduped per lab) |

Visit-scoped prefetch: `frontend/src/utils/lecturerBootstrapStore.js` + NavBar hover/idle (survives route remount).

### Student in-dashboard sections

| State | Renders | API |
|---|---|---|
| `showHistory === false` | Main dashboard (left lab list + right upload/stats/results) | Full-page spinner waits only for `GET /api/labs/list` (`inCurrentTerm`). That payload includes per-lab `challenges` and `totalSubmissions` / `latestSubmission`, so Challenges and stats populate without follow-up `.../challenges/list` or `/stats` unless those fields are missing. Also `GET /api/submissions/my-labs?scope=current` for notifications; **after upload, challenges/stats/my-labs are not refetched for that lab** — scores and attempt counts come from the upload payload. **Current Grade** and challenge scores + class/MMD detail only after upload in session; success **Toast** on grading complete |
| `showHistory === true` | `StudentHistoryPage` | Live `my-history` + `my-labs` (all quarters, `termLabel` on each lab/submission) |

### Header commands (`Header.jsx` → `onCommand`)

Shared: `home`, `history`, `changePassword` (opens `ChangePasswordModal`). Lecturer shell hides **History** in the avatar menu (`hideHistory`); student shell may hide **Home** when out of term (`hideHome`). Desktop practice build hides **Change Password** (`hideChangePassword`) — local synthetic session has no password change API.

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
| `PUT /api/users/updateUser/{id}` | `UserManagement.jsx` — body: `roleNames`, `studentCode`, `teacherCode`, optional `password` |
| `DELETE /api/users/deleteUser/{id}` | `UserManagement.jsx` |
| `POST /api/users/{id}/suspend` | `UserManagement.jsx`, `TermManagement.jsx` — student-only; blocks login |
| `POST /api/users/{id}/unsuspend` | `UserManagement.jsx`, `TermManagement.jsx` — restores login |
| `GET /api/labs/list` | `StudentDashboard.jsx` (student JWT; current-term labs only, with embedded challenges + attempt stats; skipped when out of term) |
| `GET /api/students/term-access` | `StudentDashboard.jsx` |
| `GET /api/lecturer/terms/list` | `TermManagement.jsx` |
| `GET /api/lecturer/terms/{termId}/roster` | `TermManagement.jsx` — enrolled + available students |
| `POST /api/lecturer/terms/{termId}/students/import` | `TermManagement.jsx` — body `{ rows: [{ studentCode, email, fullName }] }` parsed from Excel; warning popup + **Show details** uses `notFoundStudents` and `alreadyInTermStudents` |
| `DELETE /api/lecturer/terms/{termId}` | `TermManagement.jsx` — delete non-current quarter (must have no labs) |
| `GET /api/lecturer/labs/clone-sources` | `SolutionManagement.jsx` — previous-current quarter labs for Copy lab |
| `POST /api/lecturer/labs/clone` | `SolutionManagement.jsx` — body `{ sourceLabIds, targetTermId }` deep-copies rubric + OT |
| `GET /api/lecturer/terms/{termId}/sync-labs` | `TermManagement.jsx` — labs from all other quarters (with `termLabel`) |
| `POST /api/lecturer/terms/{termId}/sync-labs` | `TermManagement.jsx` — body `{ sourceLabIds }` deep-copies into selected quarter |
| `GET /api/labs/{labId}/challenges/list?studentId=` | `StudentDashboard.jsx` |
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
| `PATCH /api/lecturer/labs/{labId}/student-access` | `SolutionManagement.jsx` — **Visible to students** toggle and **Save access** |

Upload (`POST /api/submissions/{labId}/{attemptNumber}/upload`) is called from `DropZone.jsx`, not directly from pages.

## Work Guidance

- Pages compose `AppShell` (layout), child components, and local state
- `LoginUI.jsx` shows field validation after a Sign In attempt or after a field loses focus (`touchedFields`); auth API failures use `readFriendlyAuthError` from `frontend/src/utils/apiError.js` (never raw backend `detail` text). Google 403 opens first-time setup; Google 423 is inactive and stays on the login form.
- `ForgotPasswordUI.jsx`, `ResetPasswordUI.jsx`, and `FirstTimeSetupUI.jsx` use touched-field gating (inline errors after blur) like `LoginUI.jsx`; login and forgot/reset also set errors on submit attempt
- Persist actions (users, terms, lab structure, testcases, deadline, student access, change password, first-time setup, forgot/reset password) show a shared **Toast** via `useToast()`: success (`Saved successfully.` or a specific save line) or fail (friendly `toFriendlyError`). Do not use `window.alert` / `window.confirm` for these — destructive deletes use an in-app confirm dialog (`ModalOverlay`, same pattern as Users delete).
- `UserManagement.jsx` normalizes backend field names (`fullName`/`fullname`, `studentCode`/`irn`); Add/Edit modal shows field errors only after blur or save attempt; Lecturer or dual-role users collect Lecturer ID only (no Student IRN field). Delete confirmation stays open while `DELETE /api/users/deleteUser/{id}` runs: spinner on the trash icon and Delete button, Cancel/backdrop locked, then close on success (error keeps the dialog so the lecturer can retry)
- When replacing mock data, update the relevant page and its child component docs
- Student history: `GET /api/submissions/my-history` and `GET /api/submissions/my-labs` via `StudentHistoryPage.jsx`
- Term Excel import: `frontend/src/utils/studentImport.js` finds Student ID / IRN / IRD, Email, and optional Fullname columns anywhere in the sheet; Terms drop zone accepts drag/drop or click; `isSpreadsheetFile` lives in that util; import results that skip unknown students keep a **Details** list on the Terms page
- Lecturer suspend/restore: `POST /api/users/{id}/suspend` and `POST /api/users/{id}/unsuspend` from `UserManagement.jsx` (student-only row action) and `TermManagement.jsx` roster

## Verification

- Manual role-based navigation after login
- Lecturer user CRUD round-trip
- Student lab sidebar list populated from API; click selects the lab for upload/results
- Lecturer Terms: create year + term, set current, **Sync labs** into the selected quarter from any other quarter, enroll/remove students, import Excel by IRN or email (drag/drop or click); import warnings open a popup with **Show details** (not in system vs already enrolled); search available and enrolled rosters; suspend/restore student-only accounts from the roster
- Lecturer Users: suspend/restore student-only accounts; suspended students cannot log in. Delete User keeps the confirm dialog open with a spinner until the server finishes
- Lecturer Solution Management: pick a lab in Structure, choose a date in the Flatpickr calendar (`DatePicker`), click **Save deadline** (or **Clear deadline**); requires backend CORS `PATCH`.

## Child DOX Index

No child docs. Component details live under `src/components/*/AGENTS.md`.
