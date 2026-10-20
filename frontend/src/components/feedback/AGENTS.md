# Feedback components

## Purpose

Signed-in web Feedback FAB and submit form. Lecturer inbox lives in `pages/FeedbackInbox.jsx`.

## Ownership

| File | Role |
|---|---|
| `FeedbackFab.jsx` | Viewport-pinned control (`fixed` bottom-right `z-[90]`) |
| `FeedbackForm.jsx` | Bug vs Contribution, disabled gray IRN/name, message, optional files |

## Local Contracts

- Mounted from web `AppShell` only. Desktop mode (`VITE_APP_MODE === 'desktop'`) must not render the FAB.
- Form posts `POST /api/feedback/create` multipart (`type`, `message`, optional `files`).
- Allowed suffixes: `.png`, `.jpg`, `.jpeg`, `.java`, `.mmd`. Client caps match the API (5 files, 25 MB total per upload).
- Semantic theme classes only (`bg-primary`, `bg-surface`, `text-error-text`, …).

## Work Guidance

## Verification

Manual: FAB on student and lecturer shells; absent on login and desktop build.

## Child DOX Index
