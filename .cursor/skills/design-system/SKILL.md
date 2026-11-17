---
name: design-system
description: "Authoritative UI/UX Design System Skill and Implementation Guide for OOP AutoGrader. Covers Swiss Modernism 2.0 principles, Corporate EdTech 60-30-10 color theory, 6 theme presets, dark mode depth calibration, zero-gradient & single-level card mandates, AppShell responsive layouts, in-depth dropbox specifications (drag-and-drop file upload dropzone and anchored dropdown comboboxes), shadow elevation, component primitives, typography, and WCAG AA accessibility."
version: "2.0.0"
stack: "React 18 + Vite 7 + Tailwind CSS 3 + Headless UI + Framer Motion + Phosphor & Lucide Icons + Fontsource Offline Fonts"
---

# Design System Skill — OOP AutoGrader

> **Classification:** Official UI/UX Design System & Implementation Specification  
> **Philosophy:** Swiss Modernism 2.0 · Rational Modular Grid · Corporate EdTech Hierarchy  
> **Target Platform:** OOP AutoGrader (EIU Capstone) — Web & Desktop Practice App  
> **Single Source of Truth:** `frontend/src/theme/tokens.js` (Color Tokens) & `design-system/oop-autograder/MASTER.md` (Layout Rules)

---

## 1. Skill Overview & When to Activate

This skill serves as the comprehensive, binding reference for designing, reviewing, and implementing user interfaces across the OOP AutoGrader platform.

### When to Activate This Skill
- **Building New Pages & Views:** Constructing responsive student/lecturer dashboards, submission drawers, grading matrices, or report views.
- **Designing Interactive Components:** Building or modifying form controls, dropdowns, comboboxes, modals, badges, or buttons.
- **File Upload & Drag-and-Drop Workflows:** Implementing or tuning the file dropzone ("dropbox"), folder validation, upload states, and grading animations.
- **Applying Theme Colors & Contrast:** Ensuring WCAG AA compliance across all 6 light and dark theme presets without hardcoding raw Tailwind colors.
- **Reviewing UI/UX Quality:** Enforcing the strict zero-gradient ban, the single-level card rule ("no card-inside-card"), and proper `tabular-nums` formatting.

---

## 2. Core Philosophy & Architectural Foundations

### 2.1 Swiss Modernism 2.0
The platform adheres strictly to **Swiss Modernism 2.0**:
- **Rational & Modular:** Every element aligns to a rigorous 12-column grid and a standardized 8px/16px mathematical spacing system.
- **Function Over Ornament:** Zero decorative background gradients, zero arbitrary drop shadows, zero cartoonish bevels. Visual depth is created through crisp surface layers, hairline borders, and calibrated stage shadows.
- **Information Density with Breathability:** High data density for complex academic grading, balanced by clear visual hierarchy, consistent typography scales, and generous section spacing.

### 2.2 The Two Absolute Design Prohibitions

> [!CAUTION]
> **1. STRICT ZERO-GRADIENT BAN**  
> Absolutely **NO** CSS or Tailwind gradients anywhere in `frontend/src/`:
> - Forbidden: `linear-gradient(...)`, `radial-gradient(...)`, `conic-gradient(...)`
> - Forbidden: Tailwind `bg-gradient-*`, `from-*`, `via-*`, `to-*`
> - Allowed: Flat calibrated semantic background colors (`bg-surface`, `bg-navy`, `bg-primary`), hairline borders (`border-border`), and calibrated box shadows (`shadow-stage-*`).

> [!CAUTION]
> **2. SINGLE-LEVEL CARD RULE ("NO CARD-INSIDE-CARD")**  
> Never nest a bordered card inside another bordered card.
> - Forbidden: Wrapping a bordered `Card` inside a bordered `DashboardSection` or parent card container.
> - Allowed Interior Separators:
>   - **Flush Line Dividers:** `border-t border-border` or `divide-y divide-border`.
>   - **Subtle Surface Tints:** Borderless tinted containers using `bg-surface-secondary` or `bg-surface-tertiary`.
>   - **Left-Edge Accent Strips:** Informational callouts using `border-l-4 border-info bg-info-bg` or `border-l-4 border-primary`.

> [!CAUTION]
> **3. NO UNSOLICITED VERBOSE DESCRIPTIONS (ZERO MICROCOPY CLUTTER)**  
> Do not write redundant subtitle descriptions, advisory paragraphs, or boilerplate captions unless explicitly requested:
> - **Forbidden (unless requested):** Subtitle text under dialog or card headers (e.g. `Import Gemini key · token management`).
> - **Forbidden (unless requested):** Explanatory disclaimers or policy paragraphs (e.g. `Your Gemini key is stored encrypted on the server and used only for your Conceptual hints. Feedback is advisory and never changes your official score.`).
> - **Forbidden (unless requested):** Unsolicited helper captions under progress bars or input fields (e.g. `Alerts at 50% and 80%. Hints pause at 90% (budget exhausted).`).
> - **Required Standard:** Keep UI elements concise, self-evident, and minimal (true Swiss Modernism). Show only clear titles, direct statuses, clean input labels, and actionable validation errors.

### 2.3 Offline-First Asset Architecture
The application runs both on the cloud web and as an offline Windows practice distribution (`App.desktop.jsx`). Therefore:
- **Zero External CDNs:** No Google Fonts CDN links, no unpkg icon scripts.
- **Bundled Fonts:** Bundled via Fontsource: `@fontsource/plus-jakarta-sans` (400–800) and `@fontsource/jetbrains-mono` (400–600).
- **Offline CSP-Safe Animations:** Vector Lottie animations use `lottie-web/build/player/lottie_light` importing local JSON files (`upload-cloud.json`, `grading-engine.json`, `empty-orbit.json`).

### 2.4 Dual Icon System (Zero Emojis)
Never use Unicode emojis (e.g. 📊, 🚀, ❌) for UI controls or status badges. Use the standardized dual-pack:
1. **`lucide-react`:** For compact functional controls, chevrons, sorting arrows, close buttons, search fields, and table actions.
2. **`@phosphor-icons/react` (Duotone / Fill):** For high-status elements, KPI wells, section header anchors, leaderboard podium crowns, alert indicators, and folder upload icons.

---

## 3. Comprehensive Color Theory (The 60-30-10 Hierarchy)

The platform implements an institutional Corporate EdTech **60-30-10 color hierarchy** calibrated for readability, WCAG AA compliance (4.5:1 for body text, 3:1 for graphical UI), and seamless switching across light and dark modes.

```
┌────────────────────────────────────────────────────────┐
│  60% NEUTRAL ARCHITECTURAL SURFACES                    │
│  Canvas, Stage Cards, Secondary Wells, Hairline Borders │
├────────────────────────────────────────┬───────────────┤
│  30% INSTITUTIONAL ANCHORS             │  10% ACCENTS  │
│  Corporate Navy + Supporting Teal      │  Cobalt CTA   │
│  Headers, Navigation, Badges, Metrics  │  Gold, Sem.   │
└────────────────────────────────────────┴───────────────┘
```

### 3.1 The 60% Architectural Neutrals (Canvas & Structure)
Neutrals provide the structural canvas. Cards must never blend into the background, and borders must remain crisp.

| Element | CSS Variable / Tailwind | Light Theme | Dark Theme (`.dark`) | Purpose |
|---|---|---|---|---|
| **App Canvas** | `--background` / `bg-background` | `#F8FAFC` (Slate 50) | `#0B1220` (Deep Slate) | Base page viewport background |
| **Card Surface** | `--surface` / `bg-surface` | `#FFFFFF` (Pure White) | `#111A2E` (Elevated Slate) | Top-level stage cards, panels |
| **Secondary Surface** | `--surface-secondary` / `bg-surface-secondary` | `#F1F5F9` (Slate 100) | `#18243C` (Mid Slate) | Table headers, well insets, chips |
| **Tertiary Surface** | `--surface-tertiary` / `bg-surface-tertiary` | `#E2E8F0` (Slate 200) | `#22304C` (High Slate) | Scrollbar thumbs, muted dividers |
| **Hairline Border** | `--border` / `border-border` | `#CBD5E1` (Slate 300) | `#2A3956` (Calibrated Slate) | Card borders, table grid lines |
| **Subtle Border** | `--border-subtle` / `border-border-subtle` | `#E2E8F0` (Slate 200) | `#1E2D47` (Subtle Slate) | Inner dividers, resting inputs |
| **Primary Text** | `--text-primary` / `text-foreground` | `#0F172A` (Slate 900) | `#F8FAFC` (Slate 50) | Headings, primary labels, values |
| **Secondary Text** | `--text-secondary` / `text-foreground-secondary` | `#475569` (Slate 600) | `#CBD5E1` (Slate 300) | Subheadings, table cells, hints |
| **Muted Text** | `--text-muted` / `text-foreground-muted` | `#64748B` (Slate 500) | `#94A3B8` (Slate 400) | Placeholders, timestamps, captions |

### 3.2 The 30% Institutional Anchors (Navy & Teal)
These colors anchor brand authority and structural navigation.

#### Corporate Navy (`--navy`)
- **Light:** `#1E3A5F` · Hover: `#162C47` · Light Wash: `#E8EEF5` · Text: `#162C47`
- **Dark:** `#23456E` · Hover: `#2B5385` · Light Wash: `#162840` · Text: `#C7D9EE`
- **Usage:** Brand header badge (`AppLogo`), Official lab prefix badges (`LabPrefixBadge`), major section header bars (`DashboardSection`), table header bars, institutional KPI wells.

#### Supporting Teal (`--secondary`)
- **Light:** `#0D9488` · Hover: `#0F766E` · Wash: `#CCFBF1` · Text: `#115E59`
- **Dark:** `#0D9488` · Hover: `#14B8A6` · Wash: `#113A3A` · Text: `#5EEAD4`
- **Usage:** Completed submission states, Practice lab badges, MMD class modeling metrics, secondary progress bars.

### 3.3 The 10% Interactive CTA & Selective Accents

#### Interactive Cobalt (`--primary`)
- **Light:** `#2563EB` · Hover: `#1D4ED8` · Active: `#1E40AF` · Wash: `#DBEAFE` · Text: `#1E3A8A`
- **Dark:** `#2563EB` · Hover: `#3B82F6` · Active: `#1D4ED8` · Wash: `#172B4D` · Text: `#93C5FD`
- **Usage:** Primary interactive buttons (`Button`), active navigation tabs, selected radio/combobox rows, active focus rings (`focus-visible:ring-primary`).

#### Academic Gold (`--gold`)
- **Light:** `#D97706` · Hover: `#B45309` · Wash: `#FEF3C7` · Text: `#92400E`
- **Dark:** `#F59E0B` · Hover: `#D97706` · Wash: `#422006` · Text: `#FDE68A`
- **Usage:** Selective recognition only: Rank #1 podium trophy, student streak flame (`StudentStreakFlame`), Midterm exam badges (`LabPrefixBadge`).

#### Semantic Status System
All status badges and alerts follow a two-tier color pairing: a soft background wash with high-contrast text.

| Status | Meaning | Light Tone | Dark Tone | Light Wash (`-bg`) | Dark Wash (`-bg`) |
|---|---|---|---|---|---|
| **Success** | Pass, 100%, enrolled, active | `#16A34A` | `#16A34A` / `#22C55E` | `#DCFCE7` | `#143E29` |
| **Warning** | Pending, needs review, expiring | `#D97706` | `#D97706` / `#F59E0B` | `#FEF3C7` | `#422408` |
| **Error** | Fail, compile error, suspended | `#DC2626` | `#EF4444` | `#FEE2E2` | `#451419` |
| **Info** | Format instructions, guides | `#0284C7` | `#0284C7` / `#38BDF8` | `#E0F2FE` | `#0C3757` |

#### Strict AI Purple Restriction (`--purple`)
- **Values:** Light `#7C3AED` · Dark `#8B5CF6` · Light Wash `#EDE9FE` / `#2E1E59`
- **Rule:** Strictly ≤ 10% of any view. Reserved exclusively for AI code suggestions, automated tutoring feedback, and `ReportsPanel` machine-generated insights. Never use purple for main navigation or primary action buttons.

### 3.4 The 6 Registered Theme Presets

The theme system (`src/theme/tokens.js`) supports 6 presets switchable via the radial `ThemeToggle.jsx`:

| Preset ID | Name | Mode | Canvas (`--background`) | Surface (`--surface`) | Anchor Navy | CTA Primary |
|---|---|---|---|---|---|---|
| `light` | **Classic Slate** | Light | `#F8FAFC` | `#FFFFFF` | `#1E3A5F` | `#2563EB` (Cobalt) |
| `sand` | **Warm Sand** | Light | `#FAF5EA` | `#FFFDF9` | `#5C4028` (Umber) | `#B45309` (Amber) |
| `evergreen` | **Evergreen** | Light | `#F4F7F5` | `#FFFFFF` | `#1B4332` (Pine) | `#0F766E` (Teal) |
| `dark` | **Midnight** | Dark | `#0B1220` | `#111A2E` | `#23456E` (Slate Navy) | `#2563EB` (Vibrant Blue) |
| `graphite` | **Graphite** | Dark | `#111215` | `#181A1F` | `#2C313A` (Charcoal) | `#6366F1` (Indigo) |
| `scarlet` | **Scarlet** | Dark | `#140B0D` | `#1E1215` | `#5E1724` (Crimson) | `#DC2638` (Ruby) |

### 3.5 Dark Mode Depth & Contrast Engineering
1. **Never Use Pitch Black (`#000000`):** Pitch black causes harsh OLED eye strain and destroys elevation perception. The dark theme uses calibrated deep slate layers (`#0B1220` canvas → `#111A2E` card → `#18243C` secondary well).
2. **Preserve Hairline Borders:** In dark mode, borders must never be hidden. The `#2A3956` hairline border provides essential shape definition.
3. **Calibrated Dark Shadows:** Dark mode shadows use `--shadow-rgb` (`rgb(2 6 23)`) scaled with higher opacity:
   - `shadow-stage-1`: `0 2px 8px -2px rgb(var(--shadow-rgb) / 0.55)`
   - `shadow-stage-2`: `0 12px 26px -6px rgb(var(--shadow-rgb) / 0.65)`
   - `shadow-stage-3`: `0 24px 48px -12px rgb(var(--shadow-rgb) / 0.78)`

---

## 4. Layout & Grid Architecture (Swiss Modernism 2.0)

### 4.1 Fluid Base Typography & Viewport Setup
The root layout (`frontend/src/index.css`) applies fluid clamp sizing to ensure readable proportions across small phones and 4K displays:
```css
html {
  font-size: clamp(14px, 0.35vw + 13px, 16px);
}
```
All major layout containers enforce `overflow-x: hidden` and `min-w-0` to eliminate horizontal page jitter.

### 4.2 AppShell Framework
The standard screen wrapper (`AppShell.jsx`) enforces a consistent three-tier vertical flow:

```
┌────────────────────────────────────────────────────────┐
│ HEADER (z-30 sticky)                                   │
│ Logo · Navigation Link Wells · Theme Orbit · User Menu │
├────────────────────────────────────────────────────────┤
│ NAVBAR (Optional Sub-navigation pill tabs)             │
├────────────────────────────────────────────────────────┤
│ <main> (Flex-1, bounded padding: px-4 sm:px-6 lg:px-8) │
│                                                        │
│ [ 12-Column Responsive Content Grid ]                  │
│                                                        │
├────────────────────────────────────────────────────────┤
│ FOOTER (Persistent subtle copyright & version status)  │
└────────────────────────────────────────────────────────┘
  [ Floating Feedback FAB: fixed right-6 bottom-6 z-50 ]
  [ Toast Viewport: fixed right-6 bottom-20 z-[110] ]
```

### 4.3 The 12-Column Responsive Dashboard Grid
Dashboards follow standardized column splits based on task complexity:

#### 1. The Standard 8 + 4 Dashboard Split
Used in `StudentDashboard.jsx` and `SolutionManagement.jsx`:
- **Left Panel (8 cols / `lg:col-span-8`):** Main operational area — lab assignment cards, submission dropzone, compilation logs, challenge breakdown tabs.
- **Right Rail (4 cols / `lg:col-span-4`):** Contextual supporting data — submission attempt history, score progress ring, student streak flame, rubric breakdown.

#### 2. Symmetrical 6 + 6 Split
Used in diff views, plagiarism comparisons, and dual code inspectors (`ClassScoreBreakdown.jsx`).

#### 3. 4-Card Metric Strip
Used in `LecturerDashboard.jsx` and `Reports.jsx` for KPI overviews:
- Desktop (`≥1024px`): `grid-cols-4` (3 cols each)
- Tablet (`768px - 1023px`): `grid-cols-2` (6 cols each)
- Mobile (`<768px`): `grid-cols-1` (12 cols full width)

### 4.4 Spacing Scale System
Use standard design tokens for spacing rather than arbitrary pixels:

| Token | Pixels | Rem | Canonical Usage |
|---|---|---|---|
| `--space-xs` | `4px` | `0.25rem` | Badge gaps, tight inline chip padding |
| `--space-sm` | `8px` | `0.5rem` | Icon-to-text spacing, input padding vertical |
| `--space-md` | `16px` | `1rem` | Standard card internal padding, table cell gaps |
| `--space-lg` | `24px` | `1.5rem` | Main card padding (`p-6`), section headers |
| `--space-xl` | `32px` | `2rem` | Page section margins (`gap-8`) |
| `--space-2xl`| `48px` | `3rem` | Major dashboard grid separation |
| `--space-3xl`| `64px` | `4rem` | Hero banners, empty state vertical breathing |

---

## 5. The "Dropbox" Patterns — In-Depth Specifications

In the OOP AutoGrader application, the term **"Dropbox"** encompasses three mission-critical design patterns:
1. **File Dropzone ("Drag-and-Drop File Upload Dropbox"):** The primary student workflow for uploading Java source code, Mermaid diagrams, and `.git` repositories.
2. **Dropdown & Combobox Menus ("Selection Dropboxes"):** High-precision select menus for labs, roles, terms, and export options.
3. **Drop Shadows ("Shadow Elevation Stages"):** The 3-tier elevation system that establishes depth without muddy gradients.

---

### 5.1 Pattern A: File Dropzone (Drag-and-Drop File Upload Dropbox)

Component: [`DropZone.jsx`](file:///home/kayd/Capstone_Project/OOP-AutoGrader/frontend/src/components/ui/DropZone.jsx)

```
┌─────────────────────────────────────────────────────────────────┐
│ ℹ Folder format: IRN_StudentName / challenge_1 / *.java & *.mmd │  <- Format Hint Strip
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                   ┌───────────────────────┐                     │
│                   │   [Lottie Animation]  │                     │
│                   │  Upload Cloud (idle)  │                     │
│                   │  Grading Engine (run) │                     │
│                   └───────────────────────┘                     │
│                                                                 │
│             Drop or drag your project folder here               │
│         Running declaration checks, MMD inspection & tests      │
│                                                                 │
│             ┌───────────────────────────────────┐               │
│             │  [📂] Select Project Folder       │               │  <- Primary CTA Button
│             └───────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
  [ 2px dashed border · shadow-stage-1 · perspective-1000 ]
```

#### 1. Visual Anatomy & Interactive States
The Dropzone is an elevated interactive stage card (`perspective-1000`) with distinct states:

| State | Visual Manifestation | Tailwind Implementation |
|---|---|---|
| **1. Idle** | Dashed border, subtle background, calm cloud Lottie animation | `border-2 border-dashed border-border bg-surface shadow-stage-1 hover:border-primary/50 hover:shadow-stage-2 transition-all duration-200` |
| **2. DragOver (Active)** | Scale boost, vibrant Cobalt border, tinted primary wash | `scale-[1.01] border-primary bg-primary-light/60 shadow-stage-2` + icon well `-translate-y-1 scale-105 bg-primary-light` |
| **3. Uploading / Grading** | Lottie switches to `gradingEngineData`, spinner on button, text shifts to "Compiling & grading...", pointer events locked | `pointer-events-none opacity-95` + animated spinner `animate-spin border-2 border-white border-t-transparent` |
| **4. Error (Validation)** | Red monospace error box, toast alert, shake micro-animation | `<pre className="text-error-text text-sm whitespace-pre-wrap text-left bg-error-bg/50 p-3 rounded-xl border border-error/30">` |
| **5. Disabled** | Reduced opacity, cursor not allowed | `opacity-60 cursor-not-allowed pointer-events-none` |

#### 2. Folder Traversal Engineering
Standard browser `<input type="file">` does not read subfolders when dragging directories. The Dropzone must recursively walk the `FileSystemEntry` tree using `webkitGetAsEntry()`:

```javascript
// Recursive directory traversal for drag-and-drop
const walkEntry = (entry, pathPrefix, collected) => {
  return new Promise((resolve, reject) => {
    if (entry.isFile) {
      entry.file((file) => {
        collected.push({ file, relativePath: pathPrefix + file.name });
        resolve();
      }, reject);
    } else if (entry.isDirectory) {
      const reader = entry.createReader();
      const readAllBatches = (accumulated = []) => {
        reader.readEntries(async (batch) => {
          if (batch.length === 0) {
            for (const child of accumulated) {
              await walkEntry(child, pathPrefix + entry.name + '/', collected);
            }
            resolve();
          } else {
            readAllBatches(accumulated.concat(batch));
          }
        }, reject);
      };
      readAllBatches();
    } else {
      resolve();
    }
  });
};
```

#### 3. Client-Side Folder Validation Contracts
Before transmitting bytes to the server, the Dropzone validates folder structure locally:
- **Standard Lab Format:** Root folder `IRN_StudentName` containing subfolders `challenge_1`, `challenge_2`, etc., containing `.java` and `.mmd` files.
- **Midterm Lab Format:** Root folder `IRN_StudentName` containing `.java` and `.mmd` files directly at root (no challenge subfolders).
- **Git Preservation:** Files inside `.git/` must be collected and sent intact for plagiarism and commit history verification.

---

### 5.2 Pattern B: Dropdown & Combobox Controls (Selection Dropboxes)

Components: [`Combobox.jsx`](file:///home/kayd/Capstone_Project/OOP-AutoGrader/frontend/src/components/ui/Combobox.jsx) and [`Select.jsx`](file:///home/kayd/Capstone_Project/OOP-AutoGrader/frontend/src/components/ui/Select.jsx)

```
┌───────────────────────────────────────────────┐
│ [Icon] Lab 4: Object Composition         [ ▼ ]│  <- Trigger Button (rounded-xl, border-border)
└───────────────────────────────────────────────┘
      │
      ▼  (gap: 6px, backdrop-blur-xl bg-surface/98, shadow-stage-3)
┌───────────────────────────────────────────────┐
│ 🔍 [Search labs...]                       [×] │  <- Optional Search Bar
├───────────────────────────────────────────────┤
│ OFFICIAL LABS                                 │  <- Sticky Group Header
├───────────────────────────────────────────────┤
│    Lab 1: Class & Object Basics               │
│ ✓  Lab 4: Object Composition         [Badge]  │  <- Selected (bg-primary-light, Check)
│    Lab 5: Polymorphism & Interfaces           │
├───────────────────────────────────────────────┤
│ PRACTICE LABS                                 │
├───────────────────────────────────────────────┤
│    Practice: Bank Account Modeling            │
└───────────────────────────────────────────────┘
```

#### 1. Anatomy of the Modern Combobox
1. **Trigger Shell (`ListboxButton`):**
   - Default: `min-h-10 rounded-xl px-3 py-2 text-sm border border-border bg-surface shadow-sm`
   - Compact: `min-h-[32px] rounded-lg px-2.5 py-1 text-xs` (for table filter bars)
   - Interactive feedback: `hover:border-border-subtle hover:bg-surface-secondary/40 focus:border-primary focus:ring-2 focus:ring-primary/25`
   - Chevron: Smooth 180° rotation on open: `transition-transform duration-200 group-data-open:rotate-180 group-data-open:text-primary`
2. **Floating Popover (`ListboxOptions`):**
   - Precision alignment: Anchored via Headless UI `anchor={{ to: 'bottom start', gap: 6 }}`. Zero horizontal overlap.
   - Elevation: `z-[250] rounded-2xl border border-border bg-surface/98 backdrop-blur-xl shadow-stage-3 ring-1 ring-border/80`.
   - Motion: Micro-scale entrance without layout shift: `data-closed:scale-95 data-closed:opacity-0 data-[anchor~="bottom"]:data-closed:-translate-y-1 transition duration-150 ease-out`.
3. **Rich Option Rows (`ListboxOption`):**
   - Active state: `data-focus:bg-surface-secondary/70`
   - Selected state: `data-selected:bg-primary-light/60 font-semibold text-primary-text`
   - Leading icon support, subtitle description support, and trailing checkmark (`<Check className="h-4 w-4 text-primary" />`).
4. **Searchable Mode:**
   - Includes a sticky top input (`sticky top-0 bg-surface p-2 border-b border-border/70`) with real-time filtering, clear button, and an empty state displaying `SearchX` with "No matches found".

---

### 5.3 Pattern C: Drop Shadows & Elevation System

Box shadows establish spatial hierarchy. The system provides three distinct stages:

```css
/* Light Theme Stage Shadows */
--shadow-stage-1: 0 1px 3px 0 rgb(15 23 42 / 0.08), 0 1px 2px -1px rgb(15 23 42 / 0.06);
--shadow-stage-2: 0 4px 12px -2px rgb(15 23 42 / 0.12), 0 2px 6px -2px rgb(15 23 42 / 0.08);
--shadow-stage-3: 0 20px 32px -8px rgb(15 23 42 / 0.18), 0 8px 16px -4px rgb(15 23 42 / 0.10);

/* Calibrated Dark Theme Stage Shadows (avoiding white halos) */
.dark .shadow-stage-1 {
  --tw-shadow: 0 2px 8px -2px rgb(var(--shadow-rgb) / 0.55);
}
.dark .shadow-stage-2 {
  --tw-shadow: 0 12px 26px -6px rgb(var(--shadow-rgb) / 0.65);
}
.dark .shadow-stage-3 {
  --tw-shadow: 0 24px 48px -12px rgb(var(--shadow-rgb) / 0.78);
}
```

- **`shadow-stage-1`:** Resting cards, idle inputs, closed dropzones.
- **`shadow-stage-2`:** Hovered cards, drag-active file dropzones, table row focus.
- **`shadow-stage-3`:** Dropdown popovers, combobox options, modal dialogs, drawers.

---

## 6. Component Primitives & Recipes Catalog

### 6.1 Button Primitive (`Button.jsx`)
```jsx
// Standard interactive button with active micro-scale press feedback
<button
  type={type}
  onClick={onClick}
  disabled={disabled}
  className={`inline-flex items-center justify-center gap-2 px-5 py-2 rounded-xl font-medium
    transition-all duration-150 active:scale-[0.985]
    focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-1
    disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100 ${className}`}
>
  {children}
</button>
```

#### Button Variant Classes
- **Primary CTA:** `bg-primary hover:bg-primary-hover text-white shadow-stage-1`
- **Secondary / Outline:** `border border-border bg-surface hover:bg-surface-secondary text-foreground`
- **Destructive:** `bg-error hover:bg-error-hover text-white shadow-stage-1`
- **Ghost:** `hover:bg-surface-secondary text-foreground-secondary hover:text-foreground`

### 6.2 Standard Card (`Card.jsx`) & 3D Stage Card (`StageCard3D`)
```jsx
// Standard Flat Card
export default function Card({ children, className = "" }) {
  return (
    <div className={`bg-surface rounded-2xl p-6 shadow-stage-1 border border-border transition-colors ${className}`}>
      {children}
    </div>
  );
}

// 3D Perspective Stage Card with gentle pointer tracking
<StageCard3D maxTilt={4} hoverLift={-3} className="p-6">
  <CardContent />
</StageCard3D>
```

### 6.3 Scoring Pill (`ScorePill.jsx`)
Grading results must always render with Phosphor duotone icons and `tabular-nums` formatting:
- **≥ 80% (Pass / High Distinction):** `bg-success-bg text-success-text` + `<CheckCircle weight="duotone" />`
- **60% – 79% (Average / Pass):** `bg-warning-bg text-warning-text` + `<WarningCircle weight="duotone" />`
- **< 60% (Fail / Needs Work):** `bg-error-bg text-error-text` + `<XCircle weight="duotone" />`

### 6.4 Lab Prefix Badge (`LabPrefixBadge.jsx`)
- **Official Lab:** `variant="navy"` (`border-navy/20 bg-navy-light text-navy-text`)
- **Practice Lab:** `variant="teal"` (`bg-secondary-light text-secondary-text`)
- **Midterm Exam:** `variant="gold"` (`border-gold/30 bg-gold-bg text-gold-text`)

### 6.5 Modal & Overlay Architecture (`Modal.jsx`)
Dialogs portal directly to `document.body` to guarantee they render above sticky headers (`z-30`) and navigation rails:
- **Overlay:** `fixed inset-0 z-[100] bg-black/60 backdrop-blur-[2px] dark:bg-black/75 flex items-center justify-center p-4`
- **Modal Box:** `w-full max-w-2xl rounded-2xl bg-surface border border-border p-6 shadow-stage-3` with Framer Motion scale entrance (`scale: 0.96 → 1.0`).

### 6.6 Persistent Feedback Toasts (`Toast.jsx`)
- **Location:** `fixed right-6 bottom-20 z-[110]` (floats safely above the Feedback FAB and below header elements).
- **Auto-dismiss:** Default 3 seconds for simple confirmations.
- **Persistent Mode (`persist: true`):** Retains complex warnings (e.g., student roster CSV unmatched rows) with action buttons like **Show details** until explicitly dismissed.

---

## 7. Typography & Monospace Rules

The design system standardizes on two bundled typefaces:

### 7.1 Sans-Serif (`Plus Jakarta Sans`)
Used for all UI prose, labels, headings, and controls.
- **Hierarchy Scale:**
  - `text-2xl font-bold` (24px / 32px line height): Page titles, dashboard greetings
  - `text-lg font-semibold` (18px / 28px): Section card titles, modal titles
  - `text-sm font-semibold` (14px / 20px): Table headers, form labels, button text
  - `text-sm font-normal` (14px / 20px): Table body cells, card descriptions
  - `text-xs font-medium` (12px / 16px): Badge text, status pills, helper captions

### 7.2 Monospace & Tabular Numerics (`JetBrains Mono`)
Used for code, IRNs, test case signatures, timestamps, and numeric scores.

> [!IMPORTANT]
> **Mandatory Tabular Numbers (`tabular-nums`):**  
> Every numeric display that updates dynamically (scores, timers, percentages, attempt counts, student IRNs) **MUST** include the `tabular-nums` class. This enforces fixed-width numerals and prevents jarring layout jitter during re-renders.

```jsx
// Correct numeric display
<span className="font-mono text-sm font-semibold tabular-nums text-foreground">
  {score.ok}/{score.total} · {score.pct}%
</span>
```

---

## 8. Motion, 3D Perspective & Accessibility (WCAG AA)

### 8.1 Reduced Motion Mandate
All Framer Motion springs and Lottie animations must respect user OS accessibility preferences:
- Root wrapper: `<MotionConfig reducedMotion="user">` in `main.jsx`.
- In custom components: Guard with `useReducedMotion()`. If reduced motion is active, skip 3D tilt, disable card lift, and freeze Lottie animations on a calm resting frame.

### 8.2 Keyboard Navigation & Focus Rings
Every interactive control (buttons, comboboxes, dropzones, table sort headers) must provide an accessible focus ring:
```css
focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-1
```

---

## 9. Anti-Patterns (Strict Prohibitions)

| Anti-Pattern (Strictly Forbidden) | Reason for Ban | Required Replacement |
|---|---|---|
| ❌ `bg-gradient-to-r`, `linear-gradient` | Clutters UI, violates Swiss Modernism, causes theme mismatch | Flat semantic background (`bg-surface`, `bg-navy`, `bg-primary`) + border |
| ❌ Nested bordered cards ("card-in-card") | Visual noise, boxiness, ruins elevation depth | Flush line dividers (`border-t`, `divide-y`), secondary tints (`bg-surface-secondary`), or `border-l-4` strips |
| ❌ Raw Tailwind colors (`bg-blue-600`, `text-purple-500`) | Bypasses theme switcher tokens, breaks 6 presets | Semantic theme classes (`bg-primary`, `text-foreground-muted`, `bg-accent-purple`) |
| ❌ Emojis as UI icons (📊, 🚀, 💡, ❌) | Inconsistent platform rendering, unprofessional | `lucide-react` or `@phosphor-icons/react` Duotone SVGs |
| ❌ Invisible dark mode borders (`border-transparent`) | Surfaces melt together in dark mode | Hairline calibrated border (`border-border` / `#2A3956`) |
| ❌ Unrestricted Purple (> 10% UI) | Distorts Corporate EdTech brand palette | Keep purple strictly ≤ 10%, reserved solely for AI features |
| ❌ Plain numbers without `tabular-nums` | Causes horizontal layout jitter during real-time updates | Add `tabular-nums` class to all numbers, scores, and timers |
| ❌ Verbose boilerplate descriptions / unsolicited helper paragraphs (e.g. `Import Gemini key · token management`, `Your Gemini key is stored encrypted...`, `Alerts at 50% and 80%...`) | Creates microcopy clutter, degrades Swiss Modernism minimalism, wastes modal real estate | Do NOT write descriptions or explanatory paragraphs unless explicitly requested. Rely on clear titles, clean status chips, and self-evident fields |

---

## 10. Developer Pre-Flight Checklist

Before submitting any frontend component, page, or style edit, verify:
- [ ] **Zero Gradients:** Ran `grep -rn "gradient" frontend/src/` and confirmed 0 results.
- [ ] **No Card-in-Card:** Verified no `<Card>` or bordered container sits inside another bordered card.
- [ ] **No Unsolicited Descriptions:** Verified modals, cards, and inputs do not include redundant subtitle descriptions, policy paragraphs, or unrequested helper text unless explicitly requested.
- [ ] **Token Purity:** No raw Tailwind colors (`bg-blue-*`, `text-slate-*`) — all use semantic tokens.
- [ ] **Dropbox States:** Verified File Dropzone handles Idle, DragOver, Uploading (Lottie switch), and Error.
- [ ] **Combobox Placement:** Verified Combobox uses `anchor={{ to: 'bottom start', gap: 6 }}` and closes cleanly.
- [ ] **Theme Preset Test:** Tested view in both `Classic Slate` (light) and `Midnight` (dark) — contrast is sharp.
- [ ] **Tabular Numerics:** All scores, percentages, and IRN codes have `tabular-nums`.
- [ ] **Offline Fonts & Icons:** No external CDN requests made; fonts bundled via Fontsource.
- [ ] **Reduced Motion:** Verified components don't shake or tilt when `prefers-reduced-motion` is enabled.
- [ ] **Responsive Breakpoints:** Verified layout at `375px` (mobile), `768px` (tablet), `1024px` (desktop), and `1440px` (wide).
