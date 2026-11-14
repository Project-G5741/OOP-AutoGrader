---
date: 2026-11-12
plan: docs/plans/2026-11-12-001-feat-ui-corporate-edtech-3d-motion-plan.md
verdict: complete
---

# Review — Corporate EdTech UI Overhaul, Color Theory & 3D/Lottie Motion

## Summary of Changes

- **Color Theory & Brand Tokens (`src/theme/tokens.js`, `tailwind.config.js`, `src/index.css`)**:
  - Applied Corporate EdTech 60-30-10 hierarchy: 60% architectural neutrals (`#F8FAFC`/`#FFFFFF`/`#CBD5E1` Light, `#0B1220`/`#111A2E`/`#2A3956` Dark), 30% Institutional Navy (`#1E3A5F` / `#23456E`) + Supporting Teal (`#0D9488`), and 10% Interactive Cobalt CTA (`#2563EB`), Academic Gold (`#D97706` / `#F59E0B`), semantic status colors, and ≤ 10% AI Purple (`#7C3AED` / `#8B5CF6`).
  - Removed Dark-mode CSS rules that wiped borders and shadows on surfaces; calibrated `.shadow-stage-1/2/3` across Light and Dark themes.
  - Added `.perspective-1000`, `.preserve-3d`, `.backface-hidden`, and `@media (prefers-reduced-motion: reduce)` guards.
- **Bundled Typography, Lottie Visuals & 3D Primitives**:
  - Bundled `@fontsource/plus-jakarta-sans` (400–800) and `@fontsource/jetbrains-mono` (400–600) with zero external Google Fonts CDN calls; wrapped app in `<MotionConfig reducedMotion="user">` (`src/main.jsx`).
  - Created offline CSP-safe SVG Lottie wrapper (`LottieAnimation.jsx` using `lottie-web/build/player/lottie_light`) and four vector Lottie scenes (`oop-stage.json`, `grading-engine.json`, `upload-cloud.json`, `empty-orbit.json`).
  - Created `MotionPrimitives.jsx` (`StageCard3D`, `AnimatedEntrance`, `StaggerGroup`, `StaggerItem`, `AnimatedCounter`, `EmptyState`, `GradingActivityVisual`).
  - Combined `lucide-react` actions with `@phosphor-icons/react` Duotone/Fill iconography across KPI wells, podium badges, alerts, report pillars, and notification triggers.
- **Zero-Gradient & Flat Single-Card Discipline**:
  - Removed all CSS/Tailwind gradients across Auth/System screens (`LoginBackground`, `LoginUI.css`, `MaintenancePage.css`), `StudentStreakFlame.css`, and UI cards.
  - Eliminated nested bordered cards across Student Portal (`StudentUI`, `StudentHistoryPage`, `StudentRankingPage`, `RankingBoard`, `DropZone`), Auth (`LoginUI`, `FirstTimeSetupUI`), Lecturer Dashboard (`DashboardSection`, `OverviewPanel`, `LecturerDashboard`, `SubmissionTable`, `Reports`, `ReportsPanel`, `BulkGradingPanel`), Admin/System (`UserStats`, `FeedbackInbox`, `TermManagement`).

- **Post-Merge Integration (`origin/main` `1367ba3`) & Follow-up Visual Refinements**:
  - Merged latest `main` (`Midterm` prefix, `@headlessui/react` Combobox/Dialog primitives, stacked multi-`Toast`, `ThemeToggle` `.u038`, `DangerAlert`, `StudentOfflinePracticeDownload`, `TermManagement` roster mail) with full retention of Corporate EdTech tokens, Phosphor Duotone/Fill icons, Framer Motion, and Lottie assets; mapped `Midterm` in `LabPrefixBadge` to the `gold` variant (`Official -> navy`, `Practice -> teal`).
  - Centered the Lottie upload arrow in `frontend/src/assets/lottie/upload-cloud.json` squarely inside the rounded frame (`FolderBase` `[80, 80]` `104x84`, `UploadArrow` oscillating `[80, 80]` <-> `[80, 74]`) so it stays centered within the icon box across all frames.
  - Added crisp hairline borders to Student Operation Test items (`border-border` on `Example Testcases` rows, `border-success/35` and `border-error/35` on `Other Testcases` pills in `StudentUI.jsx`).
  - Decluttered Auth/Signup copy and height proportions in `LoginUI.jsx`, `LoginUI.css`, `FirstTimeSetupUI.jsx`, `PasswordStrengthField.jsx`, and `PasswordStrengthField.css`; unified `LoginUI.jsx` into an evenly balanced 50/50 two-column card (`login-unified-card`) with borderless left hero tint + `Footer` (`variant="login"`) flanked by hairline rules.

## Verification Run

- `cd frontend && npm run theme:sync && npm run build && npm run build:desktop` — exited `0` (verified in terminal).
- Static audits on `frontend/src/` and `frontend/index.html` — 0 CSS/Tailwind gradients, 0 raw Tailwind color palette classes, 0 external Google Fonts URLs, and 100% valid `@phosphor-icons/react` exports.
- Headless Google Chrome CDP inspection across Light/Dark themes, Desktop (`1440px`) and Mobile (`375px`), Auth flows, `/rate-limited`, and `dist-desktop/` offline student bundle.

## Notes

- Student and Lecturer authenticated dashboards share the inspected theme tokens, typography, and single-level stage card primitives (`StageCard3D`, `DashboardSection`, `ReportsPanel`, `SubmissionTable`, `BulkGradingPanel`), while full authenticated portal end-to-end visual walkthrough requires a running Spring Boot backend + PostgreSQL instance.
