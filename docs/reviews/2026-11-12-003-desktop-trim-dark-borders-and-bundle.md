---
date: 2026-11-12
branch: feat/submission-retention-keep-lab-best
base_commit: 1367ba3
prerequisite_review: docs/reviews/2026-11-12-002-session-handoff-ui-and-ot-fix.md
status: completed
---

# Session Summary — Desktop Feature Trim, Dark-Theme Borders, `.gitignore`, & Practice Zip

## 1. Desktop UI Feature Trimming (`VITE_APP_MODE === 'desktop'`)

Web behavior (`npm run build`) is unchanged. On desktop (`npm run build:desktop`):

- **Profile menu removed → Exit button only** (`frontend/src/components/Header.jsx`):
  - Replaced the account avatar button and profile dropdown (`fullName`, `email`, `ID`, `username`, `Logout`) with a direct **Exit** button calling `onLogout` (`quitPracticeApp` → `practice-quit` IPC).
  - Suppressed the fallback `user?.fullName` header pill and skipped attaching the dropdown pointer-down listener when `IS_DESKTOP_APP` is true.
- **Ranking, Shop, and Streak omitted** (`frontend/src/pages/StudentDashboard.jsx`, `frontend/src/App.desktop.jsx`):
  - Confirmed Ranking nav/routes, `StudentStreakFlame`, `MascotShop`, header shop points button, and streak/pet/restore API calls are omitted on desktop (default fox mascot with local click reactions remains).
- **Deadline notices & notification bell removed** (`frontend/src/components/student/StudentUI.jsx`, `frontend/src/components/student/StudentLabSidebar.jsx`, `frontend/src/pages/StudentDashboard.jsx`):
  - Hidden `StudentNotificationBell` (notify icon + notification dropdown) on desktop.
  - Hidden selected-lab `formatLabDeadlineMeta` text in `StudentUI` header and deadline subtitle (`due ...` / `No deadline`) + countdown/`Expired` badges in `StudentLabSidebar`.
  - Skipped the 60s `now` clock interval in `StudentUI` and skipped `fetchLabSummaries` (`GET /api/submissions/my-labs?scope=current`) in `StudentDashboard` on desktop.

---

## 2. Dark-Theme Border Fix (Web + Desktop)

### Root Cause
1. `frontend/tailwind.config.js` mapped semantic colors to `var(--<token>)` hex values without `<alpha-value>`. In Tailwind CSS v3, opacity utilities such as `border-error/40`, `border-primary/30`, `border-error/30`, `border-warning/40`, and `border-success/35` were not emitted at all, causing elements to fall back to `@tailwind base` (`border-color: #e5e7eb`, bright light-gray/white in dark mode).
2. Bare `border` utilities without a color class also fell back to `#e5e7eb`.
3. `frontend/src/components/ThemeToggle.css` used `--toggle-border: var(--text-muted)` (`#94A3B8` in dark mode), producing a bright light-gray ring and shadow on dark surfaces.

### Fixes Applied
- `frontend/scripts/generate-theme-css.mjs` & `frontend/src/theme/tokens.generated.css`: emit `--<token>-rgb: R G B` channel variables alongside `--<token>: #hex` for both `:root` and `.dark`.
- `frontend/tailwind.config.js`: updated `cssVar(name)` to `rgb(var(--${name}-rgb) / <alpha-value>)` so all `/opacity` modifiers (`border-*`, `bg-*`, `ring-*`, etc.) resolve to tinted semantic colors.
- `frontend/src/index.css`: added `@layer base { *, ::before, ::after { border-color: var(--border); } }`.
- `frontend/src/components/ThemeToggle.css`: changed `--toggle-border` from `var(--text-muted)` to `var(--border)`.
- `frontend/src/components/ui/badge.jsx`: changed `navy` dark border from `dark:border-navy-text/20` to `dark:border-navy-hover/60`.

---

## 3. `.gitignore` Cleanup

Updated root `.gitignore` to ignore:
- **Desktop build & local runtime artifacts**: `dist-desktop/`, `StudentPractice/`, `OOP-AutoGrader-Practice.zip`, `*.agpack`, `*.WebView2/`, `*.mv.db`, `*.trace.db`, `desktop-pack-fingerprint.txt`
- **Build outputs, caches, logs & temp files**: `*.class`, `*.jar` (with `!**/maven-wrapper.jar`), `.vite/`, `.eslintcache`, `coverage/`, `*.log`, `hs_err_pid*.log`, `replay_pid*.log`, `*.tmp`, `tmp/`
- **OS & IDE files**: `.DS_Store`, `Thumbs.db`, `desktop.ini`, `.idea/`, `*.iml`, `.vscode/`, `*.swp`, `*.swo`, `*~`

---

## 4. DOX Documentation Updated

- `frontend/AGENTS.md`: documented direct **Exit** header button, omitted deadline labels/badges and `StudentNotificationBell` on desktop, `--<token>-rgb` generation, and `ThemeToggle.css` `--border`.
- `frontend/src/pages/AGENTS.md`: updated desktop header contract (direct **Exit** button instead of profile/account menu).
- `frontend/src/components/student/AGENTS.md`: documented desktop omission of `StudentNotificationBell`, `labSummariesById` fetch, and sidebar/header deadline text & badges.
- `docs/DESKTOP_STUDENT_DIST.md`: updated header **Exit** behavior and omitted desktop features list.

---

## 5. Verification & Desktop Zip Packaging

- **Frontend builds** (`cd frontend && npm run build && npm run build:desktop`):
  - Exited `0`; generated `frontend/dist/` and `frontend/dist-desktop/`.
  - Verified compiled CSS in `frontend/dist/assets/index-DPKj4lVt.css` and `frontend/dist-desktop/assets/index-DPKj4lVt.css` contains `.border-error/30`, `.border-error/35`, `.border-error/40`, `.border-primary/30`, `.border-success/35`, `.border-warning/40` using `rgb(var(--...-rgb) / ...)` and `@layer base` `border-color: var(--border)`.
- **Backend desktop jars** (`cd backend && mvn -q -DskipTests package -Pdesktop-dist`):
  - Exited `0`; built `backend/target/backend-1.0.0-desktop.jar` (59M) and `backend/target/backend-1.0.0-worker.jar` (2.3M).
- **Windows desktop zip (`backend/target/OOP-AutoGrader-Practice.zip`, 103M)**:
  - Because `cargo` / Windows MSVC Build Tools are not installed on this Linux machine, `OOP-AutoGrader-Practice.exe` (1,479,168 bytes) and Windows `runtime/jdk/` (including `runtime/jdk/bin/java.exe`) were pulled from the repository's official `practice-bundle-v12` release (matching the latest `desktop/launcher/` commit `6db2620`) and repackaged with the newly built `backend.jar`, `worker.jar`, `ui/dist-desktop/`, `OOP-AutoGrader-Practice.bat`, `README.txt`, `rubric/`, and `data/` (leftover `BOOT-INF/` removed; verified with `unzip -l`).
