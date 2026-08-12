# UI Primitives

## Purpose

Reusable, role-agnostic UI building blocks shared across lecturer and student flows.

## Ownership

| File | Role |
|---|---|
| `SortableTableHeader.jsx` | Clickable table header with dual-chevron sort affordance (inactive faded pair; active direction highlighted) |
| `Button.jsx` | Styled button variants |
| `Card.jsx` | Container card wrapper |
| `Select.jsx` | Dropdown select |
| `ScorePill.jsx` | Colored score badge (`ScorePill`, `ScoreSectionHeader`) for MMD/Class/Testcase headers |
| `DropZone.jsx` | Folder drag/drop upload with backend integration |
| `Toast.jsx` | Fixed viewport toast (`success` / `error`), auto-dismiss (default 3s) |

## Local Contracts

### DropZone upload contract

- Accepts props: `labId`, `attemptNumber`, `authToken`, `onUploadComplete` (and styling props)
- Client-side filter: only `.mmd` and `.java` files
- Builds `FormData` with `files` entries; each entry uses `webkitRelativePath` as the multipart filename (preserves folder structure for backend challenge detection)
- Endpoint: `POST /api/submissions/{labId}/{attemptNumber}/upload`
- Header: `Authorization: Bearer ${authToken}`
- Errors surfaced in-component (`uploadError`); API `ErrorResponse.message` is parsed from JSON (e.g. compile diagnostics)

### Button, Card, Select

- Tailwind-styled, dark-mode aware via parent `dark` class on `<html>`
- No external state management — controlled via props

## Work Guidance

- Keep primitives generic — no role-specific logic here
- Upload contract must stay aligned with `SubmissionStorageService` folder layout on the backend
- New shared widgets belong in this folder, not in role-specific component folders

## Verification

- Manual: drag a folder with `challenge_1/*.java` and `.mmd` files; confirm upload succeeds with valid JWT

## Child DOX Index

No child docs.
