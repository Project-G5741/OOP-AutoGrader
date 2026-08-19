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
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL — use Neon **pooler** hostname (`-pooler`) for production JVM |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `GOOGLE_CLIENT_ID` | Google OAuth audience validation |
| `JWT_SECRET` | Defined in config but **not currently used** by `JwtService` |
| `FRONTEND_URL` | CORS allowed origin; fallback reset-link base when `Origin` header absent |
| `RESET_FRONTEND_URL` | Optional override for fallback reset-link base (defaults to `FRONTEND_URL`) |

Password-reset emails use the request `Origin` when it matches an allowed frontend (localhost or `https://oop-autograder.vercel.app`), so one backend can serve both local and production SPAs.
| `MAIL_PROVIDER` | `smtp` (local) or `brevo` (Render free tier — SMTP ports blocked) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Gmail SMTP when `MAIL_PROVIDER=smtp` |
| `BREVO_API_KEY`, `MAIL_FROM` | Brevo HTTPS API when `MAIL_PROVIDER=brevo` (verify sender in Brevo dashboard) |
| `SUBMISSION_BASE_DIR` | Upload temp root (default `submissions/`) |
| `PORT` | Server port (default `8002`) |

Config files: `src/main/resources/application.yml` (imports `.env`), `application.properties` (datasource, storage path).

### API surface

| Controller | Base path | Notes |
|---|---|---|
| `HealthController` | `/api` | `GET /api/health` |
| `AuthController` | `/api/auth` | Google login/upsert, IRN+password login, forgot/reset password |
| `LabController` | `/api/labs` | List labs, lab stats, lecturer lab statistics/submissions |
| `LecturerRubricController` | `/api/lecturer/labs` | Lab structure read/save, lab create/delete (lecturer JWT); challenge testcase CRUD + dry-run |
| `MasterDataController` | `/api/master-data` | Master data lookup by category |
| `TermController` | `/api/terms` | Academic term list for lab creation |
| `AnalyticsController` | `/api/analytics` | Dashboard, lab trend, student overview/report |
| `UserController` | `/api/users` | CRUD + bulk create (soft-delete); **lecturer JWT required** on all except self-service `POST /change-password` |
| `SubmissionController` | `/api/submissions` | Upload + grade + student history reads (JWT required) |

Swagger UI: `http://localhost:8002/swagger-ui/index.html`

### Security posture

- `SecurityConfig` permits all requests; CSRF disabled
- JWT is parsed manually in `SubmissionController` for upload and student history reads — other endpoints are unauthenticated except `/api/users/*` (lecturer JWT via `JwtAuthHelper`)
- `JwtService` regenerates signing key on every restart (tokens invalidated on restart)
- Google auth enforces `@eiu.edu.vn` domain via `GoogleTokenVerifier`

### Persistence

- JPA entities in `model/`, repositories in `repository/`
- Schema managed externally — no Flyway/Liquibase migrations in repo
- Rubric chain: `Lab` → `Challenge` → `ClassEntity` → `Field`/`Method`/`Constructor`; `ClassRelation` (MMD source→target + `RELATION_TYPE` master data) per challenge
- Soft-delete: users set `isActive=false`

### Submission resolution

Student-facing challenge scores, Class tab, and stats **current grade** use the student's **latest attempt** for the lab (`LabSubmissionRepository.findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc`). `student_lab_progress.best_submission_id` and `highest_score` are still updated on upload but are not used for student dashboard display.

### Submission pipeline (summary)

Upload → rubric cache load → `SubmissionStorageService` (parallel in-memory compile per challenge via `compileExecutor`) → `GradingService` (parallel reflect + MMD parse/compare + merge) → MMD hook (no-op by default) → cleanup temp folder.

Grading tuning properties (`application.properties`):

| Property | Default | Purpose |
|---|---|---|
| `app.grading.parallelism` | `4` | Max concurrent challenge workers during grading (capped at CPU count) |
| `app.compile.parallelism` | `4` | Max concurrent per-challenge compile workers during upload (capped at CPU count) |
| `app.grading.testcase-invoke-timeout-seconds` | `5` | Per-invocation timeout for operational testcases |
| `testcaseInvokeExecutor` bean | single thread | Serializes student code invocation and stdout capture |
| `pillarExecutor` bean | `max(2, parallelism×2)` threads | MMD + testcase pillars inside each challenge; separate from `gradingExecutor` to avoid pool deadlock on 1–2 CPU hosts (Render) |
| `app.grading.rubric-cache-ttl-minutes` | `30` | In-process lab rubric cache TTL |
| `app.grading.timing-log` | `false` | Log upload (`rubric_ms`, `process_ms`, `grade_ms`, `total_ms`) and read paths (`challenges_ms`, `class_ms`, `stats_ms`) |
| `app.master-data-cache-ttl-minutes` | `60` | In-process master data (scope/type labels) cache TTL |
| `app.analytics.lecturer-overview-cache-ttl-seconds` | `90` | TTL for `/api/lecturer/overview` in-process cache |
| `app.analytics.dashboard-cache-ttl-seconds` | `180` | TTL for `/api/analytics/dashboard` per filter set |
| `app.analytics.lab-statistics-cache-ttl-seconds` | `120` | TTL for `/api/labs/{id}/statistics`; invalidated on upload for that lab |

**Analytics caches:** In-process only. Multi-instance Render deploys see independent TTL staleness per instance. Lecturer overview and analytics dashboard may be stale up to configured TTL; lab statistics invalidate on the instance that handled the upload.

**Schema scripts:** Operator-run SQL in `docs/sql/` (e.g. `docs/sql/2026-08-07-analytics-indexes.sql`).

### Read-path performance

- `SubmissionResultLoader` — single JOIN FETCH load of correct field/method/constructor IDs per submission
- `MasterDataCache` — cached scope/type labels; `ClassStructureService` uses batched rubric queries (same pattern as `LabRubricService`)
- `ChallengeService` — one submission-result load + batched classes/members for all challenges in a lab
- `LabStructureService.saveLabStructure` — prefetches the full lab tree once (`SaveContext`: challenges, classes, fields/methods/constructors, relations, master data), syncs from in-memory maps (no per-entity `findById`), batches `saveAll` per challenge for classes/members/relations (parameters bulk-deleted/reinserted per challenge), logs `structure_save_timing` when `app.grading.timing-log=true`, returns the request payload (no post-save full reload)
- Upload response `challengeResult` is `Map<UUID, Integer>` (scores only); class detail via `GET /challenges/{id}/class`
- `attemptsCount` on progress is maintained incrementally on upload (new attempt increments; re-upload of same attempt does not recount)
- Per-challenge compile failures are stored in `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json` and shown on Class tab cards
- Per-challenge package-normalization notices (when student sources include `package` declarations) are stored in `{SUBMISSION_BASE_DIR}/_package_normalization/{submissionId}.json` and shown as a non-blocking warning on the student Class tab
- Per-challenge MMD metadata (file presence, class-in-diagram, relation error labels) is stored in `{SUBMISSION_BASE_DIR}/_mmd_meta/{submissionId}.json` at upload; `ClassStructureService` infers MMD was submitted from persisted DB results when that file is missing (e.g. ephemeral storage wipe)
- Parsed submission display snapshots for Class/MMD tabs are stored in `{SUBMISSION_BASE_DIR}/_parsed_snapshot/{submissionId}.json` at grade time; when missing (legacy submissions or storage wipe), tabs fall back to rubric-template labels
- `GET /api/labs/{labId}/stats` — lab-scoped stats for parallel dashboard load
- `GET /api/labs/{labId}/statistics` — lecturer lab analytics (scores, completion from active term enrollees, grade distribution)
- `GET /api/labs/{labId}/submissions` — paginated unique student roster (from `student_lab_progress` or `term_enrollment`; default page size 5); **score** field is `highest_score`; sort by `studentName` or `score`
- `GET /api/labs/{labId}/submissions/export` — full roster in one query (lecturer export); same score semantics and `sort` param
- `GET /api/labs/{labId}/students/{studentId}/attempts` — lab attempt history for lecturer roster View
- `GET /api/submissions/my-labs` — student's per-lab performance summary for history sidebar
- `GET /api/submissions/my-history` — student's submission list + stats (optional `labId` filter; `page`, `size`, `sort` for pagination)
- `GET /api/labs/{labId}/challenges/{challengeId}/students` — paginated student roster for challenge tab (same population as lab roster; score from `submission_challenge_result` or computed from element results when legacy rows are missing)
- `TermEnrollmentSyncService` — on startup, backfills `term_enrollment` from existing `student_lab_progress` (idempotent)
- `GET /api/lecturer/overview` — lecturer dashboard overview cards; **at-risk count** uses the same total-score rule as grade overview (average of highest lab scores, missing labs as 0; threshold < 70)
- `GET /api/lecturer/grade-overview` — cross-lab student grade matrix (paginated; per-lab score from `student_lab_progress.highest_score`; total = sum ÷ lab count); sort by `studentName`, `irn`, `score`, or `labScore,<labUuid>,<asc|desc>`
- `GET /api/analytics/dashboard` — reports page analytics (returns 200 with empty/null fields when no data)

## Work Guidance

- Controllers stay thin; business logic belongs in `service/` or `grading/`
- Throw `SubmissionProcessingException` for upload/compile failures — handled by `GlobalExceptionHandler` (422)
- `LabService` exists but `LabController` calls `LabRepository` directly — follow existing pattern per endpoint
- New API endpoints need CORS coverage in `CorsConfig` if called from frontend

## Verification

- No automated test suite in Docker build (`-DskipTests`); local: `mvn test` from `backend/` includes `SubmissionStorageServiceTest` and `JavaCompilerServiceTest`
- Manual: Swagger UI, `GET /api/health`, submission upload from frontend `DropZone`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` | Reflection parser, rubric comparison, scoring |
| `src/main/java/com/eiu/capstone/backend/service/AGENTS.md` | Submission storage, Java compilation, auth, user services |
