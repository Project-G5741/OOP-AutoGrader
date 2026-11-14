# Lecturer offline Bulk Grading distribution

Windows x64 offline Bulk Grading for lecturers: one folder, WebView2 host EXE (or `.bat`), local Spring Boot (`desktop-lecturer` profile) on port **18003**. Separate from [student practice](DESKTOP_STUDENT_DIST.md) (port **18002**).

## Folder layout (after assembly)

```
LecturerBulk/
  OOP-AutoGrader-LecturerBulk.exe
  desktop-host.json             # profile desktop-lecturer, port 18003
  backend.jar
  worker.jar
  runtime/jdk/
  ui/dist-desktop-lecturer/
  rubric/                       # same .agpack names as student practice
  data/                         # H2 DB, submissions, bulk-last-batch.json, offline-roster-irns.json
```

## Prerequisites before grading

1. **Rubric pack** — same signed practice packs (`Rubric_{year}_Q{n}.agpack` / `Rubric_{labName}.agpack`). Import from the app header (restart after Okay).
2. **Current-term roster** — on the website Term / Quarters page, **Download roster** (CSV, next to Practice pack); in the offline app header, **Import roster**. Import **replaces** any previous local roster (not a merge). Empty successful import clears membership and blocks Bulk. Pack import and roster import are independent: the last roster IRN set is kept under `data/offline-roster-irns.json` and re-applied after pack wipe/import.
3. Without a usable roster, drop / Start Grading stay blocked.

## Run

1. Unzip the lecturer folder (IT assemble zip or `backend/target/OOP-AutoGrader-LecturerBulk.zip`).
2. Start **`OOP-AutoGrader-LecturerBulk.exe`** (or `.bat`).
3. Import pack + roster → Lab/Exam Bulk Grading → results with plagiarism + drawer.
4. Last finished batch is retained across restart until a new completed run or **Clear batch**.
5. Export CSV/Excel from the results table (no PDF offline). Results never sync to Score.

## Developer smoke (no launcher)

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "desktop-lecturer"
$env:APP_DESKTOP_HOME = "D:\path\to\LecturerBulk"
$env:JWT_SECRET = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
mvn spring-boot:run
```

Build UI: `cd frontend && npm run build:desktop-lecturer` → `frontend/dist-desktop-lecturer/`.

## Assemble

```powershell
.\scripts\assemble-lecturer-desktop.ps1 -OutputDir "backend\target\LecturerBulk"
```

Uses the same desktop classifier JAR and Rust host binary as student practice; lecturer installs ship `desktop-host.json` so the host starts `desktop-lecturer` on **18003**. Student installs omit that file and keep defaults (**18002** / `desktop`).

## Grading parity

Grading logic matches student dashboard uploads, web Bulk Grading, and student offline practice. Lecturer offline only changes distribution, membership source (imported roster), and local retention/export — not the grader engine.
