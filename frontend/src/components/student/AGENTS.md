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
- `attemptNumber` — hardcoded to `1`
- `authToken` — from `user.accessToken`

## Work Guidance

- Student history uses live APIs in `StudentHistoryPage.jsx`
- Profile modal changes affect both roles — test both dashboards
- `attemptNumber` logic will need backend support for multiple attempts per lab

## Verification

- Manual: log in as student, toggle history view, open profile modal
- Upload: select lab, drop challenge folder, confirm API response

## Child DOX Index

No child docs.
