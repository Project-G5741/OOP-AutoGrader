# UI Primitives

## Purpose

Reusable, role-agnostic UI building blocks shared across lecturer and student flows.

## Ownership

| File | Role |
|---|---|
| `SortableTableHeader.jsx` | Clickable table header with dual-chevron sort affordance (inactive faded pair; active direction highlighted); optional `after` slot is inline on the same text line as the label |
| `Button.jsx` | Styled button variants |
| `Card.jsx` | Container card wrapper |
| `Select.jsx` | Dropdown select |
| `ScorePill.jsx` | Colored score badge (`ScorePill`, `ScoreSectionHeader`) for MMD/Class/Testcase headers |
| `DropZone.jsx` | Folder drag/drop upload with backend integration |
| `Toast.jsx` | Fixed viewport toast (`success` / `error`), auto-dismiss (default 3s) |
| `AppLogo.jsx` | App logo from `src/theme/brand.js` — variants: `header`, `login`, `inline` |
| `sidebar.jsx` | shadcn-style `SidebarProvider` / `Sidebar` / `SidebarInset` / `SidebarTrigger`. Desktop offcanvas clips a fixed-width rail (`18rem`) and slides it; inner text does not reflow. |
| `item.jsx` | shadcn-style list `Item` (`ItemTitle`, `ItemDescription`, `ItemMedia`, `ItemActions`) |
| `badge.jsx` | Small status chip (`default`, `secondary`, `outline`, `warning`, `destructive`) |
| `DatePicker.jsx` | Flatpickr calendar (`Y-m-d` value, `d.m.Y` display); themed in `datepicker.css` |
| `cn.js` | Class-name join helper |

## Local Contracts

### DropZone upload contract

- Accepts props: `labId`, `attemptNumber`, `authToken`, `onUploadComplete` (and styling props)
- Shows a one-line **Folder format** hint above the drop zone: `IRN_YourName_lab_n` / (`challenge_1`, `challenge_2`, …)
- Client-side filter: `.mmd`, `.java`, and `.git/**`. Root may include `.git` beside `challenge_*` folders. Do not mention plagiarism to the student.
- Builds `FormData` with `files` entries; each entry uses `webkitRelativePath` as the multipart filename (preserves folder structure for backend challenge detection)
- Endpoint: `POST /api/submissions/{labId}/{attemptNumber}/upload`
- Header: `Authorization: Bearer ${authToken}`
- Errors surfaced in-component (`uploadError`); API failures use friendly messages from `apiError.js` (never raw backend diagnostics)

### Button, Card, Select, DatePicker

- Tailwind-styled, dark-mode aware via parent `dark` class on `<html>`
- No external state management — controlled via props
- `DatePicker` stores ISO `Y-m-d` (API) and shows `d.m.Y`; calendar-only (no free typing of invalid days)

## Work Guidance

- Keep primitives generic — no role-specific logic here
- Upload contract must stay aligned with `SubmissionStorageService` folder layout on the backend
- New shared widgets belong in this folder, not in role-specific component folders

## Verification

- Manual: drag a folder with `challenge_1/*.java` and `.mmd` files; confirm upload succeeds with valid JWT

## Child DOX Index

No child docs.
