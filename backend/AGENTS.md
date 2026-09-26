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

- Local: `mvn spring-boot:run` from `backend/` (port `8002` by default). Operational testcase invoke uses `target/backend-1.0.0-worker.jar` (`WORKER_JAR`); after changing worker/kernel code run `mvn package -DskipTests` (root `npm run backend` does this before `spring-boot:run`)
- Root orchestration: `npm run backend` from repository root
- Docker: multi-stage `Dockerfile`; copies `backend-1.0.0.jar` → `/app/app.jar` and `backend-1.0.0-worker.jar` → `/app/worker.jar` by name; API start is `exec java $JAVA_OPTS -jar app.jar` (default `-Xmx256m`); worker stays `-Xmx64m` and does not inherit `JAVA_OPTS`; see `DEPLOY_RENDER.md` for Render deploy
- Operational testcase invoke runs in the thin worker JAR (one JVM per lecturer dry-run or per student upload when any challenge has OT; host slot of 1 on the HTTP thread). Class-tab parse stays in the API with `Class.forName(..., false, ...)`. Worker env is allowlisted; that is not a filesystem or `/proc` jail.
- **Requires a JDK** (not JRE) — `JavaCompilerService` uses `javax.tools.JavaCompiler`

### Environment

Copy `backend/.env.backend.example` to `backend/.env`. Key variables:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL — use Neon **pooler** hostname (`-pooler`) for production JVM |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `GOOGLE_CLIENT_ID` | Google OAuth audience validation |
| `JWT_SECRET` | HS256 JWT signing key (**required**, ≥32 bytes). Missing or too-short values fail startup. Generate locally with `openssl rand -base64 32`. Never commit a production value. |
| `FRONTEND_URL` | CORS allowed origin; fallback reset-link base when `Origin` header absent |
| `RESET_FRONTEND_URL` | Optional override for fallback reset-link base (defaults to `FRONTEND_URL`) |

Password-reset emails use the request `Origin` when it matches an allowed frontend (localhost or `https://oop-autograder.vercel.app`), so one backend can serve both local and production SPAs.
| `MAIL_PROVIDER` | `smtp` (local) or `brevo` (Render free tier — SMTP ports blocked) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Gmail SMTP when `MAIL_PROVIDER=smtp` |
| `BREVO_API_KEY`, `MAIL_FROM` | Brevo HTTPS API when `MAIL_PROVIDER=brevo` (verify sender in Brevo dashboard) |
| `SUBMISSION_BASE_DIR` | Upload temp root (default `submissions/`) |
| `SPRINGDOC_ENABLED` | OpenAPI/Swagger. Default `true` locally. Set `false` in production so `/v3/api-docs` and `/swagger-ui/**` are not registered. |
| `PORT` | Server port (default `8002`) |
| `JAVA_OPTS` | Docker/Render API JVM flags only (image default `-Xmx256m`). Expanded by the Dockerfile entrypoint. Ignored by `mvn spring-boot:run`. Do not set `-Xmx512m` on 512MB hosts (worker needs headroom). |

Config files: `src/main/resources/application.yml` (imports `.env`), `application.properties` (datasource, storage path).

### API surface

| Controller | Base path | Notes |
|---|---|---|
| `RootController` | `/` | `GET /` — liveness probe (Render health check) |
| `AuthController` | `/api/auth` | Google login/upsert, IRN+password login, forgot/reset password. Unregistered Google users: 403 (frontend first-time setup). Inactive Google users: 423 (not setup). Inactive IRN login: 403. |
| `LabController` | `/api/labs` | List labs (with `deadlineDate`, `urgencyState`, natural name sort), lab stats, lecturer lab statistics/submissions |
| `LecturerRubricController` | `/api/lecturer/labs` | Lab structure read/save, create/delete, `PATCH /{labId}/deadline`, `PATCH /{labId}/student-access`; challenge testcase CRUD + dry-run |
| `LecturerTermController` | `/api/lecturer/terms` | Create term (year + term number), set current term, delete non-current term (no labs), enroll/remove students, Excel import by IRN or email, `GET /{termId}/roster` (enrolled + available in one call) |
| `LecturerAnalyticsController` | `/api/lecturer` | Overview, grade overview, `GET /plagiarism/flags`, `GET /labs/{labId}/plagiarism`, `GET /labs/{labId}/students/{studentId}/plagiarism` |
| `MasterDataController` | `/api/master-data` | Master data lookup by category |
| `TermController` | `/api/terms` | Academic term list for lab creation |
| `StudentAccessController` | `/api/students` | `GET /term-access` — whether the student is in the current term |
| `AnalyticsController` | `/api/analytics` | Dashboard, lab trend, student overview/report |
| `UserController` | `/api/users` | CRUD + bulk create; `DELETE /{id}` hard-deletes user and related rows; `POST /{id}/suspend` and `POST /{id}/unsuspend` for student-only accounts; **lecturer JWT required** on all except self-service `POST /change-password` |
| `SubmissionController` | `/api/submissions` | Upload + grade + student history reads (JWT required) |
| `PresenceController` | `/api/presence` | `GET` — public active-user count; a valid JWT records a heartbeat. `DELETE` — signed-in leave (JWT required) |

Swagger UI: `http://localhost:8002/swagger-ui/index.html` (unauthenticated locally when `SPRINGDOC_ENABLED` is true; omit or set `false` in production)

### Security posture

- Default-deny Spring Security: `JwtAuthenticationFilter` is the only JWT parser and validates claim `sv` via `SessionValidityService` (missing user or version mismatch → unauthenticated); matcher table authorizes by path + method; anonymous → 401, authenticated without role → 403
- No role hierarchy. `TEACHER` in a token maps to `LECTURER`. Dual-role accounts need both `STUDENT` and `LECTURER` authorities
- **Lecturer JWT (`hasRole(LECTURER)`):** `/api/users/**` except `POST /api/users/change-password`, `/api/lecturer/**`, `/api/analytics/**`, `/api/master-data/**`, `/api/terms/**`, lecturer lab statistics/submissions/export/attempts and challenge student roster under `/api/labs`
- **Student or lecturer (`hasAnyRole`):** `POST /api/users/change-password`, `/api/labs/**` after the lecturer-specific lab rows, challenge reads, lab list/stats
- **Student (`hasRole(STUDENT)`):** `/api/submissions/**`, `/api/students/**`
- **Public:** `OPTIONS /**`, `GET /`, `GET /api/presence`, `/api/auth/**`, swagger/OpenAPI when springdoc is enabled
- `JwtAuthHelper` is identity only (`requireActiveUser`, `resolveStudentScope`, `resolveDisclosureMode`, `isStudentOnly`) — not authorization
- `JwtService` derives the HS256 signing key once at construction from `jwt.secret` (`JWT_SECRET`); missing, blank, or shorter-than-32-byte values fail startup (no random per-restart key)
- `UserAccount.passwordHash` omitted from JSON (`@JsonIgnore`)
- Google auth enforces `@eiu.edu.vn` domain and configured `GOOGLE_CLIENT_ID` audience via `GoogleTokenVerifier`
- Password-reset request does not reveal whether an email exists (anti-enumeration)
- JWT routes re-check active account via `requireActiveUser`

### Persistence

- JPA entities in `model/`, repositories in `repository/`
- Schema managed externally — no Flyway/Liquibase migrations in repo
- Rubric chain: `Lab` → `Challenge` → `ClassEntity` → `Field`/`Method`/`Constructor`; `ClassRelation` (MMD source→target + `RELATION_TYPE` master data) per challenge
- Scoring weights (int, min 1, default 1): `challenge.weight`, `challenge.class_weight`, `challenge.mmd_weight`, `challenge.testcase_weight`, `class_entity.weight` — operator SQL `docs/sql/2026-08-19-scoring-weights.sql` and `docs/sql/2026-08-22-testcase-weight.sql`. Labs have no weight. Native lecturer SQL must use `CAST(l.deadline_date AS timestamp)`, not `::timestamp` (Hibernate treats `:` as a parameter).
- Operational testcase persistence (operator SQL `docs/sql/2026-09-23-operational-testcase-unit-composition.sql`; also applied on startup by `TestcaseSchemaMigrator` when leftover types/columns remain): wipe CASCADE of OT graphs, drop `testcase_instance` / `oop_principle_tag` / per-testcase `weight` / `COMPARISON_RESULT`, rewrite `testcase_type` to `UNIT` | `COMPOSITION`. After wipe the migrator calls `LabRubricCache.invalidateAll()`. Kept columns: `testcase_invocation.order_index` (unique `(testcase_id, order_index)`), `instance_name` (constructor/static product or Composition receiver). No per-testcase weight. Challenge `testcase_weight` is unchanged.
- Plagiarism (operator SQL `docs/sql/2026-08-19-plagiarism.sql`): after upload, compare this student to other students in the same lab — git commit hashes in order (100%), git metadata (100%), `.java`/`.mmd` SHA-256 Jaccard `> 90%`. A content match is flagged only if the uploader's **prior** lab best (excluding current attempt) is strictly below the other student's **lab best** and the current attempt scores **> 0** (first-time copy to 100 still flags; already-proven ≥ peer best does not; zero-score uploads never flag). Only the uploader's **latest** attempt stays active — a later original submit clears older copy flags. Signals snapshot on the upload thread; inspect (one attempts query, persist only content matches, re-evaluate matches where this uploader is the other side) runs on `persistExecutor`. Lecturer flags typically appear within ~1–3s. Missing `.git` skips git/metadata only. Lecturer UI roles: earlier **first** submit in the lab → `ORIGINAL` (victim), later first submit → `PLAGIARIZER` (never both; re-uploads do not invert roles). Exposed on roster rows, `GET /api/lecturer/plagiarism/flags` → `rolesByStudentAndLab`, and `GET /api/lecturer/labs/{labId}/students/{studentId}/plagiarism` (lineage).
- `Lab.deadline_date` (optional `DATE`) — end 23:59:59 Vietnam time; lecturer score SQL uses qualifying submissions on or before cutoff; extend deadline to backfill from history
- `Lab.student_visible` (default `true`) — when `false`, lab is hidden from student dashboard and submission APIs return 403
- `Lab.release_date` (optional `DATE`) — when set, students see the lab from 00:00 Vietnam time on that date (requires `student_visible=true`); operator SQL `docs/sql/2026-09-09-lab-student-visibility.sql`
- `lab_deadline_email_sent` — ledger for 72h/24h reminder emails to enrolled non-submitters (`LabDeadlineReminderScheduler`, minutely). Candidate selection is one anti-join (`findActiveStudentIdsForDeadlineEmail`); save-after-each-send stays for retry safety
- Soft-delete (inactive login): users set `isActive=false` via suspend or restore; inactive accounts cannot log in
- Lecturer **delete** (`DELETE /api/users/{id}`) permanently removes the user and bulk-deletes related submissions, grading result rows, enrollments, progress, plagiarism rows, deadline-email ledger entries, and password-reset tokens (no per-submission delete loop)
- Lecturer **suspend** (`POST /api/users/{id}/suspend`) is student-only `isActive=false`; restore via `POST /api/users/{id}/unsuspend`. Lecturer and dual-role accounts cannot be suspended this way.
- `term.is_current` — lecturer-selected current term; operator SQL `docs/sql/2026-08-19-term-current.sql`. Students in that term may submit; others only use history.

### Submission resolution

Student-facing challenge scores, Class tab, and stats **current grade** use the student's **latest attempt** for the lab (`LabSubmissionRepository.findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc`). `student_lab_progress.best_submission_id` and `highest_score` are still updated on upload but are not used for student dashboard display.

### Submission pipeline (summary)

Upload → `StudentTermAccessService.requireUploadAccess` (one query, cached 30s on success; warmed by `GET /api/labs`) → rubric cache load **overlaps** compile on `persistExecutor` → `SubmissionStorageService` (parallel in-memory compile per challenge via `compileExecutor`) → assign `lab_submission.id` in memory → `GradingService` compute + `lab_result` assemble → `UploadPersistService` one JDBC statement (insert submission `MAX+1` with final score, challenge-score UPSERT, progress UPSERT; snapshot file; detail UPSERT after that statement on `persistExecutor`) → compile/package/mmd sidecars on `persistExecutor` → snapshot plagiarism signals on the request thread, then `PlagiarismService.inspectUpload(submission, signals)` on `persistExecutor` (failures swallowed) → MMD hook (no-op by default) → cleanup temp folder on `persistExecutor`. Lecturer plagiarism flags typically appear within ~1–3s (live SQL; lab statistics cache invalidated again after inspect).

Class / MMD / Testcase GETs wait on `SubmissionDetailPersistGate` until that submission’s detail UPSERT finishes (or 60s). Challenge sidebar scores use stored `submission_challenge_result` when present.

Grading tuning properties (`application.properties`):

| Property | Default | Purpose |
|---|---|---|
| `app.grading.parallelism` | `4` | Max concurrent challenge workers during grading (capped at CPU count) |
| `app.compile.parallelism` | `4` | Max concurrent per-challenge compile workers during upload (capped at CPU count) |
| `app.grading.testcase-invoke-timeout-seconds` | `5` | Per-invocation timeout for operational testcases; tree-kills the worker JVM |
| `app.grading.worker-jar` | `/app/worker.jar` | Thin isolated worker JAR (`WORKER_JAR`). Local Maven: `target/backend-1.0.0-worker.jar`. Docker/Render image sets `/app/worker.jar`; if the configured path is missing, `WorkerProcessClient` falls back to `/app/worker.jar` then the local Maven path (so a Render env copied from local `target/...` still works). |
| `app.grading.worker-java` | `java` | Java binary used to spawn the worker (`WORKER_JAVA`) |
| `app.grading.sandbox.enabled` | `false` | Route testcase invoke/dry-run through remote `sandbox-runner` (`SANDBOX_ENABLED`) |
| `app.grading.sandbox.runner-url` | _(empty)_ | Runner base URL (`SANDBOX_RUNNER_URL`) |
| `app.grading.sandbox.runner-token` | _(empty)_ | Bearer token shared with runner (`SANDBOX_RUNNER_TOKEN`) |
| `workerJvmSlot` bean | `Semaphore(1)` | Host-wide isolated worker JVM; acquire/release on the HTTP thread in `TestcaseDryRunService` and `GradingService.gradeSubmission` when OT applies. Capacity stays 1. |
| `pillarExecutor` bean | `max(2, parallelism×2)` threads | MMD + testcase pillars inside each challenge; separate from `gradingExecutor` to avoid pool deadlock on 1–2 CPU hosts (Render) |
| `persistExecutor` bean | 2 threads (not CPU-capped) | Off-request detail UPSERT, rubric overlap, sidecars, plagiarism inspect, and temp-folder delete. Uncapped so 1-CPU Render can wait on Neon without blocking the other persist task. |
| `app.grading.rubric-cache-ttl-minutes` | `30` | In-process lab rubric cache TTL |
| `app.grading.timing-log` | `false` | Print aligned `[timing]` blocks (`utility/TimingLog`) for upload (`access`, `rubric`, `compile`, `grade`, `persist`, `plagiarism` = signal snapshot + schedule, `total`), off-thread `Plagiarism inspect`, compile, each challenge, grade submission, structure save, and read paths |
| `app.upload.access-cache-ttl-seconds` | `30` | Successful `requireUploadAccess` cache TTL. `GET /api/labs` warms it. Denials are not cached. `0` disables. |
| `app.master-data-cache-ttl-minutes` | `60` | In-process master data (scope/type labels) cache TTL |
| `app.analytics.lecturer-overview-cache-ttl-seconds` | `90` | TTL for `/api/lecturer/overview` in-process cache |
| `app.analytics.dashboard-cache-ttl-seconds` | `180` | TTL for `/api/analytics/dashboard` per filter set |
| `app.analytics.lab-statistics-cache-ttl-seconds` | `120` | TTL for `/api/labs/{id}/statistics`; invalidated on upload for that lab |

**Analytics caches:** In-process only. Multi-instance Render deploys see independent TTL staleness per instance. Lecturer overview and analytics dashboard may be stale up to configured TTL; lab statistics invalidate on the instance that handled the upload.

**Detail persist gate:** In-process `CompletableFuture` per submission. A Class-tab GET that hits a different instance than the upload may not wait; details should already be in Postgres if the UPSERT finished.

**Schema scripts:** Operator-run SQL in `docs/sql/` (e.g. `docs/sql/2026-08-07-analytics-indexes.sql`). OT rebuild: apply `docs/sql/2026-09-23-operational-testcase-unit-composition.sql` on a copy first, then restart the API so `TestcaseSchemaMigrator` + `LabRubricCache.invalidateAll()` drop stale rubric graphs.

### Read-path performance

- `SubmissionResultLoader` — single JOIN FETCH load of correct field/method/constructor IDs per submission
- `MasterDataCache` — cached scope/type labels; Class/MMD/Testcase **GET** tabs assemble from `LabRubricCache` + `buildClassDataFromRubric` / `buildMmdDataFromRubric` / challenge rubric by id (same mappers as upload `lab_result`); `loadChallengeStructures` remains for lecturer structure GET; student-facing assembly uses `DisclosureMode.STUDENT` (generic placeholders when snapshot missing); lecturer drawer passes `DisclosureMode.LECTURER`
- `ChallengeService` — sidebar scores from stored `submission_challenge_result` when present; otherwise recompute from element results
- `LabStructureService.saveLabStructure` — prefetches the full lab tree once (`SaveContext`: challenges, classes, fields/methods/constructors, relations, master data), syncs from in-memory maps (no per-entity `findById`), batches `saveAll` per challenge for classes/members/relations (parameters bulk-deleted/reinserted per challenge), prints a `[timing] Save lab structure` block when `app.grading.timing-log=true`, returns the request payload (no post-save full reload)
- Upload response `challengeResult` is `Map<UUID, Integer>` (scores only); class detail via `GET /challenges/{id}/class`
- `attemptsCount` on progress and upload `totalSubmissions` are the assigned attempt number (`MAX+1` inside the persist statement). Each upload inserts a **new** attempt after grade, with the final score. The path `{attemptNumber}` is not used to upsert.
- Per-challenge compile failures are stored in `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json` and shown on Class tab cards as one `CompileErrorMessage` line (convention: `service/compile/AGENTS.md`)
- Per-challenge package-normalization notices (when student sources include `package` declarations) are stored in `{SUBMISSION_BASE_DIR}/_package_normalization/{submissionId}.json` and shown as a non-blocking warning on the student Class tab
- Per-challenge MMD metadata (file presence, class-in-diagram, relation error labels) is stored in `{SUBMISSION_BASE_DIR}/_mmd_meta/{submissionId}.json` at upload; `ClassStructureService` infers MMD was submitted from persisted DB results when that file is missing (e.g. ephemeral storage wipe)
- Parsed submission display snapshots for Class/MMD tabs are stored in `{SUBMISSION_BASE_DIR}/_parsed_snapshot/{submissionId}.json` at grade time; class shells capture student scope/type/abstract/static plus declared superclass and interfaces. When missing (legacy submissions or storage wipe), class type labels fall back to rubric and shell checks are omitted. When the class shell fails, member rows are shown as fail even if individual attributes would match. A matching shell with no fields/constructors/methods is card status `success`, not `info`. Inheritance/realization pairs are graded on the Java shell independently of the MMD pillar.
- `GET /api/labs` — lab list (`deadlineDate`, `urgencyState`) scoped to the **current quarter** for lecturers and enrolled students; students also require `student_visible` + `release_date` (`LabDeadlineHelper.isOpenForStudentSubmission`); empty when no current quarter is set. Student JWT also receives per-lab `challenges` (names/ids/weights, scores omitted) plus `totalSubmissions` / `latestSubmission` in the same response so the dashboard does not wait on follow-up `/challenges` and `/stats` calls. Upload access is `UserAccountRepository.findUploadAccess` (user + lab + term + enrollment in one query).
- `GET /api/labs/{labId}/statistics` — lecturer lab analytics (scores, completion from active term enrollees, grade distribution, `plagiarismRate` = unique flagged students ÷ students submitted)
- `GET /api/labs/{labId}/submissions` — paginated roster of students who submitted for the lab (default page size 5); **score** is best qualifying submission before lab deadline (null when only late submissions); sort by `studentName` or `score`; optional `search` filters by name or student/teacher code (case-insensitive)
- `GET /api/labs/{labId}/submissions/export` — full submitter roster in one query (lecturer export); same score semantics and `sort` param
- `GET /api/labs/{labId}/students/{studentId}/attempts` — lab attempt history for lecturer roster View
- `GET /api/submissions/my-labs` — per-lab performance summary; optional `scope=current` (dashboard: current quarter only) or default `all` (history: every quarter, includes `termLabel`)
- `GET /api/submissions/my-history` — student's submission list + stats (optional `labId` filter; `page`, `size`, `sort` for pagination). Scope stats (`labsAttempted`, `totalSubmissions`, `averageScore` scale-2 DOWN, `bestScore`) come from one aggregate query
- `GET /api/labs/{labId}/challenges/{challengeId}/students` — paginated roster of students with a graded submission for that challenge (submitters only; **score** is highest qualifying challenge score before deadline; **attempts** / **submittedAt** from latest graded attempt; score from `submission_challenge_result` or computed from element results when legacy rows are missing)
- `TermEnrollmentSyncService` — on startup, backfills `term_enrollment` from existing `student_lab_progress` (idempotent)
- `GET /api/lecturer/overview` — lecturer dashboard overview cards scoped to the **current quarter** (enrolled active students, labs in that quarter, submissions for those labs); **at-risk count** uses the same total-score rule as grade overview (average of highest lab scores, missing labs as 0; threshold < 70); empty when no current quarter is set
- `GET /api/lecturer/grade-overview` — current-quarter grade matrix (paginated, default page size 10): **active students enrolled in the current term** × **labs in that term**; per-lab score from qualifying submissions before deadline; total = sum ÷ lab count in term; sort by `studentName`, `irn`, `score`, or `labScore,<labUuid>,<asc|desc>`; optional `search` filters by name or student/teacher code (case-insensitive); empty when no current term is set
- `GET /api/analytics/dashboard` — reports page analytics (returns 200 with empty/null fields when no data)

## Work Guidance

- Controllers stay thin; business logic belongs in `service/` or `grading/`
- Throw `SubmissionProcessingException` for upload/compile failures — handled by `GlobalExceptionHandler` (422)
- `LabService` exists but `LabController` calls `LabRepository` directly — follow existing pattern per endpoint
- New API endpoints need CORS coverage in `CorsConfig` if called from frontend; `allowedMethods` must include `PATCH` (lab deadline set/clear uses `PATCH /api/lecturer/labs/{labId}/deadline`)

## Verification

- `mvn test` from `backend/` and the Docker image build (`mvn -B test package`) run tests in `unit/`, `integration/`, `authorization/`, `regression/`, and `support/` under `backend/src/test/java/`.
- `@WebMvcTest` classes under `authorization/` declare a nested `@SpringBootApplication` on the test class so Boot can find configuration outside `com.eiu.capstone.backend`.
- Surefire sets `net.bytebuddy.experimental=true` so Mockito can run on a local JDK newer than 22; image builds use JDK 17.
- Manual: Swagger UI, `GET /`, submission upload from frontend `DropZone`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` | Reflection parser, isolated testcase worker, rubric comparison, scoring |
| `src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md` | Git / metadata / file-hash plagiarism checks |
| `src/main/java/com/eiu/capstone/backend/service/AGENTS.md` | Submission storage, Java compilation, auth, user services |
