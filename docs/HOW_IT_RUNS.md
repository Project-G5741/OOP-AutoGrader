# OOP AutoGrader — System Runtime Report

This document explains how the OOP AutoGrader application runs end to end: how you start it, how the frontend, backend, and database interact, and what happens during the main user flows (login, upload, grading, and lecturer analytics).

---

## 1. What This System Does

The OOP AutoGrader is a full-stack web application for **EIU Capstone** OOP courses. Students upload folders containing:

- **Java source files** (`.java`) — compiled and graded via reflection against a rubric stored in PostgreSQL
- **Mermaid class diagrams** (`.mmd`) — parsed and compared against the same rubric

Lecturers manage users, view lab statistics, grade overviews, and analytics dashboards. Authentication uses **Google OAuth** (restricted to `@eiu.edu.vn`) or **IRN + password**, with **JWT** sessions stored in the browser.

---

## 2. How to Start the Application

### 2.1 Root orchestration

From the repository root:

```bash
npm start
```

This runs **two processes concurrently** via `concurrently`:

| Process | Command | Port |
|---------|---------|------|
| Backend | `cd backend && mvn spring-boot:run` | **8002** |
| Frontend | `cd frontend && npm run dev` | **5173** |

You can also run them separately:

- `npm run backend` — Spring Boot API only
- `npm run frontend` — Vite dev server only

### 2.2 Prerequisites

| Tier | Requirement |
|------|-------------|
| **Frontend** | Node.js, `frontend/.env` (copy from `.env.example`) |
| **Backend** | **JDK 17+** (not JRE — needed for `javax.tools.JavaCompiler`), Maven, `backend/.env` |
| **Database** | PostgreSQL (e.g. Neon cloud); connection via `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` |

### 2.3 Environment variables

**Frontend** (`frontend/.env`):

- `VITE_GOOGLE_CLIENT_ID` — Google OAuth client ID
- `VITE_API_URL` — backend URL (default `http://localhost:8002`)

**Backend** (`backend/.env`, loaded by `application.yml`):

- Database: `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Auth: `GOOGLE_CLIENT_ID`, `JWT_SECRET` (defined but signing key is generated in-memory on restart)
- CORS: `FRONTEND_URL` (default includes `http://localhost:5173`)
- Storage: `SUBMISSION_BASE_DIR` (default `submissions/`)
- Mail: SMTP settings for password reset emails

---

## 3. High-Level Architecture

```mermaid
flowchart TB
    subgraph Browser["Browser (React SPA :5173)"]
        App["App.jsx — routes, auth state"]
        Pages["Pages: Login, StudentDashboard, LecturerDashboard"]
        DropZone["DropZone — folder upload"]
    end

    subgraph API["Spring Boot API :8002"]
        Controllers["REST Controllers"]
        Services["Services — auth, storage, history"]
        Grading["Grading Engine — reflection + MMD"]
        JPA["Spring Data JPA"]
    end

    subgraph DB["PostgreSQL"]
        Users["user_account, user_role"]
        Rubric["lab, challenge, class, field, method, constructor"]
        Results["lab_submission, submission_*_result"]
        Progress["student_lab_progress, term_enrollment"]
    end

    subgraph Temp["Ephemeral disk"]
        SubDir["submissions/&lt;irn&gt;/&lt;requestId&gt;/"]
    end

    App --> Pages
    Pages -->|"fetch + Bearer JWT"| Controllers
    DropZone -->|"POST multipart"| Controllers
    Controllers --> Services
    Services --> Grading
    Services --> JPA
    Grading --> JPA
    JPA --> DB
    Services --> SubDir
```

**Communication model:**

- No Vite proxy — the frontend calls the backend directly with `fetch`.
- CORS is configured in `CorsConfig` for `/api/**` (localhost + production Vercel origin).
- Most endpoints are **open** at the Spring Security layer; JWT is checked **manually** in controllers that need it (upload, student history, user management).

---

## 4. Frontend — How It Runs

### 4.1 Bootstrap

1. `index.html` loads `main.jsx`.
2. `main.jsx` mounts React with:
   - `BrowserRouter` (React Router)
   - `ThemeProvider` (dark/light via `dark` class on `<html>`)
   - `App.jsx`

### 4.2 Application shell (`App.jsx`)

- **Auth state** lives in React `useState`, hydrated from `sessionStorage`:
  - `accessToken` — JWT from backend
  - `user` — JSON with `id`, `email`, `roles`, `irn`, etc.
- **Google OAuth** via `@react-oauth/google` (`GoogleOAuthProvider`).
- **Routing** with role guards (`RequireRole`):

| Path | Role | Screen |
|------|------|--------|
| `/` | — | Login |
| `/student-dashboard` | STUDENT | Upload + grades |
| `/student-history` | STUDENT | Submission history |
| `/lecturer-dashboard` | LECTURER | Grading overview |
| `/lecturer-grading` | LECTURER | Cross-lab grade matrix |
| `/lecturer-users` | LECTURER | User CRUD |
| `/lecturer-report` | LECTURER | Analytics reports |

Dual-role users default to the **lecturer** dashboard after login; student routes remain available by URL.

### 4.3 API integration pattern

Every page/component repeats:

```javascript
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const res = await fetch(`${API_BASE}/api/...`, {
  headers: { Authorization: `Bearer ${sessionStorage.getItem('accessToken')}` }
});
```

There is no shared HTTP client or interceptors.

### 4.4 Student dashboard flow

1. On load, `StudentDashboard.jsx` calls `GET /api/labs` to populate the lab dropdown.
2. When a lab is selected:
   - `GET /api/labs/{labId}/stats?studentId=` — current grade, attempt count, latest submission time (from **latest DB attempt**)
   - `GET /api/labs/{labId}/challenges` — challenge list (scores empty until upload in current session)
3. **Upload** via `DropZone.jsx`:
   - Student drops a folder like `12345_StudentName_lab_1/challenge_1/*.java` and `*.mmd`
   - Client validates folder structure, builds `FormData` with relative paths
   - `POST /api/submissions/{labId}/{attemptNumber}/upload` with `Authorization: Bearer <token>`
4. On upload success, the dashboard refreshes stats, challenge scores, and class/MMD detail for the session (in-memory cache per challenge).

### 4.5 Lecturer dashboard flow

`LecturerDashboard.jsx` switches sections via `activeNav`:

- **dashboard** — `GET /api/lecturer/overview`, lab statistics, paginated submissions
- **grading** — `GET /api/lecturer/grade-overview`
- **users** — `UserManagement.jsx` → `/api/users/*`
- **reports** — `Reports.jsx` → `GET /api/analytics/dashboard`

### 4.6 Login flow

`LoginUI.jsx` supports:

1. **IRN + password** → `POST /api/auth/login` → JWT + user payload
2. **Google** → `POST /api/auth/google` (existing user) or first-time `POST /api/auth/google/upsert`
3. **Forgot password** → email with reset link → `ResetPasswordUI` → `POST /api/auth/reset-password`

On success, `handleLoginSuccess` in `App.jsx` stores token/user in `sessionStorage` and navigates to the role-appropriate dashboard.

---

## 5. Backend — How It Runs

### 5.1 Startup

1. `EiuCapstoneBackendApplication` starts Spring Boot 3.2 on port **8002** (or `PORT` env).
2. `application.yml` imports `backend/.env`; `application.properties` configures PostgreSQL (HikariCP pool) and JPA (no Flyway — schema is managed externally).
3. Beans wire up: controllers, services, grading engine, caches, `JavaCompilerService`, JWT, CORS.
4. On startup, `TermEnrollmentSyncService` backfills `term_enrollment` from existing `student_lab_progress` (idempotent).

### 5.2 Layer structure

```
Controller (thin) → Service / GradingService → Repository (JPA) → PostgreSQL
```

| Controller | Base path | Purpose |
|------------|-----------|---------|
| `HealthController` | `/api` | Health check |
| `AuthController` | `/api/auth` | Login, Google, password reset |
| `LabController` | `/api/labs` | Labs, stats, lecturer submissions |
| `SubmissionController` | `/api/submissions` | Upload, grade, student history |
| `UserController` | `/api/users` | User CRUD (lecturer JWT) |
| `LecturerAnalyticsController` | `/api/lecturer` | Overview, grade matrix |
| `AnalyticsController` | `/api/analytics` | Dashboard, student reports |

Swagger UI: `http://localhost:8002/swagger-ui/index.html`

### 5.3 Security model

- `SecurityConfig`: CSRF off, **all requests permitted** at filter level.
- **JWT** created by `JwtService` (in-memory signing key — tokens invalidated on server restart).
- Claims: `email`, `name`, `domain`, `roles`, `irn`.
- `SubmissionController` requires Bearer token + valid `irn` (teachers cannot submit).
- `UserController` uses `JwtAuthHelper` for lecturer-only access.
- Google tokens verified via `GoogleTokenVerifier` (`@eiu.edu.vn` only).

### 5.4 Core submission pipeline (most important flow)

When `POST /api/submissions/{labId}/{attemptNumber}/upload` is called:

```
1. Parse JWT → resolve UserAccount (must have IRN)
2. Load Lab from DB
3. LabRubricCache.get(lab) → immutable rubric snapshot (cached 30 min)
4. SubmissionStorageService.processUpload(irn, requestId, files)
5. Upsert LabSubmission row (user + lab + attempt_number)
6. GradingService.gradeSubmission(...)
7. Save overall score to lab_submission
8. Update student_lab_progress (attempts, highest score, timestamps)
9. Save compile errors + MMD metadata to side stores
10. Invalidate analytics caches
11. Return challenge scores + overall score
12. finally: delete temp submission folder on disk
```

#### Step 4 — File handling (`SubmissionStorageService`)

- Expects multipart filenames with paths like:  
  `12345_Name_lab_1/challenge_1/Foo.java`
- Groups files by challenge folder (`challenge_1`, `challenge-2`, etc.)
- For each challenge **in parallel** (default 4 workers):
  - Save `.java` to `_sources_tmp/`
  - `JavaCompilerService.compile()` → output to `classes/`
  - Delete sources after compile
  - Compile errors are captured per challenge (upload continues)
- `.mmd` files are kept in memory (`mmdByChallenge`), not written to disk on the hot path
- Temp root: `submissions/<sanitized_irn>/<uuid-requestId>/`

#### Step 6 — Grading (`GradingService`)

For each challenge folder, **in parallel**:

1. **Java side**: `ReflectionClassParser` loads `.class` files via `URLClassLoader`, extracts fields, methods, constructors.
2. **MMD side**: `MmdParser` parses diagram text; `MmdComparisonService` compares to rubric (classes, members, relations).
3. **Merge rule**: For member/class elements, **both** Java and MMD must pass. Relations are MMD-only.
4. **Scoring**:
   - Per challenge: 50% Java accuracy + 50% MMD accuracy
   - Overall lab score: average across all rubric challenges (missing = 0%)
5. **Persist** via `GradingResultStore`:
   - `submission_field_result`, `submission_method_result`, `submission_constructor_result`, `submission_relation_result`, `submission_challenge_result`

After grading, the temp folder is deleted in a `finally` block — only DB rows and optional compile-error JSON remain.

### 5.5 Read paths (after grading)

| Endpoint | Data source |
|----------|-------------|
| `GET /api/labs/{id}/challenges` | Latest attempt per student; scores from `submission_challenge_result` |
| `GET /api/labs/{id}/challenges/{id}/class` | Element-level results + compile errors |
| `GET /api/submissions/my-history` | `StudentHistoryService` — submission list + stats |
| `GET /api/lecturer/overview` | Aggregated metrics (cached 90s) |
| `GET /api/labs/{id}/statistics` | Lab analytics (cached 120s; invalidated on upload) |

Student-facing grades always use the **latest attempt** for that lab (`ORDER BY attempt_number DESC`), not `best_submission_id` from progress.

---

## 6. Database — Schema and Role

PostgreSQL is the **single source of truth** for users, rubrics, submissions, and results. Schema is **not** auto-migrated by the app — operators run SQL scripts from `docs/sql/` manually.

### 6.1 Entity relationship (conceptual)

```mermaid
erDiagram
    user_account ||--o{ user_role : has
    role ||--o{ user_role : defines

    academic_year ||--o{ term : contains
    term ||--o{ lab : offers
    term ||--o{ term_enrollment : enrolls

    lab ||--o{ challenge : contains
    challenge ||--o{ class_entity : defines
    class_entity ||--o{ field : has
    class_entity ||--o{ method : has
    class_entity ||--o{ constructor : has
    class_entity ||--o{ class_relation : relates

    user_account ||--o{ lab_submission : submits
    lab ||--o{ lab_submission : receives
    lab_submission ||--o{ submission_field_result : stores
    lab_submission ||--o{ submission_method_result : stores
    lab_submission ||--o{ submission_constructor_result : stores
    lab_submission ||--o{ submission_relation_result : stores
    lab_submission ||--o{ submission_challenge_result : stores

    user_account ||--o| student_lab_progress : tracks
    lab ||--o| student_lab_progress : tracks
```

### 6.2 Key tables

| Table | Purpose |
|-------|---------|
| `user_account` | Students/lecturers: email, password hash, student_code, teacher_code, `is_active` |
| `user_role` / `role` | Many-to-many roles: `STUDENT`, `LECTURER` |
| `lab` | Lab name, linked to `term` |
| `challenge` | Numbered challenges per lab (`challenge_number` unique per lab) |
| `class_entity`, `field`, `method`, `constructor` | **Rubric** — expected OOP structure |
| `class_relation` | Expected UML relations (graded from MMD) |
| `master_data` | Scope/type labels for rubric attributes |
| `lab_submission` | One row per (user, lab, attempt_number); stores `score`, `submitted_at` |
| `submission_*_result` | Per-element pass/fail tied to rubric element IDs |
| `student_lab_progress` | Per-student-per-lab: attempt count, highest score, best submission ID |
| `term_enrollment` | Which students are enrolled in a term (roster denominator for lecturers) |
| `password_reset_token` | Hashed tokens for forgot-password flow |

### 6.3 JPA configuration

- Connection pool: HikariCP (max 10 connections)
- `spring.jpa.open-in-view=false` — no lazy-loading outside transactions
- Batch inserts/updates enabled for performance
- UUID primary keys on most entities

### 6.4 What is NOT in the database

- Compiled `.class` files (deleted after grading)
- Uploaded `.java` sources (deleted after compile)
- `.mmd` file contents on disk (parsed in memory; archival hook is no-op by default)
- JWT signing keys (in-memory only)
- Analytics cache entries (in-process TTL caches)

---

## 7. End-to-End Flow Examples

### 7.1 Student login and upload

```
1. User opens http://localhost:5173
2. LoginUI → POST /api/auth/google or /api/auth/login
3. Backend verifies credentials → returns JWT
4. Frontend stores token → navigates to /student-dashboard
5. GET /api/labs → student picks "Lab 1"
6. GET /api/labs/{id}/stats → shows prior grade if any
7. Student drops folder → DropZone POST /api/submissions/{labId}/1/upload
8. Backend: compile → reflect → compare MMD → save results → return scores
9. Frontend updates challenge sidebar, class tab, stats cards
10. Data persists in PostgreSQL; temp files deleted
```

### 7.2 Lecturer views class performance

```
1. Lecturer logs in → /lecturer-dashboard
2. GET /api/lecturer/overview → summary cards (at-risk count, recent submissions)
3. Selects lab → GET /api/labs/{id}/statistics
4. GET /api/labs/{id}/submissions?page=0 → paginated roster from term_enrollment
5. Clicks student → GET /api/labs/{id}/students/{id}/attempts
6. Opens challenge drawer → GET /api/labs/{id}/challenges/{id}/class?studentId=
```

### 7.3 Re-upload same attempt

- Unique key: `(user_id, lab_id, attempt_number)` on `lab_submission`
- Re-uploading attempt `1` **updates** the same row and upserts result rows
- `attemptsCount` does not increment on re-upload of an existing attempt

---

## 8. Caching and Performance

| Cache | TTL | Invalidation |
|-------|-----|--------------|
| `LabRubricCache` | 30 min | Manual or TTL |
| `MasterDataCache` | 60 min | TTL |
| `LecturerOverviewCache` | 90 sec | On upload |
| `LabStatisticsCache` | 120 sec | On upload for that lab |
| `AnalyticsDashboardCache` | 180 sec | TTL |

Grading uses a thread pool (`app.grading.parallelism=4`, capped at CPU count) for parallel challenge compile + grade.

---

## 9. Deployment Topology (Production)

| Tier | Platform | Notes |
|------|----------|-------|
| Frontend | **Vercel** | Static build from `frontend/dist`; `VITE_API_URL` points to Render backend |
| Backend | **Render** (Docker) | Multi-stage `Dockerfile`; needs JDK |
| Database | **Neon PostgreSQL** | Use pooler hostname (`-pooler`) for JVM |

CORS allows `https://oop-autograder.vercel.app`. Password-reset emails pick the request `Origin` when it matches an allowed frontend.

---

## 10. Summary

| Layer | Technology | Runs on | Persists |
|-------|------------|---------|----------|
| **Frontend** | React 18 + Vite 7 + Tailwind | Browser `:5173` | `sessionStorage` (JWT, user) |
| **Backend** | Spring Boot 3.2 + Java 17 | JVM `:8002` | PostgreSQL + temp `submissions/` |
| **Database** | PostgreSQL (Neon) | Cloud | All users, rubrics, grades, progress |

The **critical path** is: **browser upload → Spring controller → temp compile → reflection + MMD grading → JPA save → JSON response → React UI update**. Everything durable lives in PostgreSQL; everything on disk during upload is ephemeral.
