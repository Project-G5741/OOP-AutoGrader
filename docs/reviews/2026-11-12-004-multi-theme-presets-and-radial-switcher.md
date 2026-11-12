---
date: 2026-11-12
branch: feat/lottie-grading-upload-animations
base_commit: 217c5e4
prerequisite_review: docs/reviews/2026-11-12-003-desktop-trim-dark-borders-and-bundle.md
status: completed
---

# Session Summary — Multi-Theme Presets, Dark-Theme Scoping Fix, & Radial Theme Switcher

## 1. Modular Multi-Theme Config Architecture (6 Corporate EdTech Presets)

Refactored `frontend/src/theme/tokens.js` so all color palettes live in standalone preset config files under `frontend/src/theme/presets/` and share identical token keys verified at sync/build time:

| Preset ID | Label | Scheme | Icon (`ThemeToggle`) | Primary / Signature Palette | File |
|---|---|---|---|---|---|
| `light` | Classic Slate | `light` | `sun` (`Sun`) | Institutional Navy `#1E3A5F` + Cobalt `#2563EB` | `frontend/src/theme/presets/light.js` |
| `sand` | Sand | `light` | `sand` (`Waves`) | Warm Sand Canvas `#FAF5EA` + Umber `#5C4028` + Ochre `#B45309` | `frontend/src/theme/presets/sand.js` |
| `evergreen` | Evergreen | `light` | `leaf` (`Leaf`) | Forest Pine `#1B4332` + Emerald Teal `#0F766E` | `frontend/src/theme/presets/evergreen.js` |
| `dark` | Midnight | `dark` | `moonStar` (`MoonStar`) | Deep Navy Night `#0B1220` + Cobalt `#2563EB` / `#3B82F6` | `frontend/src/theme/presets/dark.js` |
| `graphite` | Graphite | `dark` | `terminal` (`Terminal`) | Charcoal `#111215` + Enterprise Indigo `#6366F1` / `#818CF8` | `frontend/src/theme/presets/graphite.js` |
| `scarlet` | Scarlet | `dark` | `flame` (`Flame`) | Deep Crimson-Black `#140B0D` + Scarlet `#DC2638` / `#EF4454` | `frontend/src/theme/presets/scarlet.js` |

### Related Config & Build Sync Updates
- `frontend/src/theme/tokens.js`: exports `themePresets`, `theme`, `themePresetMap`, `themeMetaList` (with `orbColor`), `isValidThemeId`, `getThemePreset`, `getChartPalette`, `chartLight`, `chartDark`, and `tokenKeys`.
- `frontend/scripts/generate-theme-css.mjs` & `frontend/src/theme/tokens.generated.css`: validates that every preset has identical keys and writes `:root`, `:root.dark:not([data-theme])`, and `:root[data-theme="<id>"]` with `color-scheme`, `--<token>`, and `--<token>-rgb` channels.
- `frontend/src/context/ThemeContext.jsx`: manages `data-theme="<id>"` and `.dark` (`colorScheme === 'dark'`) on `<html>`, persists to `localStorage` (`oop-theme`), normalizes OS default (`light`/`dark`), and exposes `{ theme, themeMeta, themes, isDark, setTheme, toggleTheme }`.

---

## 2. Post-Login Dark-Theme Cascade Bug Fix

### Root Cause
1. `frontend/src/theme/tokens.generated.css` previously emitted `.dark` (which overrode `:root[data-theme="scarlet"]` and `:root[data-theme="graphite"]` whenever any element also carried the `.dark` class).
2. `frontend/src/pages/LecturerDashboard.jsx` and `frontend/src/pages/UserManagement.jsx` wrapped their post-login views in `<div className={isDark ? 'dark' : ''}>`, creating a closer ancestor `.dark` scope that reset Scarlet/Graphite CSS variables back to Midnight (`dark`) Cobalt tones after signing in.

### Fixes Applied
- `frontend/scripts/generate-theme-css.mjs` & `frontend/src/theme/tokens.generated.css`: changed fallback dark selector from `.dark` to `:root.dark:not([data-theme])` and gave each preset `:root[data-theme="<id>"]` with explicit `color-scheme`.
- `frontend/src/pages/LecturerDashboard.jsx` & `frontend/src/pages/UserManagement.jsx`: removed the redundant `useTheme()` import and `<div className={isDark ? 'dark' : ''}>` wrapper (using `<>...</>`) so Tailwind `dark:*` variants read `html.dark` cleanly without overriding `data-theme` variables.
- `frontend/src/index.css` & `frontend/tailwind.config.js`: moved `color-scheme` to generated theme selectors and drove dark shadow utilities from `rgb(var(--shadow-rgb) / ...)`.

---

## 3. Paginated 3-Orb Radial `ThemeToggle` ("Kiểu B") & Trigger Alignment

Kept `<ThemeToggle />` in its existing slot across [`Header.jsx`](frontend/src/components/Header.jsx) and auth/maintenance screens while upgrading the switcher UX:
- **Trigger matched to header avatar (`40x40px`)** (`frontend/src/components/ThemeToggle.css`, `frontend/src/components/ThemeToggle.jsx`): resized `.theme-trigger` and `.theme-arc-anchor` from `32px` to `40px x 40px` with an `h-5 w-5` (`20px`) icon to match `<User className="h-5 w-5" />` in the header account button, and adjusted `.theme-trigger-dot` (`11px`), `.theme-orb` (`top: -6px; right: -6px`), and `.theme-page-pill` (`top: 138px; right: -4px`).
- **3 enlarged orbs per page along a tight arc** (`frontend/src/components/ThemeToggle.jsx`, `frontend/src/components/ThemeToggle.css`): `ORBS_PER_PAGE = 3`, `52x52px` `.theme-orb` (`38x38px` core), tight radius `r = 86` (`174° → 92°`), flat surface-ringed design (zero CSS gradients), and unique icon + `orbColor` per dark preset (`MoonStar` + `#3B82F6`, `Terminal` + `#818CF8`, `Flame` + `#EF4454`).
- **Paging & keyboard/wheel controls**: supports mouse wheel scrolling (`180ms` cooldown), clickable `.theme-page-pill` (`Light 1/2` / `Dark 2/2`), `ArrowLeft` / `ArrowRight` / `PageUp` / `PageDown`, `Escape`, and outside-click close. Auto-opens on the page containing the active theme.
- **Stacking context fix on standalone screens**: raised `.theme-toggle` (`frontend/src/pages/LoginUI.css`), `.mnt-toggle` (`frontend/src/pages/MaintenancePage.css`), and `FirstTimeSetupUI` toggle wrapper (`frontend/src/pages/FirstTimeSetupUI.jsx`) to `z-index: 30` / `z-30` so fanned orbs render above cards.

---

## 4. DOX & Verification

- **DOX updated:** `frontend/AGENTS.md` updated to document `src/theme/presets/*.js`, `src/theme/tokens.js`, `:root[data-theme="<id>"]` + `:root.dark:not([data-theme])` generation, and the paginated `40px` radial `ThemeToggle.jsx`.
- **Verification commands (`cd frontend && npm run build && npm run build:desktop`):**
  - Exited `0`.
  - `npm run theme:sync` (via `prebuild`) wrote `src/theme/tokens.generated.css (6 themes: light, sand, evergreen, dark, graphite, scarlet)`.
  - `vite build` transformed `7391` modules cleanly.
  - `vite build --mode desktop` (`build:desktop`) built in `17.67s`.
