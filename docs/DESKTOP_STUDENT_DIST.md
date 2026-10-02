# Student desktop practice distribution

Windows x64 offline practice grader: one folder, embedded WebView2 window, local Spring Boot (`desktop` profile) on port **18002**.

## Folder layout (after assembly)

```
StudentPractice/
  OOP-AutoGrader-Practice.exe   # WebView2 launcher
  backend.jar
  worker.jar
  runtime/jdk/                  # optional portable JRE 17
  ui/dist-desktop/              # `npm run build:desktop` output (copy into install)
  rubric/
    {term-uuid}.term.agpack     # quarter pack (optional if using lab packs only)
    {lab-uuid}.lab.agpack       # optional single-lab packs
  data/                         # H2 DB, submissions, pack fingerprint (created at run)
```

## Practice pack file names (required)

Only these names are accepted (UUID must match the id inside the signed pack):

| Kind | Filename pattern | On restart |
|---|---|---|
| Quarter (term) | `{term-uuid}.term.agpack` | **Replaces all** local labs for that quarter |
| Single lab | `{lab-uuid}.lab.agpack` | **Adds** a lab; same lab **name** as an existing row prompts replace before import |

Legacy names (`term.agpack`, `term-{uuid}.agpack`) are **not** loaded.

Every import (UI or manual copy) takes effect only after a **full app restart**. The UI clears `data/desktop-pack-fingerprint.txt` when you import via **Import rubric pack** so the next startup re-imports.

## Lecturer: publish packs

1. Set `DESKTOP_PACK_SIGNING_PRIVATE_KEY` on the server (pairs with `backend/src/main/resources/desktop-pack-public.key`).
2. **Quarter:** Term Management → select quarter → **Download practice pack** → `{termId}.term.agpack`.
3. **Single lab:** Solution Management → select lab → **Practice pack** → `{labId}.lab.agpack`.
4. Student web **Download practice folder** streams a **prebuilt** runtime-only zip (no `.agpack`); lecturers distribute packs separately.

## Student: run practice

1. Unzip the practice folder (website download or lecturer bundle).
2. Start **`OOP-AutoGrader-Practice.exe`** (or `.bat` if the exe is missing).
3. Use the local dashboard like the web submit view. Scores stay on your PC only.

## Student: import or update rubrics

**In the app (recommended):** header → **Import rubric pack** → choose `.term.agpack` or `.lab.agpack` → confirm replace if prompted → **restart** the app.

**Manual:** copy packs into `rubric/` with exact names above, delete extra `*.term.agpack` files if you only want one quarter, delete `data/desktop-pack-fingerprint.txt`, restart.

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

Copies shaded `backend.jar`, `worker.jar`, and `frontend/dist-desktop`. Creates empty `rubric/` and `data/`. Publishes the WebView2 launcher when `dotnet` is available. Always writes the web download zip to the **fixed** path `backend/target/OOP-AutoGrader-Practice.zip` (API also accepts `OOP-AutoGrader-Practice.zip` next to `app.jar` in Docker). Packs are not embedded — lecturers distribute `.agpack` separately.

## API notes (desktop profile)

- No login; fixed local student `practice@desktop.local`.
- `GET /api/desktop/status` — `ready`, `packMissing`, `error` (drives offline banner).
- `POST /api/desktop/packs/import` — multipart `file`, optional `confirmReplace=true` for lab name conflicts; saves under `rubric/` and clears import fingerprint.
- Grading uses local `worker.jar` (Java on PATH if no bundled JRE). Operational testcases are **not** container-sandboxed; they use the same isolated worker JVM as dev.
- H2 under `data/`; `LabStructureService` bulk deletes use portable SQL (no PostgreSQL-only CTE deletes).
