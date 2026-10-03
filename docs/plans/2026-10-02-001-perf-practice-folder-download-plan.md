---
title: Practice Folder Download Performance - Plan
type: perf
date: 2026-10-02
topic: practice-folder-download
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Practice Folder Download Performance - Plan

## Goal Capsule

- **Objective:** Keep student **Download practice folder** prepare time consistently fast (&lt;10s server-side, excluding network transfer) for the first and later downloads — including after Render sleep — by streaming a **prebuilt** runtime-only zip (no embedded pack, no runtime disk cache).
- **Product authority:** This Product Contract. Lecturer pack export and desktop import/restart are surrounding work.
- **Open blockers:** None.
- **Stop conditions:** Do not embed `.agpack`. Do not rely on a warm per-dyno zip cache that rebuilds after sleep. Do not re-zip ~290 MB on each HTTP request in production.
- **Execution:** Code.
- **Product Contract preservation:** Supersedes Approach A (disk cache). Session direction changed: prebuilt zip stream so 1st/Nth prepare times stay similar after sleep.

---

## Product Contract

### Summary

Authorized current-quarter students download a Windows practice runtime zip that the API streams from a file assembled at build/packaging time. No rubric pack inside. Banner copy points at `OOP-AutoGrader-Practice.exe`. Prepare work is auth + open/stream file — not zip-on-request.

### Key Decisions

- **Runtime-only zip; teachers distribute packs** (session-settled: user-directed). **Governs R2.**
- **Keep current-quarter enrollment gate** (session-settled: user-directed). **Governs R4.**
- **Prebuilt zip stream, not runtime cache** (session-settled: user-directed — chosen over disk cache: Render sleep makes first-hit cache rebuild common). **Governs R1, R5, R6.**
- **Banner copy exact** (session-settled: user-directed). **Governs R7.**
- **Prepare budget &lt;10s server-side** (session-settled: user-directed — excludes student network transfer). **Governs R1, R6.**

### Requirements

- R1. Server-side prepare (auth through response headers / start of file stream) stays under ~10s for every authorized download when the prebuilt zip is present, including after process sleep/wake.
- R2. The served zip must not contain `.agpack`.
- R3. The zip is produced by assemble/packaging (`assemble-student-desktop.ps1`) at the fixed path `backend/target/OOP-AutoGrader-Practice.zip` (or `OOP-AutoGrader-Practice.zip` next to the deploy jar).
- R4. Only active students enrolled in the current quarter may download.
- R5. The API does not zip inputs on the request path and does not maintain a rebuildable runtime zip cache.
- R6. First and later downloads on a correctly configured host have similar prepare cost (stream the same prebuilt file).
- R7. Banner body: `Download a Windows folder with the local grader, and OOP-AutoGrader-Practice.exe to start. Practice scores stay on your machine and are not submitted here.`
- R8. Docs describe prebuilt zip + lecturer packs, not embedded term packs or request-time cache.

### Scope Boundaries

- **In:** Fixed prebuilt zip path, stream endpoint, assemble always writing that zip, runtime-only contents, banner, docs.
- **Out:** Per-request zip; fingerprint disk cache; baking Windows launcher publish into Linux Docker without a supplied artifact; signed-URL native download UX.

### Implementation Units

#### U1. Stream prebuilt zip API

- Files: `StudentDesktopPracticeBundleService.java`, `StudentDesktopPracticeDownloadController.java`, `application.yml`, `.env.backend.example`, unit test.
- Approach: After current-quarter gate, resolve fixed paths (`target/…` then cwd `OOP-AutoGrader-Practice.zip`); return `FileSystemResource` stream. 503 if missing.

#### U2. Banner + Downloading label

- Files: `StudentOfflinePracticeDownload.jsx` (+ AGENTS notes).

#### U3. Assemble fixed zip + docs

- Files: `scripts/assemble-student-desktop.ps1`, `docs/DESKTOP_STUDENT_DIST.md`, `CONCEPTS.md`, backend/frontend AGENTS.

---

## Planning Contract

### Key Technical Decisions

- KTD1. No `DesktopPackExportService` on this path.
- KTD2. Fixed paths only: `target/OOP-AutoGrader-Practice.zip` (local) then `OOP-AutoGrader-Practice.zip` (deploy cwd). No env var.
- KTD3. Assemble always writes `backend/target/OOP-AutoGrader-Practice.zip`; deploy copies that file next to `app.jar` as needed.
- KTD4. Filename attachment may still use `OOP-AutoGrader-Practice-Q{termNumber}.zip` for recognition.

### Verification Contract

- `mvn test` — `StudentDesktopPracticeBundleServiceTest`, auth tests for the route.
- Manual: assemble once, restart API, two downloads both start streaming quickly (&lt;10s prepare).

### Definition of Done

- [x] No request-time zip / no runtime cache
- [x] Prebuilt zip streamed after current-quarter gate
- [x] Runtime-only product + banner copy
- [x] Assemble fixed zip path + docs
