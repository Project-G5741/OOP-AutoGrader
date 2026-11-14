# Student Components

## Purpose

Student-specific UI: submission history, profile editing. Also reused by lecturer for profile modal.

## Ownership

| File | Role |
|---|---|
| `StudentHistoryPage.jsx` | Expandable history table; live `my-history` / `my-labs` APIs |
| `StudentUI.jsx` | Student lab upload + result tabs; selected-lab header shows an Official or Practice badge; daily submission quota banner (`GET /api/students/daily-submission-quota`, web only) above DropZone; Operation Test when `scoreApplicability.testcase` is true (upload `lab_result` or revisit `/testcases`) |
| `StudentLabSidebar.jsx` | Left lab list (`Sidebar` + `Item`); selects `labId` for upload and results |
| `StudentNotificationBell.jsx` | Web-only bell + dropdown; unread items show a red dot on the row and on the bell; click a list item to mark that notification read (sessionStorage `oop-student-notif-read`); opening the bell alone does not mark read. Loads `GET /api/feedback/replies` as `Re: Bug Report` / `Re: Contribution`. On `pagehide` (and logout via `App.jsx`) calls `DELETE /api/feedback/replies` for shown reply ids so those feedback rows are hard-deleted. Not mounted in the desktop build |
| `MascotSprite.jsx` | Shared mascot painter for every student sheet. Canvas at the screen pixel ratio so a small header does not blur the sheet. Head-tracking is opt-in (`trackPointer`): the header and the gacha showcase follow the pointer; shop cards and pull thumbnails stay on the center frame and load the reaction sheet on click. Each normal click plays blink, then the next reaction cell in order (roar, smirk, slumber, wrath, burst, love, aura) and the sequence keeps advancing. Four clicks inside 420ms play dizzy instead |
| `StudentFoxMascot.jsx` | Compact header mascot avatar with speech bubbles (positioned directly below the mascot icon), 8-directional cursor head-tracking, and click boop reactions via `MascotSprite`. Web: header pets it (daily mission), triggers contextual dialogues. Desktop: paints `mascotId` from the last imported snapshot (fox until then); local click reactions, no mission API |
| `mascotBehaviors.js` | Mascot profile dialogues (perfect, good, retry, pet, idle) per mascot across Normal, Rare (`bunny`, `cat`, `vit`, `hamster`, `chuot_tui`, `shiba`, `dai_bang`, `cho_soi`), Epic (`thanh_long`, `bach_ho`, `chu_tuoc`, `huyen_vu`), and Legendary (`zerotwo`, `con_bang`, `cung_ky`, `linh_loc`, `ki_lan`) |
| `studentMascots.js` | Sheet paths (`directions`, `reactions`), tier classification (`normal`, `rare`, `epic`, `legendary`), and display names for student mascots: Normal (`fox`, `bear`, `panda`, `penguin`), Rare (`bunny`, `cat`, `vit`, `hamster`, `chuot_tui`, `shiba`, `dai_bang`, `cho_soi`), Epic (`thanh_long`, `bach_ho`, `chu_tuoc`, `huyen_vu`), and Legendary (`con_bang` and `cung_ky` 20000 pts or 4 Crystals, `zerotwo` 35000 pts or 7 Crystals, `linh_loc` and `ki_lan` 40000 pts or 8 Crystals). Teacher mascot is strictly excluded |
| `MascotShop.jsx` | Web-only shop dialog from the header points button. Purchase confirms, then `POST /api/students/mascots/{id}/purchase` or exchange for that mascot's Crystal cost (Côn Bằng and Cùng Kỳ 4, Zero2 7, Linh Lộc and Kì Lân 8). The crystal balance opens **Đổi điểm**: `POST /api/students/streak/crystals/exchange` turns crystals into reward points at 4000 each (up to 99 per confirm). Owned mascots are worn with `POST .../equip` and are the only cards that play the head-turn and reaction animation. Unowned cards stay on the still center frame. Features tier filters and link to Gacha summon. Not mounted in the desktop build |
| `MascotGachaModal.jsx` | Web-only cosmic summon / gacha modal. Supports 1x and 10x roll, summoning animations, rarity bursts, guaranteed Crystal pity at 80, points cashback, and interactive demo sandbox controls (Admin only: add tickets/points/crystals, set pity). Direct legendary drops (1 in 6,000) are Zero2, Côn Bằng, Cùng Kỳ, Linh Lộc, and Kì Lân. Rolls and those sandbox actions notify the parent. The modal does not write the controlled ticket count back while it is only receiving the parent's value |
| `StudentStreakFlame.jsx` | Web-only compact three-layer streak flame (`.u379`, styles in `StudentStreakFlame.css`) beside the mascot. Icon only, no plate. Grey until today's three missions are done, then the colored flame. Click opens daily missions. No separate Done button. `prefers-reduced-motion` draws one still flame and skips the count roll. Omitted on desktop |
| `DesktopMascotImport.jsx` | Desktop header control: paste the web practice code → `POST /api/desktop/mascot/sync`; Java pulls production `GET /api/students/mascot-snapshot` and writes `data/mascot-snapshot.json`; dashboard header mascot updates from the returned `equipped` id |
| `DesktopRubricPackImport.jsx` | Desktop header control: file picker → `POST /api/desktop/packs/import`; same-name lab conflict modal (`confirmReplace`); term import clears lab packs on disk; after success a blocking **Okay**-only dialog relaunches via `restartPracticeApp` (`practice-restart`; no submit until reopen finishes); friendly errors via `apiError` context `import` |
| `DesktopPracticeStatusBanner.jsx` | Shown in `App.desktop.jsx` when `GET /api/desktop/status` reports not ready; waits until `bootstrapComplete` then applies that one settled status (no ongoing poll); API base defaults to `:18002` in desktop mode |
| `StudentOfflinePracticeDownload.jsx` | Web student shell: animated **Practice folder** pill on NavBar trailing slot (theme `--primary` / `--success`; opens `GET /desktop-practice-bundle?access_token=` so the browser owns transfer progress; busy shows **Downloading…**, then brief **Started**; not in desktop build) |
| `ChangePasswordModal.jsx` | Change-password modal — used by both student and lecturer dashboards via Header `editProfile` |

## Local Contracts

### StudentHistoryPage

- Fetches `GET /api/submissions/my-history` with `page`, `size` (10), optional `labId`, and `sort` (server-side); plus `GET /api/submissions/my-labs` (all quarters; each lab/submission includes `termLabel`)
- Out-of-term students (`inCurrentTerm === false`, from `StudentDashboard` or `user.inCurrentTerm`) see a warning banner above **Submission History**: amber tokens (`bg-warning-bg`, `text-warning-text`) for attention required — not error red; no border
- Filter by lab via Performance by Lab rows (click to filter, click again to show all); server-side table sort via clickable column headers; prev/next pagination (10 rows per page)
- Two-column body from `xl`: Performance by Lab ~3/10 width, All Submissions ~7/10
- Expanded rows show challenge-level results only
- Scores display via shared `formatNumber` (`Math.floor`) — same round-down as the student dashboard and lecturer views
- Row status from overall score: `failed` (&lt; 50), `partial` (50–80), `passed` (&gt; 80), `unknown` (no score); thresholds use the floored display score

### ChangePasswordModal

- Optional `onPasswordFocusChange(boolean)` — fired when focus enters or leaves the three password inputs (student dashboard wires this to `StudentFoxMascot` `coverEyes`)
- Opened via `Header` `editProfile` command
- Shared across `StudentDashboard` and `LecturerDashboard`
- Client validation via `frontend/src/utils/validation.js` (password length, confirm match, new ≠ current)
- Save success/fail uses the shared **Toast** (`Saved successfully.` / friendly change-password error)

### Upload inputs (from parent page)

`StudentDashboard.jsx` passes to `DropZone`:

- Out-of-term students skip `GET /api/labs` and stay on History. The History route also skips dashboard `GET /api/labs` and `GET /api/submissions/my-labs` — `StudentHistoryPage` loads history APIs itself.
- `GET /api/labs` for a student JWT includes `challenges` plus `totalSubmissions` / `latestSubmission` per lab. `StudentDashboard` caches those by lab id and skips follow-up `/challenges` and `/stats` on login and lab switch unless the fields are missing.
- `labSummariesById` — from `GET /api/submissions/my-labs?scope=current` in `StudentDashboard.jsx` (web current quarter only; skipped in desktop); after upload, `attempts` is patched from the upload response so the “no submission yet” notification clears without a refetch
- `attemptNumber` — next unused attempt from backend `totalSubmissions + 1` after each upload (server still assigns `MAX+1` if the client number is stale)
- `authToken` — from `user.accessToken`

Successful upload shows a fixed **Toast** (`Grading complete. Your score: N/100`) from `StudentDashboard.jsx`, same pattern as Solution Management save toast. When any challenge in the upload `lab_result` has a Java compile failure, a second error **Toast** (`Compilation Error`) stacks with the score toast, and the Declaration Test tab shows a matching **DangerAlert** banner (per-class javac lines stay under each class card).

After upload, `StudentDashboard` uses the upload payload for stats and challenge scores. A persisted student `lab_result` is scores-only (thin); Class/MMD/OT trees load via `GET .../class|mmd|testcases` for scored challenges in one post-upload prefetch (single overlay spinner). An Admin upload (`persisted: false`) is not saved, so those tabs render from the inline `lab_result` trees and that prefetch is skipped. Challenge clicks then read the in-memory cache. A persisted bundle stays “thin” until each applicable pillar has its tree. Switching labs uses the `GET /api/labs` cache. History view loads class/mmd/testcases via read endpoints when no cached bundle exists.

- **MMD parse errors:** `/mmd` and `lab_result.mmd` use `{ classes, parseError }`. `StudentUI` shows a warning banner when `parseError` is set.

### Lab list layout (`StudentUI.jsx`)

- Left rail: `StudentLabSidebar` — shadcn `Sidebar` + `Item` rows (name, and on web `due {date}` plus countdown badge). The badge shows on the deadline calendar day and the two Vietnam days before it, counting the deadline day through 23:59:59: deadline 22/10 is **3 days** on 20/10, **2 days** on 21/10, **1 day** on 22/10. Chip color steps amber (3) → solid amber (2) → red (1) via `Badge` variants `deadline-3` / `deadline-2` / `deadline-1`. On web, the same count is on the main header (`due {date} · N day(s) left`) and in the notification bell (`N days until deadline`, or due today on the deadline date); expired labs show the **Expired** badge only. Desktop hides deadline text, countdown/expired badges, and the notification bell.
- Right: `SidebarInset` — single-level stage cards: selected-lab header + `DropZone` (`upload-cloud.json` centered arrow / `grading-engine.json` Lottie visuals), `StageCard3D` stats row (`AnimatedCounter` + Phosphor Duotone wells), and animated `layoutId="student-result-tab"` pillar panel. Internal MMD/Class breakdowns flatten into inset panels (`bg-surface-secondary` / `border-l-4`) with zero nested bordered cards, while Operation Test testcase items (`Example Testcases` and `Other Testcases`) use subtle hairline borders (`border-border`, `border-success/35`, `border-error/35`) for structural clarity. The full-page spinner waits only for labs; Challenges and stats come from that same `GET /api/labs` payload (a small Challenges spinner remains only if the list was omitted and a follow-up fetch is needed).
- Mobile: `SidebarTrigger` opens the lab list as an overlay; selecting a lab closes it
- Desktop: `SidebarTrigger` clips the lab rail closed (fixed inner width + slide); click again to expand

### Result tabs (`StudentUI.jsx`)

- MMD / Declaration Test / Operation Test tabs and their content are hidden until the student completes an upload in the current browser session for the selected lab (`resultsRevealed`). Before that, the main panel shows a `StageCard3D` + Lottie `EmptyState` prompt to submit first.
- After upload, tabs appear and are filtered by `lab_result.scoreApplicability`: Declaration Test is hidden when `scoreApplicability.class === false` (`has_class=false`); MMD when `has_mmd=false`; Operation Test appears when `scoreApplicability.testcase === true` or the challenge bundle has a non-empty `testcases` array (upload or revisit).
- If the active tab is hidden after switching challenges, selection falls back to the first visible tab.
- Declaration Test class cards show the student's parsed declaring type (e.g. `INTERFACE`); class shells are pass/fail (green check when correct, red X when wrong). A type with no rubric fields/constructors/methods (e.g. an enum) counts as `1/1 · 100%` from the shell, not `0/1`. When the shell fails, member rows are all fail (no green ticks). Fields, methods, and constructors are pass or fail only — leftover `partial` flags render as fail, not an orange minus
- Student Class/MMD rows come from API `DisclosureMode.STUDENT`: missing or wrong members show generic messages, never rubric expected names. `studentDisplayConsolidation.js` collapses repeated generic error rows; score counts still use the full rubric-scoped arrays. Lecturer Grading/Dashboard drawers use the same generic labels and consolidators for wrong/missing Class and MMD members.
- A class compile error is one wrapping line under the class name (`CompileErrorMessage` convention). Do not CSS-truncate it and do not show an expanded diagnostic banner. Independent sibling cards stay a normal grade.

### Testcase tab rows (`StudentUI.jsx`)

- The Operation Test tab lists Example vs Other (hidden) testcase cards with hairline borders (`border-border` on Example rows, `border-success/35` / `border-error/35` on Other pills). Students never see Unit or Composition labels. Hidden row order: pass/fail icon, `Testcase N`, colored PASS/FAIL/ERROR, lock icon — no rubric testcase names or I/O. More than 60 hidden testcases paginate (60 per page, responsive grid up to 4 columns).
- Example testcase **Your Output** for invocation errors is plain language (`formatStudentTestcaseMessage`; backend `InvocationErrorMessage` on new grades), not raw JVM text like `Car.<init>(int,java.lang.String)`.

### Dashboard stats row (`StudentUI.jsx`)

- Stats are lab-scoped; `StudentDashboard` applies `totalSubmissions` / `latestSubmission` from the selected lab in `GET /api/labs` (or `/stats` if those fields are missing). Switching labs uses the already-loaded lab payload; grade is not loaded from this API. Clicking the already-selected lab is a no-op.
- **Total Submissions** and **Latest Submission** always reflect DB history for the selected lab.
- **Current Grade** follows the same session-reveal rule as challenge sidebar scores: `--/--` until the student completes an upload in the current browser session for that lab; then shows the score from the upload response. Switching labs resets the grade until that lab is uploaded again in-session.

## Work Guidance

- Student history uses live APIs in `StudentHistoryPage.jsx`
- Profile modal changes affect both roles — test both dashboards
- `attemptNumber` is derived from backend `totalSubmissions` after each upload; the server also assigns `MAX(attempt)+1` so a stale client number cannot freeze the count

## Verification

- Manual: log in as student, use the Home / History / Ranking bar (same control as the lecturer nav), open the account menu for Change Password and Logout
- Out-of-term active students see History only (Home hidden) plus the amber warning banner above **Submission History**; inactive students cannot log in
- Upload: pick a lab from the left list, drop challenge folder, confirm API response
- Offline practice: web — **Practice folder** on the student NavBar (browser-native download of prebuilt runtime-only zip); desktop — **Import rubric pack** then **Okay** (app closes and relaunches); see `docs/DESKTOP_STUDENT_DIST.md`

## Child DOX Index

No child docs.
