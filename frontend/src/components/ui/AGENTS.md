# UI Primitives

## Purpose

Reusable, role-agnostic UI building blocks shared across lecturer and student flows.

## Ownership

| File | Role |
|---|---|
| `Button.jsx` | Styled button variants |
| `Card.jsx` | Container card wrapper |
| `Select.jsx` | Dropdown select |
| `DropZone.jsx` | Folder drag/drop upload with backend integration |

## Local Contracts

### DropZone upload contract

- Accepts props: `labId`, `attemptNumber`, `authToken`, `onUploadComplete` (and styling props)
- Client-side filter: only `.mmd` and `.java` files
- Builds `FormData` with `files` entries; each entry uses `webkitRelativePath` as the multipart filename (preserves folder structure for backend challenge detection)
- Endpoint: `POST /api/submissions/{labId}/{attemptNumber}/upload`
- Header: `Authorization: Bearer ${authToken}`
- Errors surfaced via `alert()` / callback

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
