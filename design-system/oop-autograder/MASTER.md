# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** OOP AutoGrader
**Updated:** 2026-11-12
**Category:** Corporate EdTech / Institutional OOP Assessment Platform

---

## Global Rules

### 60-30-10 Color Hierarchy (`frontend/src/theme/tokens.js`)

- **60% Neutral Architectural Surfaces:**
  - Light: App `#F8FAFC`, Card `#FFFFFF`, Secondary `#F1F5F9`, Tertiary `#E2E8F0`, Border `#CBD5E1`, Subtle Border `#E2E8F0`, Foreground `#0F172A` / `#475569` / `#64748B`.
  - Dark: App `#0B1220`, Card `#111A2E`, Secondary `#18243C`, Tertiary `#22304C`, Border `#2A3956`, Subtle Border `#1E2D47`, Foreground `#F8FAFC` / `#CBD5E1` / `#94A3B8`.
- **30% Institutional Anchors:**
  - Corporate Navy: `#1E3A5F` (`navy`, Light) / `#23456E` (`navy`, Dark); hover `#162C47` / `#2B5385`; soft wash `#E8EEF5` / `#162840`.
  - Supporting Teal: `#0D9488` (Light & Dark); hover `#0F766E` / `#14B8A6`; wash `#CCFBF1` / `#113A3A`; text `#115E59` / `#5EEAD4`.
- **10% Interactive & Selective Accents:**
  - Interactive Cobalt (`primary`): `#2563EB` (`primary-hover` `#1D4ED8` Light / `#3B82F6` Dark; `primary-text` `#1E3A8A` Light / `#93C5FD` Dark).
  - Academic Gold (`gold`): `#D97706` (Light) / `#F59E0B` (Dark), used selectively for Rank #1 trophies, honors, and daily streak flame (`StudentStreakFlame`).
  - Semantic statuses: Success `#16A34A`, Warning `#D97706`, Error `#DC2626` (Light) / `#EF4444` (Dark), Info `#0284C7`.
  - AI Accent Purple (`purple`): `#7C3AED` (Light) / `#8B5CF6` (Dark), strictly ≤ 10% (`ReportsPanel` AI insights & selected spark highlights).

**Color Rules:** Single source of truth is `frontend/src/theme/tokens.js` synced by `npm run theme:sync`. Never use raw Tailwind palette utilities (`bg-blue-*`, `bg-purple-*`, etc.).

### Typography (Offline-Safe Bundled Fontsource)

- **UI Sans Font:** `Plus Jakarta Sans` (400, 500, 600, 700, 800 via `@fontsource/plus-jakarta-sans` — Vietnamese glyphs included, zero Google Fonts CDN).
- **Code / IDs / Tabular Mono Font:** `JetBrains Mono` (400, 500, 600 via `@fontsource/jetbrains-mono`), paired with `tabular-nums` on scores, IRNs, counters, and deadlines.

### Spacing Variables

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs

### Buttons

```css
/* Primary Button */
.btn-primary {
  background: #A16207;
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: #1E3A5F;
  border: 2px solid #1E3A5F;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}
```

### Cards

```css
.card {
  background: #F8FAFC;
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-md);
  transition: all 200ms ease;
  cursor: pointer;
}

.card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}
```

### Inputs

```css
.input {
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: #1E3A5F;
  outline: none;
  box-shadow: 0 0 0 3px #1E3A5F20;
}
```

### Modals

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

---

## Style Guidelines

**Style:** Swiss Modernism 2.0

**Keywords:** Grid system, Helvetica, modular, asymmetric, international style, rational, clean, mathematical spacing

**Best For:** Corporate sites, architecture, editorial, SaaS, museums, professional services, documentation

**Key Effects:** display: grid, grid-template-columns: repeat(12 1fr), gap: 1rem, mathematical ratios, clear hierarchy

### Page Pattern

**Pattern Name:** Portfolio Grid

- **Conversion Strategy:** Visuals first. Filter by category. Fast loading essential.
- **CTA Placement:** Project Card Hover + Footer Contact
- **Section Order:** Hero (Name/Role) > Project Grid (Masonry) > About/Philosophy > Contact

---

## Anti-Patterns (Do NOT Use)

- ❌ Low hierarchy
- ❌ no publication filtering
- ❌ cluttered visuals

### Anti-Patterns (Strictly Forbidden)

- ❌ **CSS / Tailwind Gradients** — No `linear-gradient`, `radial-gradient`, `conic-gradient`, or Tailwind `bg-gradient-*` / `from-` / `via-` / `to-` utilities anywhere; use crisp flat colors, borders, shadows, and 3D perspective planes.
- ❌ **Nested Bordered Cards ("no card-inside-card")** — Never wrap cards inside another bordered card (`DashboardSection`, `Reports`, `StudentRankingPage` stay borderless). Inside a card, use `border-t` / `divide-y` dividers, borderless `bg-surface-secondary` tints, or `border-l-4` accent strips.
- ❌ **Emojis as icons** — Use SVG icons (`lucide-react` for crisp controls/actions + `@phosphor-icons/react` Duotone/Fill for KPI wells, podiums, alerts, and section anchors) plus bundled CSP-safe Lottie animations (`lottie_light`).
- ❌ **Invisible dark-mode borders** — Always preserve calibrated `--border` (`#2A3956`) and `--shadow-stage-*` depth in `.dark`.
- ❌ **Missing cursor:pointer / focus states** — All clickable controls need clear hover/focus rings (`outline-none focus-visible:ring-2`).
- ❌ **Layout-shifting hovers & unguarded motion** — Respect `<MotionConfig reducedMotion="user">` and `@media (prefers-reduced-motion: reduce)`.
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio across Light and Dark surfaces.

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] Zero CSS/Tailwind gradients anywhere in `frontend/src/`
- [ ] Zero nested bordered cards ("card-inside-card")
- [ ] Single source of truth in `src/theme/tokens.js` + zero raw Tailwind palette classes
- [ ] Bundled fonts & Lottie assets work 100% offline (`npm run build:desktop` verified)
- [ ] Light & Dark themes preserve crisp borders (`#CBD5E1` / `#2A3956`) and stage shadows
- [ ] `prefers-reduced-motion` freezes Lottie on calm frame and skips 3D spring motion
- [ ] Responsive at `375px`, `768px`, `1024px`, `1440px` without horizontal overflow
