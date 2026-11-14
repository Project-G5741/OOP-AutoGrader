# Student desktop practice distribution

Windows x64 offline practice grader: one folder, embedded WebView2 window, local Spring Boot (`desktop` profile) on port **18002**.

## Folder layout (after assembly)

```
StudentPractice/
  OOP-AutoGrader-Practice.exe   # thin Rust + WebView2 host
  backend.jar
  worker.jar
  runtime/jdk/                  # portable jlink JDK (compiler included; assemble builds this)
  ui/dist-desktop/              # `npm run build:desktop` output (copy into install)
  rubric/
    Rubric_2026-2027_Q1.agpack  # quarter pack (optional if using lab packs only)
    Rubric_Lab 2.agpack         # optional single-lab packs
  data/                         # H2 DB, submissions, pack fingerprint (created at run)
```

## Practice pack file names (required)

Only these names are accepted (filename must match the signed pack contents):

| Kind | Filename pattern | Example | On restart / UI import |
|---|---|---|---|
| Quarter (term) | `Rubric_{yearLabel}_Q{n}.agpack` | `Rubric_2026-2027_Q1.agpack` | **Replaces all** local labs. **UI import** also deletes leftover lab pack files in `rubric/` so the quarter pack alone defines the lab set |
| Single lab | `Rubric_{labName}.agpack` | `Rubric_Lab 2.agpack`, `Rubric_Midterm.agpack` | **Adds** a lab; same lab **name** (different id) in that quarter → UI asks to **replace**; same id updates silently. Manual folder copy applies without that prompt |

Lab names are at most **50 characters** and cannot contain Windows-illegal characters (`\ / : * ? " < > |`). Spaces are allowed.

Legacy UUID names (`{uuid}.term.agpack`, `{uuid}.lab.agpack`) and older aliases (`term.agpack`, `term-{uuid}.agpack`) are **not** loaded. Invalid packs show a specific rejection reason (not a generic busy message).

**UI import** materializes the pack into the local DB and writes `data/desktop-pack-fingerprint.txt`, then shows a blocking **Okay** dialog. Clicking **Okay** posts `practice-restart`: the host stops the local backend, relaunches the EXE, and exits so the new window loads a settled labs view — do not submit until that reopen finishes. **Exit** / window **X** still quit without relaunch (`practice-quit`). **Manual** `rubric/` copy still needs fingerprint clear + restart so bootstrap can import. If you delete **all** pack files from `rubric/` and restart, local labs and practice DB rows are wiped (sidebar empty; not-ready banner).

If multiple quarter packs are present (manual copies), newest-by-mtime wins. UI term import still removes sibling quarter packs.

In the practice EXE, the header shows an **Exit** button (no profile/account menu) that closes the window and stops the local backend (same as the window **X**). Ranking, shop, streak, deadline text/badges, and the notification bell are omitted. **Import mascot** is the only step that calls the website API (`cloud-api-url.txt`). Opening the EXE, grading, and packs stay fully offline. Paste the web practice code; Java pulls equipped/owned ids into `data/mascot-snapshot.json` (header mascot follows that file; fox until imported). Scores stay local. Cloud/web Logout is unchanged. The header **ThemeToggle** is the same multi-preset picker as web (Classic Slate / Sand / Evergreen / Midnight / Graphite / Scarlet; `localStorage` key `oop-theme`) — rebuild `npm run build:desktop` before assemble when theme UI changes.

## Lecturer: publish packs

1. Set `DESKTOP_PACK_SIGNING_PRIVATE_KEY` on the server (pairs with `backend/src/main/resources/desktop-pack-public.key`).
2. **Quarter:** Term Management → select quarter → **Download practice pack** → `Rubric_{yearLabel}_Q{n}.agpack`.
3. **Single lab:** Solution Management → select lab → **Practice pack** → `Rubric_{labName}.agpack`.
4. Student web **Download practice folder** streams a **prebuilt** runtime-only zip (no `.agpack`); lecturers distribute packs separately.

## Student: run practice

1. Unzip the practice folder (website download or lecturer bundle).
2. Start **`OOP-AutoGrader-Practice.exe`** (or `.bat` if the exe is missing). The EXE opens a practice window immediately (no console), starts the **local** backend on port **18002**, then loads the UI when ready; the `.bat` fallback keeps a visible Java console by design. The website / `npm start` is **not** required to open or grade.
3. Use the local dashboard like the web submit view. Scores stay on your PC only.
4. Optional — **Import mascot**: copy **Copy practice code** from the website (same API host as `cloud-api-url.txt`), then paste in the EXE. That one request needs internet. After a successful import, disconnect; the mascot stays in `data/mascot-snapshot.json`.

## Student: import or update rubrics

**In the app (recommended):** header → **Import rubric pack** → choose `Rubric_….agpack` → confirm replace if prompted → click **Okay** (app closes and reopens automatically).

**Manual:** copy packs into `rubric/` with exact names above, delete extra quarter `Rubric_*_Q*.agpack` files if you only want one quarter, delete `data/desktop-pack-fingerprint.txt`, restart.

If a lab pack fails validation at startup, it is skipped (logged); a successful quarter import still loads. Check `GET http://127.0.0.1:18002/api/desktop/status` — `"ready": true` when rubrics are usable.

## Developer smoke (no launcher)

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "desktop"
$env:APP_DESKTOP_HOME = "D:\path\to\StudentPractice"
$env:JWT_SECRET = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
mvn spring-boot:run
```

Place `ui/dist-desktop` and at least one valid pack under `{APP_DESKTOP_HOME}/rubric/`. Open `http://127.0.0.1:18002/`.

Build UI: `cd frontend && npm run build:desktop` (output `frontend/dist-desktop/` — copy into install, not served from repo in production).

## Assemble script

From repo root:

```powershell
$env:APP_DESKTOP_CLOUD_API_URL = "https://your-production-api-host"
.\scripts\assemble-student-desktop.ps1 -OutputDir "backend\target\StudentPractice"
```

Copies desktop-trimmed `backend.jar` (`backend-1.0.0-desktop.jar` from `mvn -Pdesktop-dist package`), `worker.jar`, `frontend/dist-desktop` (replaces any existing `ui/dist-desktop` so hashed assets cannot go stale), a **jlink** `runtime/jdk` (when `JAVA_HOME` / `jlink` is available; always includes `jdk.crypto.ec` so pack Ed25519 verify works), and the **Rust** WebView2 host (`desktop/launcher/practice-host/` via `cargo build --release`). Writes `cloud-api-url.txt` from `APP_DESKTOP_CLOUD_API_URL` (fallback `VITE_API_URL`) so the host/`.bat` can set `app.desktop.cloud-api-url` for mascot pull. Assemble **requires** `cargo` + MSVC Build Tools; it fails if the host EXE cannot be built. Before zipping, it strips local-run leftovers (`*.WebView2` user-data dirs and anything under `data/`) and drops jlink staging under `backend/target`. Re-run `npm run build:desktop` before assemble when UI source changed — assemble only auto-builds UI if `frontend/dist-desktop` is missing. Prints an on-disk size breakdown (host EXE is ~1–2 MB; recent assemble ~120 MB extracted vs ~281 MB with the old self-contained .NET host). Always writes the web download zip to `backend/target/OOP-AutoGrader-Practice.zip` (not committed; Render packaging in `backend/DEPLOY_RENDER.md` step 8) — student **Download practice folder** streams that file. Packs are not embedded — lecturers distribute `.agpack` separately.

Offline prerequisites: bundled `runtime/jdk` + thin host EXE (no system Java, no .NET runtime). WebView2 Evergreen remains an OS dependency. Maintainer tools: Rust stable + MSVC Build Tools for assemble; students need neither.

## API notes (desktop profile)

- No login; fixed local student `practice@desktop.local`.
- No streak, daily missions, points, or mascot shop: `/api/students/streak` and `/api/students/mascots` beans are off; uploads do not record missions. Local H2 drops leftover `student_daily_streak` / `student_mascot_purchase` on startup. Startup never calls the website. `GET /api/desktop/mascot/snapshot` reads `data/mascot-snapshot.json` only (fox when missing). `POST /api/desktop/mascot/sync` is the only outbound call (`APP_DESKTOP_CLOUD_API_URL` / `cloud-api-url.txt`).
- `GET /api/desktop/status` — `ready`, `packMissing`, `error`, `bootstrapComplete` (launcher and UI wait for `bootstrapComplete` before showing labs / settled banner).
- `POST /api/desktop/packs/import` — multipart `file` (up to **50MB**), optional `confirmReplace=true` for lab name conflicts; stages under `rubric/`, **materializes** into H2, writes fingerprint; UI then shows **Okay**, which posts `practice-restart` so the host relaunches for a settled labs view. Oversized files return **413** with a clear message (not a generic 500). Pack SCOPE/DECLARING_TYPE ids (1000+) are flushed in the same TX before insert-only lab save so they resolve (otherwise 400 Invalid scope id). Type-only method/constructor parameters (blank names from legacy `parameterTypes`) import as `""` aliases.
- Grading uses local `worker.jar` with `DESKTOP_WORKER_JAVA` pointing at bundled `runtime/jdk/bin/java.exe` when present. Operational testcases are **not** container-sandboxed: `application-desktop.yml` forces `app.grading.sandbox.enabled=false`, and the practice EXE / `.bat` clear `SANDBOX_ENABLED` / `APP_GRADING_SANDBOX_ENABLED` / runner URL+token so a cloud shell env cannot turn sandbox on. Local worker IPC failures surface as `Operational test worker unavailable` (not the cloud sandbox message). Desktop profile sizes the worker for a student PC (`app.grading.worker-xmx=512m`, `worker-max-metaspace=256m`, `ot-batch-parallelism=2` so Midterm-size OT batches do not OOM the worker under many CPUs) and the launcher/API JVM uses `-Xmx512m` — unlike Render free-tier (`-Xmx64m` worker, OT pool = available processors). Local IPC reads lab-wide OT responses in chunks (not byte-at-a-time).
- H2 under `data/`; `LabStructureService` bulk deletes use portable SQL (no PostgreSQL-only CTE deletes).
- Declaration multi-alias: pack JSON carries ordered `names` / `dataTypes` (and method/parameter equivalents); materialize persists alias child rows. `DesktopDeclarationAliasListsSchemaMigrator` creates/backfills those tables on startup. Grading and OT use the same Cartesian alias match as cloud (rebuild `backend.jar` + `worker.jar` via assemble after grading/OT changes).
