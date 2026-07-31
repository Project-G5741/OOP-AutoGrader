# Student Components

## Purpose

Student-specific UI: submission history, profile editing. Also reused by lecturer for profile modal.

## Ownership

| File | Role |
|---|---|
| `StudentHistoryPage.jsx` | Expandable history table with filters (mock data) |
| `ProfileEditModal.jsx` | Edit profile modal — used by both student and lecturer dashboards |

## Local Contracts

### StudentHistoryPage

- Receives no API props — uses hardcoded `HISTORY` constant
- Filter by lab name, expandable rows for attempt details
- Planned replacement: `GET /api/submissions/mine?labId=...` (not implemented)

### ProfileEditModal

- Opened via `Header` `editProfile` command
- Shared across `StudentDashboard` and `LecturerDashboard`

### Upload inputs (from parent page)

`StudentDashboard.jsx` passes to `DropZone`:

- `labId` — from selected lab in `GET /api/labs` response
- `attemptNumber` — hardcoded to `1`
- `authToken` — from `user.accessToken`

## Work Guidance

- When student history API exists, fetch in `StudentDashboard.jsx` or `StudentHistoryPage.jsx` and remove mock `HISTORY`
- Profile modal changes affect both roles — test both dashboards
- `attemptNumber` logic will need backend support for multiple attempts per lab

## Verification

- Manual: log in as student, toggle history view, open profile modal
- Upload: select lab, drop challenge folder, confirm API response

## Child DOX Index

No child docs.
