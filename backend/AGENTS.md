# Backend

## Purpose

Spring Boot 3.2 / Java 17 REST API for the OOP AutoGrader: authentication, user and lab management, student submission upload, Java compilation, and reflection-based OOP grading against a PostgreSQL rubric.

## Ownership

- Package root: `com.eiu.capstone.backend`
- Entry point: `EiuCapstoneBackendApplication.java`
- Durable state: PostgreSQL (users, labs, rubrics, submission results)
- Ephemeral state: `SUBMISSION_BASE_DIR` (compiled classes, `.mmd`, temp sources — deleted after grading)

## Local Contracts

### Run and deploy

- Local: `mvn spring-boot:run` from `backend/` (port `8002` by default)
- Root orchestration: `npm run backend` from repository root
- Docker: multi-stage `Dockerfile`; see `DEPLOY_RENDER.md` for Render deploy
- **Requires a JDK** (not JRE) — `JavaCompilerService` uses `javax.tools.JavaCompiler`

### Environment

Copy `backend/.env.backend.example` to `backend/.env`. Key variables:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `GOOGLE_CLIENT_ID` | Google OAuth audience validation |
| `JWT_SECRET` | Defined in config but **not currently used** by `JwtService` |
| `FRONTEND_URL` | CORS allowed origin |
| `SUBMISSION_BASE_DIR` | Upload temp root (default `submissions/`) |
| `PORT` | Server port (default `8002`) |

Config files: `src/main/resources/application.yml` (imports `.env`), `application.properties` (datasource, storage path).

### API surface

| Controller | Base path | Notes |
|---|---|---|
| `HealthController` | `/api` | `GET /api/health` |
| `AuthController` | `/api/auth` | Google login/upsert, IRN+password login |
| `LabController` | `/api/labs` | List labs, lab stats, lecturer lab statistics/submissions |
| `LecturerAnalyticsController` | `/api/lecturer` | `GET /api/lecturer/overview` |
| `AnalyticsController` | `/api/analytics` | Dashboard, lab trend, student overview/report |
| `UserController` | `/api/users` | CRUD + bulk create (soft-delete) |
| `SubmissionController` | `/api/submissions` | Upload + grade (JWT required) |

Swagger UI: `http://localhost:8002/swagger-ui/index.html`

### Security posture

- `SecurityConfig` permits all requests; CSRF disabled
- JWT is parsed manually in `SubmissionController` only — other endpoints are unauthenticated
- `JwtService` regenerates signing key on every restart (tokens invalidated on restart)
- Google auth enforces `@eiu.edu.vn` domain via `GoogleTokenVerifier`

### Persistence

- JPA entities in `model/`, repositories in `repository/`
- Schema managed externally — no Flyway/Liquibase migrations in repo
- Rubric chain: `Lab` → `Challenge` → `ClassEntity` → `Field`/`Method`/`Constructor`
- Soft-delete: users set `isActive=false`

### Submission resolution

Student-facing challenge scores, Class tab, and stats **current grade** use the student's **latest attempt** for the lab (`LabSubmissionRepository.findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc`). `student_lab_progress.best_submission_id` and `highest_score` are still updated on upload but are not used for student dashboard display.

### Submission pipeline (summary)

Upload → rubric cache load → `SubmissionStorageService` (parallel save + compile per challenge) → `GradingService` (parallel reflect + MMD parse/compare + merge) → MMD hook (no-op by default) → cleanup temp folder.

Grading tuning properties (`application.properties`):

| Property | Default | Purpose |
|---|---|---|
| `app.grading.parallelism` | `4` | Max concurrent challenge workers (capped at CPU count) |
| `app.grading.rubric-cache-ttl-minutes` | `30` | In-process lab rubric cache TTL |
| `app.grading.timing-log` | `false` | Log upload (`rubric_ms`, `process_ms`, `grade_ms`, `total_ms`) and read paths (`challenges_ms`, `class_ms`, `stats_ms`) |
| `app.master-data-cache-ttl-minutes` | `60` | In-process master data (scope/type labels) cache TTL |

### Read-path performance

- `SubmissionResultLoader` — single JOIN FETCH load of correct field/method/constructor IDs per submission
- `MasterDataCache` — cached scope/type labels; `ClassStructureService` uses batched rubric queries (same pattern as `LabRubricService`)
- `ChallengeService` — one submission-result load + batched classes/members for all challenges in a lab
- Upload response `challengeResult` is `Map<UUID, Integer>` (scores only); class detail via `GET /challenges/{id}/class`
- `attemptsCount` on progress is synced to the count of `lab_submission` rows (one per attempt); re-upload does not add rows
- Per-challenge compile failures are stored in `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json` and shown on Class tab cards
- `GET /api/labs/{labId}/stats` — lab-scoped stats for parallel dashboard load
- `GET /api/labs/{labId}/statistics` — lecturer lab analytics (scores, completion, grade distribution)
- `GET /api/labs/{labId}/submissions` — paginated submission summaries for lecturer dashboard
- `GET /api/lecturer/overview` — lecturer dashboard overview cards
- `GET /api/analytics/dashboard` — reports page analytics (returns 200 with empty/null fields when no data)

## Work Guidance

- Controllers stay thin; business logic belongs in `service/` or `grading/`
- Throw `SubmissionProcessingException` for upload/compile failures — handled by `GlobalExceptionHandler` (422)
- `LabService` exists but `LabController` calls `LabRepository` directly — follow existing pattern per endpoint
- New API endpoints need CORS coverage in `CorsConfig` if called from frontend

## Verification

- No automated tests exist (`src/test/` empty, `-DskipTests` in Docker build)
- Manual: Swagger UI, `GET /api/health`, submission upload from frontend `DropZone`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` | Reflection parser, rubric comparison, scoring |
| `src/main/java/com/eiu/capstone/backend/service/AGENTS.md` | Submission storage, Java compilation, auth, user services |
