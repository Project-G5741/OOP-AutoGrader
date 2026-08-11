# Student Components

## Purpose

Student-specific UI: submission history, profile editing. Also reused by lecturer for profile modal.

## Ownership

| File | Role |
|---|---|
| `StudentHistoryPage.jsx` | Expandable history table; live `my-history` / `my-labs` APIs |
| `ChangePasswordModal.jsx` | Change-password modal — used by both student and lecturer dashboards via Header `editProfile` |

## Local Contracts

### StudentHistoryPage

- Fetches `GET /api/submissions/my-history` (optional `labId`) and `GET /api/submissions/my-labs`
- Filter by lab name via dropdown; client-side table sort
- Expanded rows show challenge-level results only
- Row status from overall score: `failed` (&lt; 50), `partial` (50–80), `passed` (&gt; 80), `unknown` (no score)

### ChangePasswordModal

- Opened via `Header` `editProfile` command
- Shared across `StudentDashboard` and `LecturerDashboard`
- Client validation via `frontend/src/utils/validation.js` (password length, confirm match, new ≠ current)

### Upload inputs (from parent page)

`StudentDashboard.jsx` passes to `DropZone`:

- `labId` — from selected lab in `GET /api/labs` response
- `attemptNumber` — `totalSubmissions + 1` from backend stats / upload response
- `authToken` — from `user.accessToken`

After upload, `StudentDashboard` caches `lab_result` per challenge (keyed by `challengeNumber` from `GET /api/labs/{id}/challenges`) and populates Class/MMD/Testcase tabs without follow-up `/class` or `/mmd` fetches. History view still uses read endpoints when no cached bundle exists.

### Testcase tab rows (`StudentUI.jsx`)

- **I/O Score** header uses backend pillar score from `lab_result.scores.testcase`.
- **Example Testcases** (`is_hidden = false`): full-width expandable rows with Input / Expected Output / Your Output; expand on pass and fail.
- **Other Testcases** (`is_hidden = true`): two-column grid with lock icon and PASS/FAIL only — no I/O detail.
- Multi-assertion visible rows stack additional Expected/Your pairs under the primary three-column panel.

### Dashboard stats row (`StudentUI.jsx`)

- Stats are lab-scoped; `StudentDashboard` clears attempt/latest on lab change and reloads them from `GET /api/labs/{labId}/stats` (grade is not loaded from this API).
- **Total Submissions** and **Latest Submission** always reflect DB history for the selected lab.
- **Current Grade** follows the same session-reveal rule as challenge sidebar scores: `--/--` until the student completes an upload in the current browser session for that lab; then shows the score from the upload response. Switching labs resets the grade until that lab is uploaded again in-session.

## Work Guidance

- Student history uses live APIs in `StudentHistoryPage.jsx`
- Profile modal changes affect both roles — test both dashboards
- `attemptNumber` is derived from backend `totalSubmissions` after each upload

## Verification

- Manual: log in as student, toggle history view, open profile modal
- Upload: select lab, drop challenge folder, confirm API response

## Child DOX Index

No child docs.
