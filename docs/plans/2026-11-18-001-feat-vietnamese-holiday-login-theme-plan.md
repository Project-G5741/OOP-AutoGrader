---
title: Vietnamese Holiday Login Design & Seasonal System Theme - Plan
type: feat
date: 2026-11-18
topic: vietnamese-holiday-login-theme
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections from user-settled decisions
---

# Vietnamese Holiday Login Design & Seasonal System Theme - Plan

## Goal Capsule

- **Objective:** Automatically switch Web Login (via isolated, pluggable per-holiday Design Modules) and the Web global theme preset across all 18 Vietnamese & campus holidays using a 15-day pre-holiday window, sequential overlap handoff, and Hybrid (Frontend + Backend `LocalDate.now` Vietnam timezone) date resolution.
- **Product authority:** This Product Contract.
- **Open blockers:** None.
- **Stop conditions:** Offline Desktop builds (`desktop` and `desktop-lecturer`) are excluded and keep their existing default behavior. Zero CSS/SVG gradients (`linear-gradient`, `radial-gradient`, `conic-gradient`, `<linearGradient>`, `<radialGradient>`). Zero nested bordered cards ("no card-inside-card"). Zero Unicode emojis in UI badges/controls. Zero unsolicited verbose greeting paragraphs or microcopy clutter. Never break existing local/Google login, forgot/reset password, first-time setup, or user manual `ThemeToggle` overrides during an active holiday.
- **Execution:** Code (`ce-work` after user approval).

## Product Contract

### Summary

Introduce a calendar-driven **Vietnamese Holiday Seasonal System** on **Web** covering **18 holidays** (both Solar and Vietnamese Lunar dates). Each holiday opens **14 days prior to its `startDate`** (15 calendar days total for a 1-day holiday, e.g. `15/01` runs `01/01 → 15/01 23:59:59`; multi-day holidays run `startDate - 14 days → endDate 23:59:59`). When two holiday windows overlap, **the earlier holiday (`startDate ASC`) runs through the end of its `endDate`**, and the next holiday takes over on the following calendar day (`endDate + 1`).

Visuals are structured as **independent, pluggable Design Modules** (one module per design + one `default` Corporate EdTech module). When a holiday becomes active, the system swaps in that holiday's Design Module without branching logic inside `LoginUI.jsx`:
1. **Pluggable Login Design Modules:** Each holiday module encapsulates its own kicker badge + icon, hero stage illustration/layout, ambient background ornaments, and Light/Dark scoped color tokens. Editing or adding a holiday touches only its module file.
2. **Global System Theme (Approach A2 — Web only):** `ThemeContext` automatically activates the holiday's paired preset from `src/theme/tokens.js` once per holiday occurrence (`year:holidayId`), while allowing the user to manually switch presets via `ThemeToggle` during that holiday and restoring their pre-holiday theme once no holiday is active.
3. **Hybrid Date Resolution (Approach B3 — Web only):** Frontend resolves `Asia/Ho_Chi_Minh` date synchronously on boot (0ms, zero flash) and reconciles against Backend `LocalDate.now(TimeUtil.VIETNAM_ZONE)` returned on public `GET /api/presence/count`.
4. **Offline Desktop Exclusion:** `desktop` and `desktop-lecturer` modes ignore holiday design/theme switching completely (no desktop `.zip` rebuild needed).

### Problem Frame

The Login screen and global theme currently remain static year-round. Adding automated Vietnamese holiday skins brings seasonal warmth and cultural identity for EIU students and lecturers, while isolating each holiday into its own self-contained Design Module keeps `LoginUI` maintainable and prevents monolithic CSS/JSX sprawl.

### Key Decisions

- **All 18 Vietnamese & campus holidays included** — covers national public holidays, lunar festivals (Tết Nguyên Đán, Giỗ Tổ Hùng Vương, Tết Trung Thu), Vietnamese educational/commemorative days (`09/01`, `03/02`, `08/03`, `26/03`, `19/05`, `27/07`, `20/10`, `20/11`, `22/12`), and campus international observances (`14/02`, `31/10`, `24–25/12`). *(session-settled: user-directed)* Governs R1, R2.
- **Window = `[startDate - 14 days, endDate 23:59:59]`** — a 1-day holiday on `15/01` is active from `01/01` through `15/01` (15 days inclusive) and exits on `16/01`; a multi-day holiday `[startDate..endDate]` starts `startDate - 14 days` and exits after `endDate`. *(session-settled: user-directed)* Governs R3.
- **Sequential overlap handoff** — when windows overlap, the chronologically earlier holiday (`startDate ASC`, tie-broken by `priority DESC`) holds the active state through `23:59:59` of its `endDate`; the next day (`endDate + 1`), the next active holiday takes over (e.g. `22/12` ends `22/12` → `23–25/12` Christmas → `26/12–01/01` New Year). *(session-settled: user-directed)* Governs R4.
- **Pluggable Design Modules per holiday** — split visual designs into self-contained modules registered by `holidayId` (plus `default` non-holiday module) so switching holidays swaps the active module cleanly and keeps code easy to maintain. *(session-settled: user-directed)* Governs R7, R12.
- **Global Theme Preset + Login Holiday Design (Approach A2, Web only)** — switches both the Login Design Module and the system-wide theme preset on Web, with a per-holiday-instance watermark so manual `ThemeToggle` choices still work within that holiday. *(session-settled: user-directed)* Governs R5, R6, R13.
- **Hybrid Vietnam Date Resolution (Approach B3, Web only)** — instant client-side `Asia/Ho_Chi_Minh` calculation at mount + server `LocalDate.now(TimeUtil.VIETNAM_ZONE)` sync via `GET /api/presence/count` cached locally to guard against skewed client clocks. *(session-settled: user-directed)* Governs R8, R9.
- **Offline Desktop excluded** — `VITE_APP_MODE === 'desktop'` and `'desktop-lecturer'` bypass holiday resolution, server date sync, and seasonal preset auto-switching. *(session-settled: user-directed)* Governs R13.

### Holiday Catalog & Design Module Mapping (18 Holidays)

| # | `id` | Design Module File | Kicker Label | Calendar & Official Date(s) | Duration | Global Preset | Visual Motif (Flat Geometric SVG, Zero Gradient) |
|---|---|---|---|---|---|---|---|
| 1 | `new-year` | `designs/newYearDesign.jsx` | `TẾT DƯƠNG LỊCH · 01/01` | Solar `01/01` | 1 day | `light` | Geometric fireworks, calendar badge `01`, gold stars |
| 2 | `student-day` | `designs/studentDayDesign.jsx` | `NGÀY HỌC SINH SINH VIÊN · 09/01` | Solar `09/01` | 1 day | `light` | Mortarboard cap, knowledge torch, academic pennant |
| 3 | `party-foundation` | `designs/partyFoundationDesign.jsx` | `THÀNH LẬP ĐẢNG CSVN · 03/02` | Solar `03/02` | 1 day | `scarlet` | Crimson banner, 5-point gold star, geometric laurel |
| 4 | `valentine` | `designs/valentineDesign.jsx` | `VALENTINE · 14/02` | Solar `14/02` | 1 day | `scarlet` | Faceted origami hearts, sealed envelope, rose planes |
| 5 | `tet-nguyen-dan` | `designs/tetNguyenDanDesign.jsx` | `TẾT NGUYÊN ĐÁN` | Lunar `29/12 ÂL → 05/01 ÂL` | 6–7 days | `scarlet` | Mai & đào blossoms, red-gold lì xì envelope, festive lantern |
| 6 | `intl-womens-day` | `designs/intlWomensDayDesign.jsx` | `QUỐC TẾ PHỤ NỮ · 08/03` | Solar `08/03` | 1 day | `sand` | Geometric tulip & camellia blooms, coral-amber ribbons |
| 7 | `youth-union` | `designs/youthUnionDesign.jsx` | `THÀNH LẬP ĐOÀN TNCS · 26/03` | Solar `26/03` | 1 day | `evergreen` | Youth green-cobalt badge, ascending swallow wings, star |
| 8 | `hung-kings` | `designs/hungKingsDesign.jsx` | `GIỖ TỔ HÙNG VƯƠNG · 10/03 ÂL` | Lunar `10/03 ÂL` | 1 day | `sand` | Đông Sơn bronze drum sun & Lạc bird geometric motif |
| 9 | `reunification-labor` | `designs/reunificationLaborDesign.jsx` | `THỐNG NHẤT & LAO ĐỘNG · 30/04 - 01/05` | Solar `30/04 → 01/05` | 2 days | `scarlet` | Gold star emblem, peace dove silhouette, victory ribbons |
| 10 | `ho-chi-minh-birthday` | `designs/hoChiMinhBirthdayDesign.jsx` | `SINH NHẬT BÁC · 19/05` | Solar `19/05` | 1 day | `sand` | Stylized lotus bloom, open book, bamboo stalks |
| 11 | `war-invalids-martyrs` | `designs/warInvalidsMartyrsDesign.jsx` | `TRI ÂN THƯƠNG BINH LIỆT SĨ · 27/07` | Solar `27/07` | 1 day | `graphite` | Solemn tribute candle, white-gold lotus, monument arch |
| 12 | `national-day` | `designs/nationalDayDesign.jsx` | `QUỐC KHÁNH VIỆT NAM · 02/09` | Solar `01/09 → 02/09` | 2 days | `scarlet` | Crimson flag & gold star, Ba Đình geometric columns, lotus |
| 13 | `mid-autumn` | `designs/midAutumnDesign.jsx` | `TẾT TRUNG THU · 15/08 ÂL` | Lunar `14/08 ÂL → 15/08 ÂL` | 2 days | `sand` | Full harvest moon, 5-point star lantern, stylized clouds |
| 14 | `vn-womens-day` | `designs/vnWomensDayDesign.jsx` | `PHỤ NỮ VIỆT NAM · 20/10` | Solar `20/10` | 1 day | `sand` | Lotus blossom, geometric nón lá silhouette, amber-rose petals |
| 15 | `halloween` | `designs/halloweenDesign.jsx` | `HALLOWEEN · 31/10` | Solar `31/10` | 1 day | `dark` | Crescent moon, minimal faceted lantern & star silhouettes |
| 16 | `teachers-day` | `designs/teachersDayDesign.jsx` | `NGÀY NHÀ GIÁO VIỆT NAM · 20/11` | Solar `20/11` | 1 day | `sand` | Open book, fountain pen nib, tribute bouquet & blackboard |
| 17 | `peoples-army-day` | `designs/peoplesArmyDayDesign.jsx` | `QUÂN ĐỘI NHÂN DÂN VN · 22/12` | Solar `22/12` | 1 day | `evergreen` | Pine-olive shield, gold 5-point star, evergreen branch |
| 18 | `christmas` | `designs/christmasDesign.jsx` | `GIÁNG SINH · 24/12 - 25/12` | Solar `24/12 → 25/12` | 2 days | `evergreen` | Layered geometric pine tree, crisp snowflakes, gift cube |

*(When no holiday is active, `designs/defaultDesign.jsx` renders the standard Corporate EdTech `oop-stage.json` Lottie, `EIU · SCIT` kicker, and 3D architectural backdrop planes.)*

### Actors

- **A1. Web Visitor / Student / Lecturer / Admin** — views Login / auth screens and uses signed-in web dashboards under the active seasonal preset; may manually toggle themes via `ThemeToggle`.
- **A2. Frontend Calendar, Registry & Theme Engine** — resolves active holiday synchronously in `Asia/Ho_Chi_Minh`, mounts the matching Design Module, applies the global preset, and reconciles against server date.
- **A3. Backend API** — supplies authoritative `serverDate` (`LocalDate.now(TimeUtil.VIETNAM_ZONE)`) in `GET /api/presence/count`.

### Key Flows

- **F1. Zero-flash holiday Design Module & global preset on initial Web visit**
  - **Trigger:** Browser loads `/` (Login) or any signed-in web route during an active holiday window (e.g. `2026-11-18` → `teachers-day`).
  - **Actors:** A1, A2
  - **Steps:** Before React mounts, `ThemeContext` resolves today's Vietnam date, finds `teachers-day`, applies preset `sand` (recording `oop-holiday-applied-key = "2026:teachers-day"` and preserving prior manual theme in `oop-theme-user-choice`), and sets `data-holiday="teachers-day"` on `<html>`. `LoginUI` and `LoginBackground` look up `getHolidayDesign('teachers-day')` (`teachersDayDesign.jsx`) and render its self-contained background ornaments, hero stage, kicker badge, and scoped Light/Dark tokens with zero flicker.
  - **Covered by:** R1–R8, R12.

- **F2. Hybrid server-clock reconciliation (skewed client clock)**
  - **Trigger:** Client PC clock is misconfigured, and `GET /api/presence/count` responds with `serverDate`.
  - **Actors:** A2, A3
  - **Steps:** Frontend stores `{ serverDate, clientDate }` in `localStorage`, recomputes the active holiday from `serverDate`, and swaps the active Design Module + theme preset if different. If the API is unreachable, frontend fail-soft keeps the local date result.
  - **Covered by:** R8, R9.

- **F3. Sequential holiday transition**
  - **Trigger:** Date advances from `2026-12-25` (`christmas` `endDate`) to `2026-12-26`.
  - **Actors:** A2
  - **Steps:** `christmas` expires after `25/12 23:59:59`; `new-year` (`windowStart = 18/12`, `endDate = 01/01`) becomes active; the registry swaps `christmasDesign` → `newYearDesign` and `ThemeContext` updates `oop-holiday-applied-key` from `"2026:christmas"` to `"2027:new-year"`, switching default preset from `evergreen` to `light`.
  - **Covered by:** R3, R4, R5, R12.

- **F4. Manual ThemeToggle override during a holiday & post-holiday restore**
  - **Trigger:** During `tet-nguyen-dan` (auto-applied `scarlet`), user clicks `ThemeToggle` and chooses `light`. Later, after `Mùng 5 Tết`, no holiday is active.
  - **Actors:** A1, A2
  - **Steps:** Clicking `ThemeToggle` saves `oop-theme = "light"` and `oop-theme-user-choice = "light"` while keeping `oop-holiday-applied-key = "2027:tet-nguyen-dan"`, so reloads during Tết stay `light` (while `tetNguyenDanDesign` adapts via its `tokens.light` variables). Once Tết ends and no holiday is active, `defaultDesign` mounts, `oop-holiday-applied-key` clears, and `oop-theme-user-choice` remains active.
  - **Covered by:** R5, R6, R7, R12.

### Requirements

- **R1.** Support all 18 holidays listed in the Holiday Catalog, computing Solar dates directly and Vietnamese Lunar dates (`tet-nguyen-dan`, `hung-kings`, `mid-autumn`) accurately in UTC+7 (`Asia/Ho_Chi_Minh`) for any year without external network calls or npm calendar bloat.
- **R2.** For Tết Nguyên Đán, `startDate` is `29/12` of the ending lunar year (always valid for both 29-day and 30-day tháng Chạp) and `endDate` is `05/01` of the new lunar year (6 or 7 official days accordingly).
- **R3.** Each holiday occurrence is active on Vietnam calendar date `D` iff `startDate - 14 days <= D <= endDate`. At `00:00:00` Vietnam time on `endDate + 1`, that holiday is no longer active.
- **R4.** When multiple holiday occurrences cover `D` (`windowStart <= D <= endDate`), select the one with the earliest `startDate` (`startDate ASC`, tie-broken by `priority DESC`).
- **R5.** On Web, entering a new active holiday occurrence (`occurrenceKey = "${holidayYear}:${id}"` differs from stored `oop-holiday-applied-key`) automatically switches the global `ThemeContext` preset to that holiday's mapped preset and records `oop-holiday-applied-key`. Prior user manual preset (if any) is preserved in `oop-theme-user-choice` and restored when exiting to a non-holiday period.
- **R6.** Manual `ThemeToggle` selection always works immediately during an active holiday, persists across reloads for the remainder of that holiday occurrence, and updates `oop-theme-user-choice`.
- **R7.** `LoginUI` and `LoginBackground` delegate all holiday-dependent rendering to the active Design Module returned by the Holiday Design Registry (`defaultDesign` when `activeHoliday === null`), with zero per-holiday `if/switch` branching inside `LoginUI.jsx` or `LoginUI.css`.
- **R8.** Backend `GET /api/presence/count` includes `serverDate` (`YYYY-MM-DD` from `LocalDate.now(TimeUtil.VIETNAM_ZONE)`).
- **R9.** Web Frontend resolves the date synchronously at boot from local `Asia/Ho_Chi_Minh` (adjusted by same-day cached `serverDate` if present) and asynchronously reconciles via `GET /api/presence/count` on web mounts (including Login).
- **R10.** Strict Design System compliance across every Design Module: zero CSS or SVG gradients, zero nested bordered cards, zero Unicode emojis, zero unsolicited greeting paragraphs, and full `@media (prefers-reduced-motion: reduce)` / `MotionConfig` support.
- **R11.** In `import.meta.env.DEV` only, `?holiday=<id|none>` or `?holidayDate=YYYY-MM-DD` overrides the resolved holiday for rapid visual QA without changing OS clock; production builds ignore these query params.
- **R12.** Modular Design Contract: each holiday lives in its own module under `frontend/src/components/auth/holidays/designs/<name>Design.jsx` exporting a uniform interface (`id`, `kicker`, `KickerIcon`, `tokens: { light, dark }`, `BackgroundOrnaments`, `HeroStage`), registered centrally in `frontend/src/components/auth/holidays/registry.js`. Adding, editing, or removing a holiday design only touches its module and registry entry.
- **R13.** Offline Desktop Exclusion: when `VITE_APP_MODE` is `'desktop'` or `'desktop-lecturer'`, `activeHoliday` always resolves to `null`, no seasonal preset switching runs, and no server date sync runs.

### Acceptance Examples

- **AE1. Single-day 15-day window (`20/11`)**
  - **Given:** Vietnam date is `2026-11-06` through `2026-11-20` on Web
  - **When:** User opens Login
  - **Then:** `teachersDayDesign` module + `sand` preset are active; on `2026-11-21`, `defaultDesign` returns.

- **AE2. Sequential handoff (`22/12` → `Giáng Sinh` → `Tết Dương lịch` → `09/01`)**
  - **Given:** Dates across December–January on Web
  - **Then:** `08/12–22/12` mounts `peoplesArmyDayDesign`; `23/12–25/12` mounts `christmasDesign`; `26/12–01/01` mounts `newYearDesign`; `02/01–09/01` mounts `studentDayDesign`; `10/01` mounts `defaultDesign`.

- **AE3. Lunar holiday accuracy (`Tết Nguyên Đán` & `Trung Thu`)**
  - **Given:** Year `2026` (`17/02/2026` Mùng 1 Tết; `25/09/2026` Rằm Trung Thu `15/08 ÂL`) and `2027` (`06/02/2027` Mùng 1 Tết)
  - **Then:** Vietnamese lunar conversion resolves exact `startDate`/`endDate` in UTC+7 and mounts `tetNguyenDanDesign` / `midAutumnDesign` accurately.

- **AE4. Skewed client clock corrected by Backend**
  - **Given:** Student laptop clock is stuck on `2026-06-15` (no holiday), while server `LocalDate.now(TimeUtil.VIETNAM_ZONE)` is `2026-11-18`
  - **When:** `GET /api/presence/count` returns `{ count, serverDate: "2026-11-18" }`
  - **Then:** Client caches the sync and swaps from `defaultDesign` to `teachersDayDesign`.

- **AE5. Offline Desktop untouched**
  - **Given:** App runs with `VITE_APP_MODE=desktop` or `desktop-lecturer` during Tết
  - **Then:** Theme remains standard `light`/`dark` default; no holiday overrides or network sync occur.

### Scope Boundaries

- **In scope:** 18-holiday Solar + Vietnamese Lunar calendar engine; Backend `serverDate` in `ActiveUsersDTO`; Web-only `ThemeContext` seasonal preset state machine; pluggable per-holiday Design Modules (`frontend/src/components/auth/holidays/designs/*.jsx` + `defaultDesign.jsx` + `registry.js`); thin slot integration in `LoginUI` & `LoginBackground`; dev preview query param; unit tests (Backend + Frontend Vitest).
- **Out of scope:** Offline Desktop (`desktop` / `desktop-lecturer`) seasonal designs or `.zip` bundle rebuilds; changing authentication flows/validation; adding database tables or admin CRUD for holidays; adding CSS/SVG gradients or audio effects.

---

## Planning Contract

### Key Technical Decisions

- **KTD1. Pure-math Hồ Ngọc Đức Vietnamese Lunar Converter (`UTC+7`)** — implement `frontend/src/utils/vietnameseLunar.js` (~70 lines of pure integer/astronomy math for Julian Day ↔ Vietnamese Lunar Date at meridian `UTC+7`). Zero npm packages, 100% offline, deterministic across centuries, and unit-tested in Vitest against official Vietnamese dates (2025–2035, including leap months and 29/30 tháng Chạp). Governs R1, R2.
- **KTD2. Cross-year candidate expansion `[Y - 1, Y, Y + 1]`** — `resolveActiveHoliday(isoDate)` instantiates the 18 holidays for `year - 1`, `year`, and `year + 1` so December→January solar windows (`new-year`, `student-day`) and January Lunar New Year windows never miss across calendar-year boundaries. Candidates matching `windowStart <= isoDate <= endDate` are sorted by `startDate ASC, priority DESC`. Governs R3, R4.
- **KTD3. Extend `ActiveUsersDTO` instead of adding a new endpoint** — `ActiveUsersDTO(int count, String serverDate)` adds `LocalDate.now(TimeUtil.VIETNAM_ZONE).toString()` to `GET /api/presence/count` (and `DELETE /api/presence/leave`). Keep convenience constructor `ActiveUsersDTO(int count)` delegating to `LocalDate.now(TimeUtil.VIETNAM_ZONE).toString()` for compatibility. Already public, CORS-enabled, and excluded from `BurstLimitFilter`. Governs R8.
- **KTD4. Shared server-date cache in `presence.js` + one-shot Web Login sync** — `fetchPresenceCount()` extracts `body.serverDate` (when valid `YYYY-MM-DD`) and writes `{ serverDate, clientDate }` to `localStorage['oop-vn-date-sync']`, dispatching window custom event `oop-server-date-sync` when `effectiveDate` changes. On Web Login, `ThemeProvider` triggers one background `syncVietnamDateOnce()` if today's `clientDate` has not yet been synced in this session. Skipped completely when `isDesktopApp` (`desktop` or `desktop-lecturer`). Governs R9, R13.
- **KTD5. Non-destructive Global Preset State Machine in `ThemeContext.jsx` (Web only)** — storage keys:
  - `oop-theme`: active preset id
  - `oop-theme-user-choice`: last preset explicitly clicked by the user (or pre-holiday stored `oop-theme`)
  - `oop-holiday-applied-key`: `"${occurrenceKey}"` (e.g. `"2026:teachers-day"`)
  When `isDesktopMode || isDesktopLecturerMode`, `activeHoliday` is forced `null` and existing `resolveInitialTheme()` runs unchanged. On Web, when `activeHoliday` has a new `occurrenceKey !== appliedKey`, set active theme to `activeHoliday.presetId` and write `oop-holiday-applied-key`. When `activeHoliday === null` and `appliedKey` exists, clear `appliedKey` and restore `oop-theme-user-choice ?? systemDefault`. Governs R5, R6, R13.
- **KTD6. Self-Contained Design Module Architecture (`frontend/src/components/auth/holidays/`)** —
  ```text
  frontend/src/components/auth/holidays/
  ├── registry.js                  # getLoginDesign(holidayId) -> returns matching module or defaultDesign
  ├── sharedPrimitives.jsx         # optional tiny reusable SVG geometric primitives (floating wrapper, star, petal, cloud)
  └── designs/
      ├── defaultDesign.jsx        # Standard Corporate EdTech (oop-stage.json Lottie + 3D arch planes + EIU · SCIT)
      ├── newYearDesign.jsx        # 01/01 Tết Dương lịch
      ├── studentDayDesign.jsx     # 09/01 Ngày Học sinh - Sinh viên
      ├── partyFoundationDesign.jsx# 03/02 Ngày thành lập Đảng CSVN
      ├── valentineDesign.jsx      # 14/02 Valentine
      ├── tetNguyenDanDesign.jsx   # Tết Nguyên Đán (Âm lịch)
      ├── intlWomensDayDesign.jsx  # 08/03 Quốc tế Phụ nữ
      ├── youthUnionDesign.jsx     # 26/03 Thành lập Đoàn TNCS
      ├── hungKingsDesign.jsx      # 10/03 ÂL Giỗ Tổ Hùng Vương
      ├── reunificationLaborDesign.jsx # 30/04 - 01/05
      ├── hoChiMinhBirthdayDesign.jsx  # 19/05 Sinh nhật Bác
      ├── warInvalidsMartyrsDesign.jsx # 27/07 Thương binh - Liệt sĩ
      ├── nationalDayDesign.jsx    # 02/09 Quốc khánh Việt Nam
      ├── midAutumnDesign.jsx      # 15/08 ÂL Tết Trung Thu
      ├── vnWomensDayDesign.jsx    # 20/10 Phụ nữ Việt Nam
      ├── halloweenDesign.jsx      # 31/10 Halloween
      ├── teachersDayDesign.jsx    # 20/11 Ngày Nhà giáo Việt Nam
      ├── peoplesArmyDayDesign.jsx # 22/12 Quân đội Nhân dân VN
      └── christmasDesign.jsx      # 24-25/12 Giáng Sinh
  ```
  Each module exports a uniform contract:
  ```js
  export default {
    id: 'tet-nguyen-dan',
    kicker: 'TẾT NGUYÊN ĐÁN',
    KickerIcon: FlowerLotus, // Phosphor Duotone icon
    tokens: {
      light: {
        '--login-kicker-bg': '#991B1B',
        '--login-kicker-fg': '#FFFBEB',
        '--login-stage-bg': '#FEF2F2',
        '--login-corner-tint': 'rgba(220, 38, 56, 0.10)',
        '--login-accent': '#DC2638',
        '--login-accent-hover': '#B91C2C',
      },
      dark: { ... },
    },
    BackgroundOrnaments: TetBackgroundOrnaments,
    HeroStage: TetHeroStage,
  };
  ```
  `LoginUI.jsx` and `LoginBackground.jsx` read `const { activeHoliday, isDark } = useTheme()` and `const design = getLoginDesign(activeHoliday?.id)`, applying `style={isDark ? design.tokens.dark : design.tokens.light}` on `.login-bg` and rendering `<design.BackgroundOrnaments />` and `<design.HeroStage />`. Zero giant CSS switch blocks in `LoginUI.css`! Governs R7, R10, R12.

### High-Level Technical Design

```text
[Web Boot (synchronous before React mount; bypassed on Desktop)]
  getEffectiveVietnamDate()
    ├─ DEV query (?holidayDate / ?holiday) if import.meta.env.DEV
    ├─ localStorage['oop-vn-date-sync'] if clientDate === localVietnamToday()
    └─ localVietnamToday() via Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' })
         │
         ▼
  resolveActiveHoliday(isoDate)  ──►  activeHoliday { id, occurrenceKey, presetId, ... } | null
         │
         ▼
  ThemeContext (Web seasonal preset state machine; Desktop keeps standard light/dark)
    ├─ applies holiday presetId once per occurrenceKey (preserves manual ThemeToggle override)
    ├─ exposes { theme, isDark, activeHoliday, ... }
    └─ on web mount: one-shot syncVietnamDateOnce() -> reconciles serverDate if skewed
         │
         ▼
  getLoginDesign(activeHoliday?.id)  ──►  designs/<holiday>Design.jsx  (or defaultDesign.jsx)
         ├─ <design.BackgroundOrnaments />  inside LoginBackground (shared across Login/Forgot/Reset/Setup)
         ├─ <design.KickerIcon /> + design.kicker + <design.HeroStage />  inside LoginUI hero pane
         └─ design.tokens[isDark ? 'dark' : 'light']  scoped CSS custom properties on .login-bg
```

### Sequencing

1. **U1.** Backend `ActiveUsersDTO` `serverDate` (`TimeUtil.VIETNAM_ZONE`) + Backend unit tests
2. **U2.** Frontend Vietnamese Lunar & 18-Holiday Calendar Engine + Hybrid date sync + Vitest suite
3. **U3.** `ThemeContext` Web-only seasonal global preset state machine (Desktop excluded) + Vitest tests
4. **U4.** Pluggable Design Modules (`holidays/registry.js`, `defaultDesign.jsx`, and 18 holiday design modules) + thin slot wiring in `LoginUI.jsx`, `LoginBackground.jsx`, and `LoginUI.css` + Registry contract Vitest test
5. **U5.** DOX documentation pass & full verification (`mvn test`, `npm test`, `npm run build`)

---

## Implementation Units

### U1. Backend Vietnam `serverDate` in Presence API

- **Goal:** Expose authoritative Vietnam calendar date (`YYYY-MM-DD`) on `GET /api/presence/count` with zero extra endpoints or security matcher changes.
- **Requirements:** R8
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/DTO/ActiveUsersDTO.java`
  - `backend/src/main/java/com/eiu/capstone/backend/controller/PresenceController.java`
  - `backend/src/test/java/unit/com/eiu/capstone/backend/controller/PresenceControllerTest.java`
- **Approach:** Add `String serverDate` to `ActiveUsersDTO(int count, String serverDate)` and populate via `LocalDate.now(TimeUtil.VIETNAM_ZONE).toString()`. Keep secondary constructor `ActiveUsersDTO(int count)` for compatibility.
- **Test scenarios:**
  - `GET /api/presence/count` returns `count` and ISO `serverDate` matching `^\d{4}-\d{2}-\d{2}$` in `Asia/Ho_Chi_Minh`.
  - Revoked bearer token still returns `401` (existing contract preserved).
- **Verification:** `mvn test` from `backend/`

### U2. Vietnamese Lunar Converter, 18-Holiday Calendar Engine & Hybrid Date Sync

- **Goal:** Accurately compute active holiday across all 18 Solar & Lunar holidays, enforce the 15-day window (`startDate - 14d .. endDate`) and sequential handoff (`startDate ASC`), and wire Web-only Hybrid server-date sync.
- **Requirements:** R1, R2, R3, R4, R9, R11, R13
- **Files:**
  - `frontend/src/utils/vietnameseLunar.js`
  - `frontend/src/utils/holidayCalendar.js`
  - `frontend/src/utils/presence.js`
  - `frontend/src/utils/__tests__/holidayCalendar.test.js`
- **Approach:**
  - Implement `lunarToSolar(lunarDay, lunarMonth, lunarYear, leap)` in `vietnameseLunar.js` (UTC+7).
  - Define the 18-holiday schedule in `holidayCalendar.js` (`id`, `presetId`, `priority`, solar/lunar date resolver) and `resolveActiveHoliday(isoDate)` across `[Y-1, Y, Y+1]`.
  - Update `presence.js` (skipped on `desktop` and `desktop-lecturer`) to persist `{ serverDate, clientDate }` and emit `oop-server-date-sync` when effective date changes; export `syncVietnamDateOnce()`.
- **Test scenarios:**
  - Lunar-to-solar conversion matches official Vietnamese dates for Tết Nguyên Đán (`29 Chạp` & `Mùng 1..5`), Giỗ Tổ (`10/03 ÂL`), and Trung Thu (`14–15/08 ÂL`) across 2025, 2026, 2027, 2028, 2030.
  - 1-day holiday (`20/11`): inactive `05/11`, active `06/11`..`20/11`, inactive `21/11`.
  - Multi-day holiday (`30/04–01/05`): active `16/04`..`01/05`, inactive `02/05`.
  - Sequential overlap chain: `08/12–22/12` (`peoples-army-day`) → `23/12–25/12` (`christmas`) → `26/12–01/01` (`new-year`) → `02/01–09/01` (`student-day`) → `10/01` (`null`).
  - Tết vs `03/02` & `14/02` across 2026, 2027, 2028 resolves deterministically.
  - Skewed client date reconciles when `oop-vn-date-sync` holds server date.
- **Verification:** `npm test` from `frontend/`

### U3. `ThemeContext` Web-Only Seasonal Global Preset State Machine

- **Goal:** Auto-apply the active holiday's global theme preset once per holiday occurrence on Web while keeping Offline Desktop completely untouched and preserving manual `ThemeToggle` freedom.
- **Requirements:** R5, R6, R9, R11, R13
- **Files:**
  - `frontend/src/context/ThemeContext.jsx`
  - `frontend/src/utils/__tests__/holidayThemeState.test.js`
- **Approach:**
  - Extract pure helper `resolveSeasonalThemeState({ storedTheme, userChoice, appliedHolidayKey, activeHoliday, systemDark, isDesktop })` and unit-test every transition in Vitest.
  - When `isDesktop` (`VITE_APP_MODE === 'desktop' || VITE_APP_MODE === 'desktop-lecturer'`), `activeHoliday` is `null` and seasonal preset logic is bypassed.
  - On Web, expose `activeHoliday` from `useTheme()`, set `root.dataset.holiday = activeHoliday?.id ?? ''`, trigger `syncVietnamDateOnce()` on mount, and re-evaluate on `oop-server-date-sync` and `visibilitychange`.
- **Test scenarios:**
  - Web entering `"2026:teachers-day"` switches stored `"light"` to `"sand"` and saves `userChoice = "light"`.
  - Manual `setTheme("dark")` during `"2026:teachers-day"` keeps `"dark"` on subsequent reloads during `"2026:teachers-day"`.
  - Advancing to `"2026:peoples-army-day"` auto-applies `"evergreen"`.
  - Exiting to `activeHoliday = null` restores `userChoice` (`"dark"`).
  - Desktop mode (`isDesktop: true`) ignores `activeHoliday` and keeps stored/system theme unchanged.
- **Verification:** `npm test` from `frontend/`

### U4. Pluggable Holiday Design Modules & Thin Login Slot Wiring

- **Goal:** Build the modular `frontend/src/components/auth/holidays/` system with `defaultDesign.jsx` + 18 dedicated holiday Design Modules, keeping `LoginUI.jsx`, `LoginBackground.jsx`, and `LoginUI.css` clean and holiday-agnostic.
- **Requirements:** R7, R10, R12
- **Files:**
  - `frontend/src/components/auth/holidays/registry.js`
  - `frontend/src/components/auth/holidays/sharedPrimitives.jsx`
  - `frontend/src/components/auth/holidays/designs/defaultDesign.jsx`
  - `frontend/src/components/auth/holidays/designs/*.jsx` (18 self-contained holiday design modules)
  - `frontend/src/components/ui/LoginBackground.jsx`
  - `frontend/src/pages/LoginUI.jsx`
  - `frontend/src/pages/LoginUI.css`
  - `frontend/src/utils/__tests__/holidayRegistry.test.js`
- **Approach:**
  - Create `defaultDesign.jsx` wrapping the current Corporate EdTech backdrop planes, `EIU · SCIT` kicker, and `oop-stage.json` Lottie.
  - Create 18 distinct, isolated design modules under `designs/` — each owning its own `kicker`, Phosphor Duotone `KickerIcon`, Light/Dark CSS variable map (`tokens.light`, `tokens.dark`), `BackgroundOrnaments`, and `144×144` flat vector `HeroStage`.
  - Update `LoginBackground.jsx` and `LoginUI.jsx` to consume `getLoginDesign(activeHoliday?.id)` via clean slots + CSS variables (`var(--login-kicker-bg, var(--navy))`, `var(--login-accent, var(--primary))`, etc. in `LoginUI.css`).
  - Add `holidayRegistry.test.js` asserting every holiday ID in `holidayCalendar.js` has a registered design module conforming to the module interface (`id`, `kicker`, `KickerIcon`, `tokens.light`, `tokens.dark`, `BackgroundOrnaments`, `HeroStage`).
- **Test scenarios:**
  - Registry contract test verifies 1-to-1 coverage for all 18 holidays + fallback to `defaultDesign`.
  - All 18 `?holiday=<id>` modules + `?holiday=none` render cleanly in Light & Dark modes with zero CSS/SVG gradients, zero nested borders, and zero emojis.
  - `ForgotPasswordUI`, `ResetPasswordUI`, and `FirstTimeSetupUI` automatically render the active module's `BackgroundOrnaments` and scoped background tokens.
- **Verification:** `npm test` + `npm run build` + grep check confirming zero `gradient` in `frontend/src/`

### U5. DOX Pass & End-to-End Verification

- **Goal:** Update owning `AGENTS.md` contracts and run full backend/frontend verification suites.
- **Requirements:** R1–R13
- **Files:**
  - `backend/AGENTS.md` (`PresenceController` `serverDate` note)
  - `frontend/AGENTS.md` (Web-only `ThemeContext` seasonal preset + Hybrid date sync note)
  - `frontend/src/pages/AGENTS.md` (`LoginUI` modular holiday design registry contract)
- **Verification:**
  - `mvn test` in `backend/`
  - `npm test` in `frontend/`
  - `npm run build` (plus smoke check `npm run build:desktop` to ensure no broken imports)

## Verification Contract

- Backend unit/controller suite green via `mvn test` in `backend/`.
- Frontend Vitest suite green via `npm test` in `frontend/` (covering Lunar conversion, 18-holiday windows & sequential handoff, `ThemeContext` Web-only state machine + Desktop exclusion, and 18-module Design Registry contract).
- Production Vite build succeeds (`npm run build`).
- Static check verifies zero occurrences of `linear-gradient`, `radial-gradient`, `conic-gradient`, `<linearGradient`, or `<radialGradient` across `frontend/src/`.

## Definition of Done

- All 18 holidays resolve accurately in `Asia/Ho_Chi_Minh` (Solar + Vietnamese Lunar) with `[startDate - 14d, endDate]` windows and sequential `startDate ASC` handoff.
- Each holiday's Login visuals live in its own self-contained Design Module under `frontend/src/components/auth/holidays/designs/`, swapped cleanly via `registry.js` without branching inside `LoginUI.jsx`.
- Hybrid date sync (`GET /api/presence/count` → `serverDate`) protects Web clients against skewed clocks without delaying first paint.
- Global theme preset switches automatically per holiday occurrence on Web while respecting manual `ThemeToggle` changes and restoring post-holiday preference; Offline Desktop builds remain completely unaffected.
- Tests, builds, and DOX documentation updates complete.
