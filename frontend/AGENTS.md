# Frontend

## Purpose

React 18 + Vite 7 SPA for the OOP AutoGrader: Google/IRN login, role-based dashboards (lecturer and student), user management, and lab submission upload.

## Ownership

- Dev server: port `5173` (`vite.config.js`)
- Build output: `dist/` (gitignored)
- Deployment: Vercel — see `DEPLOY_VERCEL.md`

## Local Contracts

### Stack

- React 18, Vite 7, Tailwind CSS 3 (`darkMode: 'class'`)
- `react-router-dom` installed but **not used** — navigation is conditional rendering + local state
- `@react-oauth/google` for Google sign-in
- `lucide-react` for icons

### Environment

Copy `frontend/.env.example` to `frontend/.env`:

| Variable | Purpose |
|---|---|
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `VITE_API_URL` | Backend base URL (default `http://localhost:8002`) |

`VITE_*` vars are baked in at build time. `App.jsx` has a hardcoded fallback Google client ID.

### Run

- `npm run dev` from `frontend/`
- Root orchestration: `npm run frontend` or `npm start` (both frontend + backend)

### Auth and session

- No `AuthContext` — auth state lives in `App.jsx` `useState` + `sessionStorage`
- Keys: `accessToken`, `user` (JSON with `roles` array)
- Role gate in `App.jsx`: `LECTURER` → `LecturerDashboard`, `STUDENT` → `StudentDashboard`
- `GoogleOAuthProvider` wraps the app in `App.jsx`

### API integration

- Every caller repeats: `const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002'`
- Native `fetch` only — no shared client, no interceptors
- No Vite proxy — backend must allow CORS for frontend origin
- Upload endpoint requires `Authorization: Bearer <token>` header

### Theme

- `ThemeContext` in `src/context/ThemeContext.jsx` — toggles `dark` class on `<html>`
- `ThemeProvider` mounted in both `main.jsx` and `App.jsx` (duplicate)
- Login pages (`LoginUI`, `FirstTimeSetupUI`) use local dark state instead of `ThemeContext`

### Mock vs live data

| Area | Source |
|---|---|
| Auth, user CRUD, labs list, student upload | Live API |
| Lecturer dashboard submissions | Hardcoded in `LecturerDashboard.jsx` |
| Student history and stats | Hardcoded in `StudentHistoryPage.jsx` / `StudentDashboard.jsx` |
| Submission management (lecturer) | Local mock in `SubmissionManagement.jsx` |

## Work Guidance

- New screens go in `src/pages/`; reusable widgets in `src/components/`
- Match existing Tailwind utility patterns; login screens use custom CSS (`LoginUI.css`)
- Pass `user` and `onLogout` props from `App.jsx` down to dashboards
- When wiring new API calls, follow existing `fetch` + `API_BASE` pattern until a shared client is extracted
- `attemptNumber` is currently hardcoded to `1` in `StudentDashboard.jsx`

## Verification

- `npm run build` must succeed
- Manual: login flow (Google + IRN), role dashboards, user CRUD, student upload via `DropZone`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/pages/AGENTS.md` | Screen orchestration, navigation model, page-to-API mapping |
| `src/components/ui/AGENTS.md` | Shared UI primitives and upload contract |
| `src/components/lecturer/AGENTS.md` | Lecturer grading dashboard widgets |
| `src/components/student/AGENTS.md` | Student history, profile, upload inputs |
