---
title: Practice Empty Rubric, Status Once, Logout Quit - Plan
type: feat
date: 2026-10-02
topic: practice-empty-rubric-status-logout
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Practice Empty Rubric, Status Once, Logout Quit - Plan

## Goal Capsule

- **Objective:** When all practice packs are removed from `rubric/` and the app restarts, clear labs from the local DB and sidebar; check desktop status once at startup (no polling); make Logout quit the practice window like **X** (stop backend too), desktop only.
- **Product authority:** This Product Contract.
- **Open blockers:** None.
- **Stop conditions:** Do not wipe labs while the app is running without restart. Do not change cloud/web Logout behavior. Do not reintroduce continuous status polling (interval or focus refresh).
- **Execution:** Code.

---

## Product Contract

### Summary

Three desktop-only practice UX fixes: empty `rubric/` on restart wipes local labs/DB; status banner is a one-shot check at UI start; Logout closes the host like the window **X** (including stopping the Java backend).

### Problem Frame

1. Deleting every `.agpack` under `rubric/` and restarting leaves labs in H2 and the sidebar; bootstrap returns pack-missing without wiping.
2. `DesktopPracticeStatusBanner` polls every 3s (and on focus), so readiness lags after start.
3. Desktop `App.desktop.jsx` wires `noopLogout`; students need header Logout to quit like **X**.

### Key Decisions

- **Empty `rubric/` wipes on restart only** (session-settled: user-directed — chosen over live-while-running wipe: matches “restart applies pack changes”). **Governs R1, R2.**
- **Status fetch once at app start** (session-settled: user-directed — chosen over fast/continuous polling). **Governs R3.**
- **Logout = same as X** — close window and stop practice backend (session-settled: user-directed — chosen over close-window-only). **Governs R4, R5.**

### Requirements

- R1. On desktop bootstrap, if `rubric/` has no valid term or lab packs, wipe local practice data (labs + cascade / terms / enrollments — same scope as term-pack `wipePracticeData`) so `/api/labs/list` is empty and the sidebar shows no assignments; set pack-missing / not-ready as today.
- R2. Clearing packs mid-session without restart does not require live wipe (out of scope).
- R3. Desktop status banner loads status **once** when the practice UI mounts — no `setInterval`, no focus re-fetch.
- R4. Desktop **Logout** closes the practice host window and stops the local backend, same outcome as clicking **X**. Web/cloud Logout unchanged.
- R5. Button label stays **Logout**; quit behavior is desktop-only.

### Non-Goals

- Live folder watching or wiping without restart.
- Renaming Logout to Exit/Close.
- Changing pack import/restart apply rules beyond empty-folder wipe.
- Making Logout quit when the UI is opened in a normal browser without WebView2 (no host bridge).

### Success Criteria

- AE1: With packs loaded, delete all files in `rubric/`, restart → banner not ready, sidebar “No labs available”, DB has no practice labs.
- AE2: Cold start with packs present (EXE path: backend already healthy before WebView) → status correct from the single mount fetch without waiting ~3s.
- AE3: Desktop Logout closes the window and frees port 18002; web app Logout still signs out only.

### Actors / Flows

- A1. Student (desktop practice EXE).
- F1. Empty rubric restart — delete all packs → restart → wipe + missing banner + empty labs (R1, AE1).
- F2. Status once — open app → one status GET → banner shown or hidden (R3, AE2).
- F3. Logout quit — click Logout → host closes → backend stopped (R4, AE3).

---

## Scope Boundaries

### In scope

- `DesktopPackBootstrapService` empty-pack path + public wipe API on `DesktopPackImportService`.
- `DesktopPracticeStatusBanner` one-shot fetch.
- `App.desktop.jsx` Logout → WebView2 host message; `PracticeHostForm` handles close (existing `FormClosing` → `BackendLauncher.Stop()`).
- Docs: `docs/DESKTOP_STUDENT_DIST.md`, student AGENTS note on status polling, CONCEPTS if wipe-on-empty is missing.
- Backend integration test for empty-rubric wipe; manual smoke for Logout/status.

### Out of scope

- Live wipe without restart; cloud Logout; browser-only desktop mode quit.

---

## Key Technical Decisions

- KTD1. **Reuse term-pack wipe for empty rubric** — Expose a package-facing `wipePracticeData()` (or `clearAllPracticeData()`) on `DesktopPackImportService` and call it from bootstrap when term+lab pack lists are empty **before** returning `BootstrapResult.missing`. Also clear the import fingerprint so the next pack apply is not skipped. Re-run `ensureLocalStudent` after wipe if needed so the fixed practice user remains. **Implements R1, AE1.**
- KTD2. **One-shot status only** — Remove `setInterval(3000)` and the `focus` listener from `DesktopPracticeStatusBanner`; keep a single `refresh()` on mount. EXE already waits for `/api/desktop/status` before navigating (`BackendLauncher.WaitUntilHealthyAsync`), so one fetch is enough for the product path. **Implements R3, AE2.**
- KTD3. **WebView2 postMessage close bridge** — Desktop Logout posts a small JSON message (e.g. `{ type: "practice-quit" }`) via `window.chrome.webview.postMessage` when available; `PracticeHostForm` subscribes to `WebMessageReceived` and calls `Close()` (same path as user X → `BackendLauncher.Stop()`). Guard missing `chrome.webview` (no-op). Do not change `App.jsx` / web Logout. **Implements R4, R5, AE3.**

### Approach

1. Empty-rubric wipe at bootstrap (KTD1).
2. One-shot status banner (KTD2).
3. Logout → host quit bridge (KTD3).
4. Tests + docs.

### Sequencing

U1 (wipe) → U2 (banner) parallel with U3 (logout) after U1 API surface is clear → U4 docs last. U5 tests can track U1 tightly.

### Risks

- Empty wipe deletes local attempt history (same as term import wipe) — intended under R1.
- `.bat` + external browser has no WebView bridge; Logout stays no-op there (documented non-goal).
- Removing focus refresh means banner will not update mid-session if status changes without restart — matches R2/R3.

### Research breadcrumbs

- Empty path today: `DesktopPackBootstrapService.bootstrap()` returns missing without wipe (`backend/.../DesktopPackBootstrapService.java`).
- Wipe impl: private `DesktopPackImportService.wipePracticeData()`.
- Banner poll: `frontend/src/components/student/DesktopPracticeStatusBanner.jsx`.
- Desktop logout noop: `frontend/src/App.desktop.jsx`.
- Host close + stop: `desktop/launcher/OOPAutoGraderPractice/PracticeHostForm.cs`, `BackendLauncher.Stop()`.
- Existing desktop tests: `backend/src/test/java/integration/.../DesktopPackImportIntegrationTest.java`, `DesktopPackBootstrapIntegrationTest.java` if present.

---

## Implementation Units

### U1. Empty rubric wipe on bootstrap

- **Goal:** No valid packs in `rubric/` at startup → wipe local practice DB; labs list empty; pack-missing status.
- **Requirements:** R1, R2; AE1; KTD1
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackImportService.java`, `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackBootstrapService.java`; tests in U5
- **Approach:**
  1. Make wipe callable from bootstrap (public/`package` method wrapping existing wipe + cache invalidate if needed).
  2. In `bootstrap()`, when term+lab pack lists are empty: wipe, clear fingerprint, ensure local student, return existing missing message.
  3. Same when `rubric/` directory missing if labs could still exist from a prior home — wipe if DB has labs, then missing (keep message sensible).
- **Patterns to follow:** `importTermPack` → `wipePracticeData()`; `clearImportFingerprint()`.
- **Test scenarios:** See U5.
- **Verification:** After bootstrap with empty rubric dir and prior labs seeded, `labRepository` empty and status pack-missing.
- **Dependencies:** None

### U2. One-shot desktop status banner

- **Goal:** Status checked once at UI start; no 3s poll / focus refresh.
- **Requirements:** R3; AE2; KTD2
- **Files:** `frontend/src/components/student/DesktopPracticeStatusBanner.jsx`, `frontend/src/components/student/AGENTS.md`
- **Approach:** Mount-only `refresh()`; delete interval and focus listener; keep loading→ready hide behavior.
- **Patterns to follow:** Existing fetch/`API_BASE` fallback.
- **Test scenarios:** Manual AE2; `npm run build:desktop`.
- **Verification:** No `setInterval` / focus refresh in banner; first successful status drives banner.
- **Dependencies:** None

### U3. Desktop Logout quits host

- **Goal:** Logout in practice EXE closes window and stops backend like **X**.
- **Requirements:** R4, R5; AE3; KTD3
- **Files:** `frontend/src/App.desktop.jsx`, `desktop/launcher/OOPAutoGraderPractice/PracticeHostForm.cs`; optional tiny helper under `frontend/src/utils/` if preferred over inline
- **Approach:**
  1. Replace `noopLogout` with a function that posts `{ type: "practice-quit" }` (or agreed string) when `window.chrome?.webview` exists.
  2. After `EnsureCoreWebView2Async`, enable web messaging if needed and handle `WebMessageReceived` → `BeginInvoke(Close)` / `Close()`.
  3. Rely on existing `FormClosing` → `BackendLauncher.Stop()` for port cleanup.
- **Patterns to follow:** Existing `FormClosing` stop path; WebView2 `postMessage` docs.
- **Test scenarios:** Manual AE3 on EXE; confirm web `App.jsx` Logout unchanged.
- **Verification:** Logout exits EXE; nothing listening on 18002 shortly after.
- **Dependencies:** None (parallel with U2)

### U4. Docs

- **Goal:** Document empty-rubric wipe, one-shot status, Logout=quit on desktop.
- **Requirements:** R1–R5
- **Files:** `docs/DESKTOP_STUDENT_DIST.md`, `CONCEPTS.md` (practice/rubric pack note if needed), `frontend/src/components/student/AGENTS.md`
- **Approach:** Short bullets under student desktop ops; remove “polls every 3s” from AGENTS.
- **Test scenarios:** Docs-only.
- **Verification:** DOX pass; no claim of continuous status polling.
- **Dependencies:** U1–U3 behavior settled

### U5. Bootstrap empty-wipe integration test

- **Goal:** Automate AE1 DB half under `desktop` profile.
- **Requirements:** R1; AE1; KTD1
- **Files:** Prefer extend or add under `backend/src/test/java/integration/com/eiu/capstone/backend/desktop/` using temp `APP_DESKTOP_HOME` / rubric dir (same pattern as `DesktopPackImportIntegrationTest`)
- **Approach:** Seed a lab via import/bootstrap with a pack; delete all packs from rubric; clear fingerprint if needed; call `bootstrap()` (or restart-equivalent); assert no labs and missing/not-ready result.
- **Test scenarios:**
  - Covers AE1. Empty rubric after prior labs → wipe → `labRepository.count() == 0` and bootstrap `packMissing` / not ready
  - Non-empty rubric still loads labs (smoke regression)
- **Verification:** Targeted `mvn test` for the desktop bootstrap/import integration class from `backend/`
- **Dependencies:** U1

---

## Verification Contract

- Backend: from `backend/`, targeted `mvn test` for the desktop empty-wipe integration test(s).
- Frontend: `npm run build:desktop` from `frontend/`.
- Launcher: rebuild/publish practice EXE (or assemble script) before manual Logout smoke.
- Manual: AE1 (empty rubric restart), AE2 (banner immediate after open), AE3 (Logout frees 18002).

## Definition of Done

- U1–U5 complete; R1–R5 and AE1–AE3 satisfied.
- Empty `rubric/` + restart → no labs in sidebar/DB; status once at start; desktop Logout quits like **X**.
- Verification Contract commands pass for touched suites / desktop build.
