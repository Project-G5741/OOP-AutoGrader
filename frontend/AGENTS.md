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
- `react-router-dom` installed; `App.jsx` uses URL routes with role guards (`RequireRole`)
- `@react-oauth/google` for Google sign-in
- `lucide-react` for icons
- `flatpickr` for lecturer date fields (`DatePicker` in `src/components/ui/`)

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
- `user.inCurrentTerm` missing or not `false` counts as enrolled (`isInCurrentTerm` in `authRoutes.js`); student dashboard updates the stored flag via `patchStoredUser`
- Role gate in `App.jsx`: `RequireRole` + URL routes; lecturer-first default dashboard; dual-role users reach student routes by URL
- `GoogleOAuthProvider` wraps the app in `App.jsx`

### API integration

- Every caller repeats: `const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002'`
- Native `fetch` only — no shared client, no interceptors
- No Vite proxy — backend must allow CORS for frontend origin
- Upload endpoint requires `Authorization: Bearer <token>` header

### Theme

- **Edit colors in one file:** `src/theme/tokens.js` (`theme.light` / `theme.dark` — primary, secondary, success, error, warning, surfaces, chart, etc.)
- **Edit logo and app naming in one file:** `src/theme/brand.js` — in-app logo is always the graduation cap (`AppLogo`); tab favicon comes from any image in `public/brand/` (`.png`, `.svg`, `.webp`, …) via `npm run theme:sync`
- After editing tokens, run `npm run theme:sync` (also runs automatically before `dev` / `build`) to regenerate `src/theme/tokens.generated.css`
- Tailwind semantic classes (`bg-primary`, `text-success`, `bg-surface`, …) map to CSS variables — use these in components, never raw `blue-600` / `purple-*` / hex backgrounds
- `ThemeContext` — OS default on first visit, `localStorage` key `oop-theme`, single `ThemeProvider` in `main.jsx`
- Global scrollbar styling in `src/index.css` (thin thumb using `--surface-tertiary`, transparent track) on `html` and overflow containers
- Grading status helpers: `src/theme/statusClasses.js`
- Design reference: `docs/design/color-theory-light-dark-theme.md`

### Mock vs live data

| Area | Source |
|---|---|
| Auth, user CRUD, labs list, student upload | Live API |
| Lecturer dashboard overview, lab statistics, submissions | Live API (`/api/lecturer/overview`, `/api/labs/{id}/statistics`, `/api/labs/{id}/submissions`) |
| Reports page | Live API (`/api/analytics/dashboard`) |
| Student history and stats | Live API via `StudentHistoryPage` (`my-history`, `my-labs`) |
| Term management | Live API (`TermManagement.jsx` → `/api/lecturer/terms`, `GET /{id}/roster`, Excel import by IRN + email; roster **Suspend** / **Restore** via `/api/users/{id}/suspend` and `/unsuspend`) |
| Submission management (lecturer) | Live API (`SolutionManagement.jsx` → `/api/lecturer/labs`, testcase endpoints under `.../challenges/{id}/testcases`) |

## Work Guidance

- New screens go in `src/pages/`; reusable widgets in `src/components/`
- Match existing Tailwind utility patterns; login screens use custom CSS (`LoginUI.css`)
- Pass `user` and `onLogout` props from `App.jsx` down to dashboards
- When wiring new API calls, follow existing `fetch` + `API_BASE` pattern until a shared client is extracted
- Form field validation rules live in `src/utils/validation.js`; use inline errors and disable submit until valid
- API error bodies: `src/utils/apiError.js` — `readFriendlyApiError`, `toFriendlyError`, `friendlyLoadErrorFromResponse`; never surface raw backend `message`/`error`/`detail` to users (login wrong credentials → "IRN or password is wrong"; fetch/network/5xx → "Server Busy")
- Post-upload refresh updates stats cards + challenges sidebar + class panel only (`isRefreshingResults`); lab selector and DropZone stay mounted
- Class tab data is cached per challenge id in memory; switching back to a loaded challenge skips `/class`

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
