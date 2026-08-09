# Student Components

## Purpose

Student-specific UI: submission history, profile editing. Also reused by lecturer for profile modal.

## Ownership

| File | Role |
|---|---|
| `StudentHistoryPage.jsx` | Expandable history table; live `my-history` / `my-labs` APIs |
| `ProfileEditModal.jsx` | Edit profile modal — used by both student and lecturer dashboards |

## Local Contracts

### StudentHistoryPage

- Fetches `GET /api/submissions/my-history` (optional `labId`) and `GET /api/submissions/my-labs`
- Filter by lab name via dropdown; client-side table sort
- Expanded rows show challenge-level results only
- Row status from overall score: `failed` (&lt; 50), `partial` (50–80), `passed` (&gt; 80), `unknown` (no score)

### ProfileEditModal

- Opened via `Header` `editProfile` command
- Shared across `StudentDashboard` and `LecturerDashboard`

### Upload inputs (from parent page)

`StudentDashboard.jsx` passes to `DropZone`:

- `labId` — from selected lab in `GET /api/labs` response
- `attemptNumber` — `totalSubmissions + 1` from backend stats / upload response
- `authToken` — from `user.accessToken`

After upload, `StudentDashboard` caches `lab_result` per challenge (keyed by `challengeNumber` from `GET /api/labs/{id}/challenges`) and populates Class/MMD/Testcase tabs without follow-up `/class` or `/mmd` fetches. History view still uses read endpoints when no cached bundle exists.

## Work Guidance

- Student history uses live APIs in `StudentHistoryPage.jsx`
- Profile modal changes affect both roles — test both dashboards
- `attemptNumber` is derived from backend `totalSubmissions` after each upload

## Verification

- Manual: log in as student, toggle history view, open profile modal
- Upload: select lab, drop challenge folder, confirm API response

## Child DOX Index

No child docs.
