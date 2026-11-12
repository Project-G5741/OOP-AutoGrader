# Corporate EdTech UI Overhaul — Color Theory, Business-Rule Semantics, 3D Motion, Animated Visuals & Icon System

**Date:** 2026-11-12
**Status:** Approved by user for implementation
**Scope:** Frontend (`frontend/`), Design Docs (`design-system/oop-autograder/MASTER.md`, `docs/design/color-theory-light-dark-theme.md`), DOX docs (`frontend/AGENTS.md`, `frontend/src/**/AGENTS.md`). Backend and grading rules remain untouched.

---

## 1. Settled Requirements & Constraints

1. **Full Frontend Coverage**: Upgrade all primary user flows — Auth/System (`Login`, `ForgotPassword`, `ResetPassword`, `FirstTimeSetup`, `Maintenance`, `NoAccess`, `RateLimited`), Shared Shell (`AppShell`, `Header`, `NavBar`, `Footer`), Student Portal (`StudentDashboard`, `StudentHistoryPage`, `StudentRankingPage`, `StudentLabSidebar`, `StudentStreakFlame`, `MascotShop`), and Lecturer/Admin Portal (`LecturerDashboard`, `OverviewPanel`, `SubmissionTable`, `GradeOverviewTable`, `BulkGradingPanel`, `Reports`/`ReportsPanel`, `TermManagement`, `UserManagement`, `SolutionManagement`, `FeedbackInbox`).
2. **Offline / Desktop Parity**: Must compile cleanly for both web (`npm run build`) and Windows WebView2 offline desktop bundle (`npm run build:desktop` / `App.desktop.jsx`). Zero runtime CDN dependencies (fonts, Lottie JSON animations, icons, and assets are 100% locally bundled).
3. **Corporate EdTech Palette & 60-30-10 Color Theory**:
   - **60% Neutral Structure**: Clean slate/alabaster light surfaces and deep slate-navy dark surfaces (`#0B1220` canvas → `#111A2E` card → `#18243C` recessed → `#22304C` elevated, with crisp `#2A3956` dark borders instead of `#0F172A` invisible borders).
   - **30% Institutional Brand**: Corporate Navy (`#1E3A5F` light / `#274B7A` dark) + Supporting Teal (`#0D9488` / `#14B8A6`) for active progression, MMD modeling, and rubric metrics.
   - **10% Semantic Action & Achievement**: Interactive Cobalt (`#2563EB` / `#3B82F6`), Academic Amber/Gold (`#D97706` / `#F59E0B`), Verification Green (`#16A34A` / `#22C55E`), Alert Red (`#DC2626` / `#EF4444`), and limited AI Purple (`#7C3AED` / `#8B5CF6`, strictly ≤ 10%).
   - Contrast ≥ 4.5:1 for normal text and ≥ 3:1 for large text/controls in both Light and Dark modes.
4. **Strict Zero-Gradient Rule**: Eliminate every `linear-gradient`, `radial-gradient`, `conic-gradient`, and Tailwind `bg-gradient-*` / `from-*` / `via-*` / `to-*` across the entire frontend. Use solid flat corporate tonal layers, architectural geometry, hairline borders, and crisp Z-elevation shadows.
5. **Strict Zero-Nested-Cards Rule ("không card lồng card")**: Never nest a bordered/rounded card inside another bordered/rounded card. Section wrappers are borderless layout containers (`DashboardSection`, `Reports.jsx`), and items inside a single card use flush `border-b` dividers or flat tinted rows (`bg-surface-secondary`) rather than inner bordered boxes.
6. **Combined Icon Architecture**:
   - **`lucide-react`**: Functional UI controls, navigation, toolbars, table sorting, forms.
   - **`@phosphor-icons/react` (Duotone / Fill)**: KPI summary cards, 3 grading pillars (Declaration, MMD Diagram, Operational Testcases), podium/rank tiers, domain empty states, business status indicators.
   - **Local Animated SVG/Motion Icons**: Interactive upload dropzone, streak flame, notification bell pulse, live grading/verification feedback.
7. **3D Motion & Animated Visuals (Option B)**:
   - CSS `perspective` + `transform-style: preserve-3d` architectural stages and subtle interactive pointer/hover 3D card tilt (`StageCard3D`).
   - `framer-motion` (`<MotionConfig reducedMotion="user">`) for spring transitions, shared-element `layoutId` active navigation/tab pills, modal/drawer Z-depth entrances, staggered KPI reveals, and animated bar charts.
   - Locally bundled `lottie-web/build/player/lottie_light` + compact local vector Lottie JSON animations (no `eval()` / expression engine, CSP & desktop safe) for Login 3D stage visual, active compiling/grading visual, empty states, and achievement cues.
8. **Business Rule Preservation**: All domain rules from `CONCEPTS.md` remain exact (`Math.floor` score formatting, Student Latest vs Lecturer Highest, Official vs Practice `LabPrefixBadge`, 3→2→1 day deadline urgency, Plagiarism `ORIGINAL` amber vs `PLAGIARIZER` red, Daily Streak + Mascots, Desktop exclusions).

---

## 2. File-by-File Implementation Map

### Step A — Packages & Build Verification
Install in `frontend/`:
- `@phosphor-icons/react`
- `framer-motion` (verified React `^18.2.0` peer compatibility)
- `lottie-web` (using CSP-safe `lottie-web/build/player/lottie_light` sub-export)
- `@fontsource/plus-jakarta-sans` & `@fontsource/jetbrains-mono` (bundled offline fonts matching Corporate EdTech typography)

### Step B — Token System, Dark Mode Parity & Typography
- `frontend/src/theme/tokens.js`:
  - Add institutional Corporate EdTech tokens: `navy`, `navy-hover`, `navy-light`, `navy-text`, `gold`, `gold-bg`, `gold-text`.
  - Calibrate **Light** palette (`background: #F8FAFC`, `surface: #FFFFFF`, `surface-secondary: #F1F5F9`, `surface-tertiary: #E2E8F0`, `border: #CBD5E1`, `border-subtle: #E2E8F0`).
  - Fix **Dark** palette contrast & surface hierarchy (`background: #0B1220`, `surface: #111A2E`, `surface-secondary: #18243C`, `surface-tertiary: #22304C`, `border: #2A3956`, `border-subtle: #1E2D47`, `primary: #3B82F6` with white text contrast or balanced cobalt `#2563EB` + `#60A5FA` text tokens).
  - Run `npm run theme:sync` to regenerate `frontend/src/theme/tokens.generated.css`.
- `frontend/tailwind.config.js`: Expose `navy`, `gold`, font families (`Plus Jakarta Sans`, `JetBrains Mono`), and crisp flat/3D shadow levels (`elevation-1`, `elevation-2`, `elevation-3`, `3d-stage`).
- `frontend/src/index.css`:
  - Remove the blunt dark-mode border/shadow suppression rules that erased `.shadow-*` and `.bg-surface.border-border` borders; replace with dark-calibrated shadows (`rgba(2, 6, 23, 0.55)` + crisp hairline border contrast) while preserving explicit outline button border styles.
  - Add `.perspective-1000`, `.preserve-3d`, `.backface-hidden`, and global `@media (prefers-reduced-motion: reduce)` safeguards.
- `frontend/src/main.jsx`: Import local `@fontsource/plus-jakarta-sans` (400/500/600/700/800) and `@fontsource/jetbrains-mono` (400/500/600), and wrap `<App />` with `<MotionConfig reducedMotion="user">`.
- `frontend/src/theme/statusClasses.js`: Standardize business-rule status pills, Official/Practice tokens, and 3→2→1 day deadline urgency styles using cohesive Light & Dark accessible contrast tokens.

### Step C — Reusable UI, 3D Motion, Icon & Local Lottie Primitives
- `frontend/src/assets/lottie/*.json` + `frontend/src/components/ui/LottieAnimation.jsx`:
  - Lightweight wrapper around `lottie-web/build/player/lottie_light` respecting `useReducedMotion()` and clean destroy-on-unmount.
  - Craft 4 clean vector Lottie JSON assets locally (100% self-contained, small payload):
    1. `oop-stage.json` — Architectural OOP class-nodes & pulse connection animation for Login/Hero 3D stage.
    2. `grading-engine.json` — Precision gears/check-pulse animation for compiling/grading & bulk upload states.
    3. `upload-cloud.json` — Uplift folder/file submission pulse animation for DropZone & empty drop states.
    4. `empty-orbit.json` — Calm architectural document/cube loop for empty states & system notices.
- `frontend/src/components/ui/MotionPrimitives.jsx`:
  - `StageCard3D`: Perspective + pointer-reactive 3D tilt (max ±5 deg, disabled cleanly on touch / reduced-motion) + single-level crisp surface elevation.
  - `AnimatedEntrance` / `StaggerList` / `StaggerItem`: Smooth Y+Z settle transitions.
  - `AnimatedCounter`: Numeric count-up for dashboard KPI totals.
  - `EmptyState`: Standard unified empty-state component with Lottie visual + Phosphor duotone icon fallback + clear action CTA.
- `frontend/src/components/ui/AppLogo.jsx`: Upgraded solid Navy/Cobalt 3D layered emblem with graduation/code precision.
- `frontend/src/components/ui/Button.jsx`, `Card.jsx`, `badge.jsx`, `LabPrefixBadge.jsx`, `ScorePill.jsx`, `Progress.jsx`, `Toast.jsx`, `ModalOverlay.jsx`, `DropZone.jsx`:
  - Tactile micro-lift/press states, zero gradients, smooth animated progress bars, Phosphor duotone icons on Toasts/KPI/DropZone, crisp `ModalOverlay` fade-scale entrance.

### Step D — Auth & System Screens (Zero Gradients + 3D Architectural Stage)
- `frontend/src/components/ui/LoginBackground.jsx` & `frontend/src/pages/LoginUI.css`:
  - Strip all CSS `linear-gradient` and `radial-gradient` declarations.
  - Replace with a Corporate EdTech **3D Architectural Stage**: solid deep Institutional Navy / Slate planes with subtle grid dot matrix, floating `preserve-3d` OOP artifact cards (`Class Diagram`, `Declaration Check`, `Runtime Testcases`), and smooth CSS keyframes respecting `prefers-reduced-motion`.
- `frontend/src/pages/LoginUI.jsx`, `ForgotPasswordUI.jsx`, `ResetPasswordUI.jsx`, `FirstTimeSetupUI.jsx`:
  - Keep single-level crisp `bg-surface` form card, incorporate `LottieAnimation` (`oop-stage.json`) badge in the hero split panel, Phosphor duotone security/account icons, and solid password-strength segments.
- `frontend/src/pages/MaintenancePage.jsx` + `MaintenancePage.css`, `NoAccessPage.jsx`, `RateLimitedPage.jsx`:
  - Strip all radial gradients in `MaintenancePage.css`, frame the 3D graduation-cap stage in a crisp solid architectural halo ring + Navy/Gold badge, and add animated visuals + clear Phosphor duotone state icons to `NoAccessPage` and `RateLimitedPage`.

### Step E — Shared Shell & Student Portal (No Nested Cards + Business Semantics)
- `frontend/src/components/layout/Header.jsx`, `NavBar.jsx`, `AppShell.jsx`, `Footer.jsx`, `ThemeToggle.jsx`:
  - Crisp Institutional Navy accent bar, `framer-motion` `layoutId` active navigation indicator in `NavBar`, animated Sun/Moon swap in `ThemeToggle` & header theme button, interactive Bell badge in `Header`.
  - Flatten `Header` user menu dropdown: remove inner bordered rounded cards inside the menu card (`border-b` dividers + flat `bg-surface-secondary` section strips instead).
- `frontend/src/components/student/StudentStreakFlame.css` & `.jsx`:
  - Replace all 3 CSS gradients (`u379-glow`, `u379-dt.is-on`, `u379-go`) with crisp flat Amber/Coral solid tokens and subtle 3D badge lift.
- `frontend/src/components/student/StudentLabSidebar.jsx`:
  - Add crisp Official/Practice indicator styling, Phosphor icon cues for locked/deadline states, and clean selection highlight.
- `frontend/src/components/student/StudentUI.jsx`:
  - Top summary KPI strip with `StageCard3D` + Phosphor Duotone icons (`Trophy`, `Target`, `CheckCircle`, `Clock`).
  - Result workspace flattened to strict 1-card depth: remove inner bordered card wrappers around MMD/Declaration/Testcase panels inside the result `bg-surface` container; use flat `bg-surface-secondary` rows and hairline `border-b` separators instead.
  - Add animated grading Lottie spinner (`grading-engine.json`) during active submission grading and animated pre-submit hero visual.
- `frontend/src/components/student/StudentHistoryPage.jsx`:
  - Flatten nested `rounded-3xl border` table wrapper inside the outer `rounded-3xl border bg-surface` card into a flush table layout.
  - Add `StageCard3D` KPI header cards with Phosphor duotone icons and animated score fill indicators.
- `frontend/src/pages/StudentRankingPage.jsx` & `frontend/src/components/ranking/RankingBoard.jsx`:
  - Remove outer card wrapper from `StudentRankingPage.jsx` (`div` container on page canvas) so `RankingBoard` is the single card level, and flatten `Your ranking` footer bar (`border-t` divider strip instead of nested card).
  - Add Gold/Silver/Bronze medal Phosphor icons (`Medal`, `Crown`) for ranks 1–3.

### Step F — Lecturer / Admin Portal (No Nested Cards + Business Semantics)
- `frontend/src/components/lecturer/DashboardSection.jsx`:
  - Refactor from bordered `bg-surface` outer box into a clean borderless section header container so child cards (`Select Lab`, `Lab Details`, `Roster`, `BulkGrading`, `RankingBoard`) are strictly 1-level cards.
- `frontend/src/components/lecturer/OverviewPanel.jsx` & `LecturerOverviewCard.jsx`:
  - Upgrade to interactive 3D tilt KPI cards (`StageCard3D`) with Phosphor Duotone icons, strictly solid Corporate Navy / Teal / Amber / Red accents (no nested icon card borders — use clean solid pill icon wells).
- `frontend/src/components/lecturer/GradeDistributionChart.jsx`:
  - Animated bar height reveal (`framer-motion`), crisp Cobalt/Teal solid palette tokens, clean empty state.
- `frontend/src/components/lecturer/SubmissionTable.jsx`:
  - Flatten `RosterSummaryBar` from mini bordered boxes inside the card into a flush horizontal segmented metric bar separated by hairline dividers.
  - Preserve exact `PlagiarismDangerMark` semantic colors (`ORIGINAL` yellow/amber, `PLAGIARIZER` red).
- `frontend/src/components/lecturer/BulkGradingPanel.jsx`:
  - Flatten nested bordered panels inside the bulk workspace, embed animated grading engine visual during batch run, crisp Phosphor status indicators.
- `frontend/src/pages/Reports.jsx` & `frontend/src/components/lecturer/ReportsPanel.jsx`:
  - Remove outer `bg-surface border` wrapper in `Reports.jsx` so each export card in `ReportsPanel` is a standalone 1-level `StageCard3D`, with Phosphor Duotone icons and ≤10% AI purple tag for AI/analytics reports.
- `frontend/src/pages/TermManagement.jsx`, `UserManagement.jsx` (`UserStats.jsx`, `UserTable.jsx`), `SolutionManagement.jsx`, `FeedbackInbox.jsx`:
  - Flatten nested inner bordered boxes into flat `bg-surface-secondary` divider strips / flush tables.
  - Upgrade `UserStats.jsx` to 3D KPI cards with Phosphor Duotone icons; enhance `FeedbackInbox` reply note as an inset left-accent callout (`border-l-4 border-primary bg-surface-secondary`) rather than a nested card.

### Step G — DOX Documentation Pass & Verification
- Update `docs/design/color-theory-light-dark-theme.md` & `design-system/oop-autograder/MASTER.md` so typography (`Plus Jakarta Sans` + `JetBrains Mono`), Corporate EdTech 60-30-10 tokens, Zero-Gradient, and Zero-Nested-Card standards agree 100%.
- Update `frontend/AGENTS.md` and affected `frontend/src/components/*/AGENTS.md` + `frontend/src/pages/AGENTS.md`.
- Run verification checks:
  1. `cd frontend && npm run theme:sync && npm run build && npm run build:desktop`
  2. Grep checks confirming **0 CSS/Tailwind gradients** and checking bordered card wrappers for any accidental nesting.
  3. Launch local Vite preview (`npm run preview`) and verify rendered screens in headless Google Chrome across Light & Dark themes and Mobile (`375px`) + Desktop (`1440px`) viewports with zero console/page errors.
  4. Write review record in `docs/reviews/2026-11-12-001-ui-corporate-edtech-3d-motion-review.md`.
