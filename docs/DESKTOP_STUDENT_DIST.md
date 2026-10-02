# Student desktop practice distribution

Windows x64 offline practice grader: one folder, embedded WebView2 window, local Spring Boot (`desktop` profile) on port **18002**.

## Folder layout (after assembly)

```
StudentPractice/
  OOP-AutoGrader-Practice.exe   # WebView2 launcher
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

**UI import** materializes the pack into the local DB and writes `data/desktop-pack-fingerprint.txt` before asking you to restart (restart still required so the window reloads a settled labs view). **Manual** `rubric/` copy still needs fingerprint clear + restart so bootstrap can import. If you delete **all** pack files from `rubric/` and restart, local labs and practice DB rows are wiped (sidebar empty; not-ready banner).

If multiple quarter packs are present (manual copies), newest-by-mtime wins. UI term import still removes sibling quarter packs.

In the practice EXE, **Logout** closes the window and stops the local backend (same as the window **X**). Cloud/web Logout is unchanged.

## Lecturer: publish packs

1. Set `DESKTOP_PACK_SIGNING_PRIVATE_KEY` on the server (pairs with `backend/src/main/resources/desktop-pack-public.key`).
2. **Quarter:** Term Management → select quarter → **Download practice pack** → `Rubric_{yearLabel}_Q{n}.agpack`.
3. **Single lab:** Solution Management → select lab → **Practice pack** → `Rubric_{labName}.agpack`.
4. Student web **Download practice folder** streams a **prebuilt** runtime-only zip (no `.agpack`); lecturers distribute packs separately.

## Student: run practice

1. Unzip the practice folder (website download or lecturer bundle).
2. Start **`OOP-AutoGrader-Practice.exe`** (or `.bat` if the exe is missing). The EXE starts the local backend with no console window; the `.bat` fallback keeps a visible Java console by design.
3. Use the local dashboard like the web submit view. Scores stay on your PC only.

## Student: import or update rubrics

**In the app (recommended):** header → **Import rubric pack** → choose `Rubric_….agpack` → confirm replace if prompted → **restart** the app.

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
.\scripts\assemble-student-desktop.ps1 -OutputDir "D:\dist\StudentPractice"
```

Copies desktop-trimmed `backend.jar` (`backend-1.0.0-desktop.jar` from `mvn -Pdesktop-dist package`), `worker.jar`, `frontend/dist-desktop`, a **jlink** `runtime/jdk` (when `JAVA_HOME` / `jlink` is available), and a **self-contained** WebView2 launcher when `dotnet` is available. Prints an on-disk size breakdown. Always writes the web download zip to the **fixed** path `backend/target/OOP-AutoGrader-Practice.zip` (API also accepts `OOP-AutoGrader-Practice.zip` next to `app.jar` in Docker). Packs are not embedded — lecturers distribute `.agpack` separately.

Offline prerequisites: bundled `runtime/jdk` + self-contained EXE (no system Java/.NET). WebView2 Evergreen remains an OS dependency.

## API notes (desktop profile)

- No login; fixed local student `practice@desktop.local`.
- `GET /api/desktop/status` — `ready`, `packMissing`, `error`, `bootstrapComplete` (launcher and UI wait for `bootstrapComplete` before showing labs / settled banner).
- `POST /api/desktop/packs/import` — multipart `file`, optional `confirmReplace=true` for lab name conflicts; stages under `rubric/`, **materializes** into H2, writes fingerprint; restart still required for UI.
- Grading uses local `worker.jar` with `DESKTOP_WORKER_JAVA` pointing at bundled `runtime/jdk/bin/java.exe` when present. Operational testcases are **not** container-sandboxed; they use the same isolated worker JVM as dev.
- H2 under `data/`; `LabStructureService` bulk deletes use portable SQL (no PostgreSQL-only CTE deletes).
