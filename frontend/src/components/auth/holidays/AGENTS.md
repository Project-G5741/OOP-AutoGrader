# Holiday Login Theme & Session Ambient Effects

## Purpose

Pluggable 2K HD holiday backgrounds and login pop-up ornaments for the 7 major Vietnamese & campus holidays (Login/Auth screens only), paired with a session-wide ambient animation layer (`SessionHolidayEffects.jsx`) that persists across the entire web session.

## Ownership

| File | Role |
|---|---|
| `registry.js` | Central registry `HOLIDAY_BACKGROUND_REGISTRY` & `getHolidayBackgroundEntry(holidayId)` mapping the 7 major holiday IDs (`halloween`, `christmas`, `new-year`, `tet-nguyen-dan`, `reunification-labor`, `childrens-day`, `mid-autumn`) to their background `Scene`, Login pop-up metadata (`kicker`, `KickerIcon`, `popupTokens`), and session-wide particle/corner config (`sessionEffects`) |
| `LoginCardHolidayDecor.jsx` | 2D holiday decorative corner/rim ornaments (`LoginCardHolidayDecor`) rendered on `.login-unified-card` and holiday logo accessories (`LogoHolidayAccessory`: Santa hat, New Year top hat with holly, Hoa Mai tassel, Jade Rabbit ears, Golden Star rosette, Witch hat with buckle, Pinwheel) perched on the Login `AppLogo` box |
| `SessionHolidayEffects.jsx` | Session-wide `fixed` (`pointer-events: none`, `z-index: 45`) ambient animation overlay mounted at the root of `App.jsx` so falling snow, corner cobwebs with dangling spider & flying bats, Mai/Đào petals, rising sky lanterns, paper airplanes, and star confetti persist across all web routes throughout the session |
| `sharedPrimitives.jsx` | Shared 2D decorative drawing primitives (`AtmosphericAura`, `PaintedBlossomCluster`, `PaintedLotus2D`, `HoiAnLantern2D`, and `HolidayParticleLayer`) |
| `HolidayDevPreviewBar.jsx` | Compact floating preview switcher (`import.meta.env.DEV` only, web only) for stepping through `Auto`, `Mặc định`, and all 7 major holidays |
| `scenes/DefaultBackgroundScene.jsx` | Default Corporate EdTech 3D architectural backdrop planes when no holiday is active |
| `scenes/HalloweenScene.jsx` | Halloween (`31/10`) 2K HD background (`/holidays/halloween-bg.webp` + glowing pumpkin-lantern halos & rolling ground mist) |
| `scenes/ChristmasScene.jsx` | Noel / Giáng Sinh (`24–25/12`) 2K HD background (`/holidays/christmas-bg.webp` + twinkling fairy-light halos) |
| `scenes/NewYearScene.jsx` | Tết Dương Lịch (`01/01`) 2K HD background (`/holidays/new-year-bg.webp` + twinkling string-light, firework & streetlamp halos) |
| `scenes/TetNguyenDanScene.jsx` | Tết Nguyên Đán (`29 Chạp – Mùng 5 ÂL`) 2K HD background (`/holidays/tet-nguyen-dan-bg.webp` + glowing lantern halos) |
| `scenes/LaborDayScene.jsx` | Tết Lao Động (`30/04 – 01/05`) 2K HD background (`/holidays/reunification-labor-bg.webp`) |
| `scenes/ChildrensDayScene.jsx` | Tết Thiếu Nhi (`01/06`) 2K HD background (`/holidays/childrens-day-bg.webp` + sunbeam & star highlights) |
| `scenes/MidAutumnScene.jsx` | Tết Trung Thu (`15/08 ÂL`) 2K HD background (`/holidays/mid-autumn-bg.webp` + glowing star-lantern & sky-lantern halos) |

## Local Contracts

- **Automatic 20-Day Pre-Holiday Schedule & Admin Early Activation (`Asia/Ho_Chi_Minh`):** `src/utils/holidayCalendar.js` (`HOLIDAY_WINDOW_PRE_DAYS = 20`) automatically activates each major holiday on Web from `[startDate - 20 days, endDate]` in Vietnam time (`UTC+7`). In addition, `ADMIN` users can activate or schedule any holiday early from the **Holidays** nav tab (`src/pages/HolidayManagement.jsx`, `/lecturer-holidays`, backed by `GET /api/auth/holiday-config` & `PUT /api/lecturer/holiday-config` + `localStorage` key `oop-holiday-admin-config`); once the Vietnam calendar date passes `endDate` (`todayIso > endDate`), the early activation turns off automatically.
- **2K HD Background Clarity:** All 7 holiday background assets under `public/holidays/<id>-bg.webp` are upscaled and sharpened to `2048×1144` WebP (`quality=96`) and rendered with `image-rendering: -webkit-optimize-contrast` and GPU compositing (`transform: translateZ(0)`).
- **Login-Only Theme Boundary:** Full holiday background scenes (`LoginBackground.jsx`), login pop-up ornaments (`LoginCardHolidayDecor`, `LogoHolidayAccessory`), compact holiday card width (`max-width: 820px` so left/right background characters remain visible), and scoped CSS variables (`--login-kicker-bg`, `--login-accent`, `--login-logo-bg`, `--login-hero-bg`, `--login-card-border`) apply strictly on the Login / Auth screens (`LoginUI`, `ForgotPasswordUI`, `ResetPasswordUI`, `FirstTimeSetupUI`). Global dashboard theme presets in `ThemeContext` are not altered.
- **Session-Wide Ambient Animations:** `SessionHolidayEffects.jsx` mounts in `App.jsx` so ambient particles (falling snow, cobwebs with dangling spider & bats, Mai/Đào petals, rising sky lanterns, paper airplanes, star confetti) continue non-intrusively (`pointer-events: none`) across the entire web session.
- **Zero Gradients:** Strictly zero `linear-gradient`, `radial-gradient`, `conic-gradient`, `<linearGradient>`, or `<radialGradient>`.
- **Offline Desktop Exclusion:** `isDesktopLocalMode()` (`desktop` and `desktop-lecturer`) resolves `activeHoliday` to `null`, rendering `DefaultBackgroundScene` and disabling `SessionHolidayEffects`.

## Work Guidance

- Place new holiday background assets under `public/holidays/<id>-bg.webp` and register them in `registry.js` and `src/utils/holidayCalendar.js`.
- Keep all animations compliant with `@media (prefers-reduced-motion: reduce)` in `src/pages/LoginUI.css`.

## Verification

- `npm test` from `frontend/` (`holidayCalendar.test.js`, `holidayRegistry.test.js`)
- `npm run build` from `frontend/`

## Child DOX Index

No child docs.
