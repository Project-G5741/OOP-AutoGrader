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
| `RootController` | `/` | `GET /` — liveness probe (Render health check) |
| `AuthController` | `/api/auth` | Google login/upsert, IRN+password login, forgot/reset password. Unregistered Google users: 403 (frontend first-time setup). Inactive Google users: 423 (not setup). Inactive IRN login: 403. |
| `LabController` | `/api/labs` | List labs (with `deadlineDate`, `urgencyState`, natural name sort), lab stats, lecturer lab statistics/submissions |
| `LecturerRubricController` | `/api/lecturer/labs` | Lab structure read/save, create/delete, `PATCH /{labId}/deadline`; challenge testcase CRUD + dry-run |
| `LecturerTermController` | `/api/lecturer/terms` | Create term (year + term number), set current term, enroll/remove students, Excel import by IRN + email, `GET /{termId}/roster` (enrolled + available in one call) |
| `LecturerAnalyticsController` | `/api/lecturer` | Overview, grade overview, `GET /plagiarism/flags`, `GET /labs/{labId}/plagiarism` |
| `MasterDataController` | `/api/master-data` | Master data lookup by category |
| `TermController` | `/api/terms` | Academic term list for lab creation |
| `StudentAccessController` | `/api/students` | `GET /term-access` — whether the student is in the current term |
| `AnalyticsController` | `/api/analytics` | Dashboard, lab trend, student overview/report |
| `UserController` | `/api/users` | CRUD + bulk create (soft-delete); `POST /{id}/suspend` and `POST /{id}/unsuspend` for student-only accounts; **lecturer JWT required** on all except self-service `POST /change-password` |
| `SubmissionController` | `/api/submissions` | Upload + grade + student history reads (JWT required) |

Swagger UI: `http://localhost:8002/swagger-ui/index.html`

### Security posture

- `SecurityConfig` permits all requests; CSRF disabled
- JWT is parsed manually in `SubmissionController` for upload and student history reads — other endpoints are unauthenticated except lecturer JWT via `JwtAuthHelper.requireLecturer` (`/api/users/*`, `/api/lecturer/labs`, `/api/lecturer/terms`)
- `JwtService` regenerates signing key on every restart (tokens invalidated on restart)
- Google auth enforces `@eiu.edu.vn` domain via `GoogleTokenVerifier`

### Persistence

- JPA entities in `model/`, repositories in `repository/`
- Schema managed externally — no Flyway/Liquibase migrations in repo
- Rubric chain: `Lab` → `Challenge` → `ClassEntity` → `Field`/`Method`/`Constructor`; `ClassRelation` (MMD source→target + `RELATION_TYPE` master data) per challenge
- Scoring weights (int, min 1, default 1): `challenge.weight`, `challenge.class_weight`, `challenge.mmd_weight`, `class_entity.weight` — operator SQL `docs/sql/2026-08-19-scoring-weights.sql`. Labs have no weight. Native lecturer SQL must use `CAST(l.deadline_date AS timestamp)`, not `::timestamp` (Hibernate treats `:` as a parameter).
- Plagiarism (operator SQL `docs/sql/2026-08-19-plagiarism.sql`): after upload, compare this student to other students in the same lab — git commit hashes in order (100%), git metadata (100%), `.java`/`.mmd` SHA-256 Jaccard `> 90%`. Flag if any check fires. Missing `.git` skips git/metadata only.
- `Lab.deadline_date` (optional `DATE`) — end 23:59:59 Vietnam time; lecturer score SQL uses qualifying submissions on or before cutoff; extend deadline to backfill from history
- `lab_deadline_email_sent` — ledger for 72h/24h reminder emails to enrolled non-submitters (`LabDeadlineReminderScheduler`, minutely)
- Soft-delete: users set `isActive=false`; inactive accounts cannot log in
- Lecturer **suspend** (`POST /api/users/{id}/suspend`) is student-only `isActive=false`; restore via `POST /api/users/{id}/unsuspend`. Lecturer and dual-role accounts cannot be suspended this way.
- `term.is_current` — lecturer-selected current term; operator SQL `docs/sql/2026-08-19-term-current.sql`. Students in that term may submit; others only use history.

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
| `app.grading.timing-log` | `false` | Print aligned `[timing]` blocks (`utility/TimingLog`) for upload, compile, each challenge, grade submission, structure save, and read paths |
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
- `LabStructureService.saveLabStructure` — prefetches the full lab tree once (`SaveContext`: challenges, classes, fields/methods/constructors, relations, master data), syncs from in-memory maps (no per-entity `findById`), batches `saveAll` per challenge for classes/members/relations (parameters bulk-deleted/reinserted per challenge), prints a `[timing] Save lab structure` block when `app.grading.timing-log=true`, returns the request payload (no post-save full reload)
- Upload response `challengeResult` is `Map<UUID, Integer>` (scores only); class detail via `GET /challenges/{id}/class`
- `attemptsCount` on progress is maintained incrementally on upload (new attempt increments; re-upload of same attempt does not recount)
- Per-challenge compile failures are stored in `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json` and shown on Class tab cards
- Per-challenge package-normalization notices (when student sources include `package` declarations) are stored in `{SUBMISSION_BASE_DIR}/_package_normalization/{submissionId}.json` and shown as a non-blocking warning on the student Class tab
- Per-challenge MMD metadata (file presence, class-in-diagram, relation error labels) is stored in `{SUBMISSION_BASE_DIR}/_mmd_meta/{submissionId}.json` at upload; `ClassStructureService` infers MMD was submitted from persisted DB results when that file is missing (e.g. ephemeral storage wipe)
- Parsed submission display snapshots for Class/MMD tabs are stored in `{SUBMISSION_BASE_DIR}/_parsed_snapshot/{submissionId}.json` at grade time; when missing (legacy submissions or storage wipe), tabs fall back to rubric-template labels
- `GET /api/labs` — student-facing lab list (`deadlineDate`, `urgencyState`); with a student JWT, only current-term labs if the student is enrolled; lecturers still see all labs. Upload loads the lab with `findByIdWithTerm` so submit access does not lazy-load `lab.term`.
- `GET /api/labs/{labId}/statistics` — lecturer lab analytics (scores, completion from active term enrollees, grade distribution)
- `GET /api/labs/{labId}/submissions` — paginated unique student roster (from `student_lab_progress` or `term_enrollment`; default page size 5); **score** is best qualifying submission before lab deadline (null when none or only late submissions); sort by `studentName` or `score`
- `GET /api/labs/{labId}/submissions/export` — full roster in one query (lecturer export); same score semantics and `sort` param
- `GET /api/labs/{labId}/students/{studentId}/attempts` — lab attempt history for lecturer roster View
- `GET /api/submissions/my-labs` — student's per-lab performance summary for history sidebar
- `GET /api/submissions/my-history` — student's submission list + stats (optional `labId` filter; `page`, `size`, `sort` for pagination)
- `GET /api/labs/{labId}/challenges/{challengeId}/students` — paginated student roster for challenge tab (same population as lab roster; score from `submission_challenge_result` or computed from element results when legacy rows are missing)
- `TermEnrollmentSyncService` — on startup, backfills `term_enrollment` from existing `student_lab_progress` (idempotent)
- `GET /api/lecturer/overview` — lecturer dashboard overview cards; **at-risk count** uses the same total-score rule as grade overview (average of highest lab scores, missing labs as 0; threshold < 70)
- `GET /api/lecturer/grade-overview` — cross-lab student grade matrix (paginated, default page size 10; per-lab score from `student_lab_progress.highest_score`; total = sum ÷ lab count); sort by `studentName`, `irn`, `score`, or `labScore,<labUuid>,<asc|desc>`
- `GET /api/analytics/dashboard` — reports page analytics (returns 200 with empty/null fields when no data)

## Work Guidance

- Controllers stay thin; business logic belongs in `service/` or `grading/`
- Throw `SubmissionProcessingException` for upload/compile failures — handled by `GlobalExceptionHandler` (422)
- `LabService` exists but `LabController` calls `LabRepository` directly — follow existing pattern per endpoint
- New API endpoints need CORS coverage in `CorsConfig` if called from frontend

## Verification

- No automated test suite in Docker build (`-DskipTests`); local: `mvn test` from `backend/` includes `SubmissionStorageServiceTest`, `JavaCompilerServiceTest`, `StudentTermAccessServiceTest`, `TermServiceImportTest`, and `PasswordResetServiceTest`
- Manual: Swagger UI, `GET /`, submission upload from frontend `DropZone`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` | Reflection parser, rubric comparison, scoring |
| `src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md` | Git / metadata / file-hash plagiarism checks |
| `src/main/java/com/eiu/capstone/backend/service/AGENTS.md` | Submission storage, Java compilation, auth, user services |
