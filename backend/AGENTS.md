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
- Practice assemble: `mvn -DskipTests package -Pdesktop-dist` also builds classifier `desktop` → `target/backend-1.0.0-desktop.jar` (excludes PostgreSQL driver, springdoc, mail, jjwt). Default `package` still produces the full `backend-1.0.0.jar` + worker. `scripts/assemble-student-desktop.ps1` copies the desktop classifier to `backend.jar`, builds the thin Rust WebView2 host via `cargo` (`desktop/launcher/practice-host/`), and jlinks `runtime/jdk` when available.
- Docker: multi-stage `Dockerfile`; copies `backend-1.0.0.jar` → `/app/app.jar` and `backend-1.0.0-worker.jar` → `/app/worker.jar` by name; optional build-arg `PRACTICE_BUNDLE_URL` → `/app/OOP-AutoGrader-Practice.zip`; API start is `exec java $JAVA_OPTS -jar app.jar` (default caps in Dockerfile — `-Xmx192m`, Metaspace `96m`, code cache `32m`, G1 periodic GC); worker stays `-Xmx64m` and does not inherit `JAVA_OPTS`; see `DEPLOY_RENDER.md`, `DEPLOY_RAILWAY.md`
- Operational testcase invoke runs in the thin worker JAR (one JVM per lecturer dry-run or per student upload when any challenge has OT; host slot of 1 on the HTTP thread). Class-tab parse prefers a one-shot worker `--reflect` (same `workerJvmSlot`, env allowlist) so student types stay out of API Metaspace; falls back to in-process `URLClassLoader` when the worker JAR is missing or reflect fails. Worker env is allowlisted; that is not a filesystem or `/proc` jail.
- **Requires a JDK** (not JRE) — `JavaCompilerService` uses `javax.tools.JavaCompiler`

### Environment

Copy `backend/.env.backend.example` to `backend/.env`. Key variables:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL — use Neon **pooler** hostname (`-pooler`) for production JVM; prefer `socketTimeout=30`. Hikari defaults are sleep-friendly for Railway Serverless: `minimum-idle=0`, `idle-timeout=10000`, `keepalive-time=0`, `tcpKeepAlive=false` (override via `HIKARI_*` for always-on warm pools). `maximum-pool-size` defaults to **5** (`HIKARI_MAXIMUM_POOL_SIZE`); `reWriteBatchedInserts=true` and `stringtype=unspecified` so VARCHAR-mapped OT enums cast under batched inserts (lab clone / sync). |
| `APP_DEADLINE_REMINDER_ENABLED` / `APP_DEADLINE_REMINDER_FIXED_RATE_MS` | Cloud `LabDeadlineReminderScheduler` (`!desktop`): default on, **hourly** (`3600000`). Was minutely (blocked Serverless). While the replica is asleep the job does not run until wake. |
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
| `JAVA_OPTS` | Docker/Render API JVM flags only (image default `-Xmx192m` plus Metaspace/code-cache/G1 periodic). Expanded by the Dockerfile entrypoint. Ignored by `mvn spring-boot:run`. Do not set `-Xmx512m` on 512MB hosts (worker needs headroom). |
| `DESKTOP_PACK_SIGNING_PRIVATE_KEY` | Optional Base64 PKCS#8 Ed25519 key for lecturer desktop practice pack export; pairs with `classpath:desktop-pack-public.key`. When unset, export returns 503. |
| `APP_BURST_MAX_REQUESTS` | Mutating burst max (POST/PUT/PATCH/DELETE outside `/api/auth`) per window (default `5` → `app.burst.max-requests`) |
| `APP_BURST_MAX_READS` | Read burst max (GET/HEAD) per window (default `30` → `app.burst.max-reads`). Higher than writes so one screen's GET fan-out (lecturer dashboard ~13) does not jail the user |
| `APP_BURST_MAX_AUTH_REQUESTS` | `/api/auth/**` burst max per window (default `30` → `app.burst.max-auth-requests`). Separate IP lane so a lab on shared Wi‑Fi can log in together without sharing the tight write budget |
| `APP_BURST_WINDOW_SECONDS` | Rolling window length in seconds (default `10` → `app.burst.window-seconds`) |
| `APP_BURST_RETRY_AFTER_SECONDS` | `Retry-After` / jail cool-down seconds (default `5` → `app.burst.retry-after-seconds`) |

Config files: `src/main/resources/application.yml` (imports `.env`; multipart `max-file-size` / `max-request-size` **50MB**), `application.properties` (datasource, storage path), `application-desktop.yml` (profile `desktop`: H2 file DB under `${APP_DESKTOP_HOME}/data`, bundled paths, Swagger off; same 50MB multipart ceiling). Oversized uploads → `MaxUploadSizeExceededException` → **413** via `GlobalExceptionHandler` (not opaque 500).

**Desktop profile (`spring.profiles.active=desktop`):** Local student practice API on port **18002** (`server.port` in `application-desktop.yml`, override `DESKTOP_SERVER_PORT`) — avoids colliding with dev/cloud **8002**. H2 (PostgreSQL compatibility mode), no JWT / OAuth / mail / Swagger beans (`DesktopSecurityConfig`; cloud-only `@Profile("!desktop")` on `JwtService`, `AuthController`, mail senders, `SwaggerConfig`, `BurstLimitFilter` / `BurstBudgetService`, etc.), no plagiarism pipeline (`DesktopPlagiarismService`), no production schema migrators or term-enrollment backfill. Set `APP_DESKTOP_HOME` to the install folder; static UI from `app.desktop.ui-dir` (default `{home}/ui/dist-desktop`); operational testcases spawn `worker.jar` via `app.grading.worker-java` (defaults to `java` on PATH; optional bundled `runtime/jdk` via `DESKTOP_WORKER_JAVA`). Desktop OT uses a larger local worker heap (`worker-xmx=512m`) and CPU-capped in-worker parallelism; the practice host starts the API with `-Xmx512m`. UI pack import (`DesktopPackStageService`) stages the `.agpack` then eagerly runs the same bootstrap materialization + fingerprint write so restart is fingerprint-cheap; failed materialize wipes practice data and clears the fingerprint. `application-desktop.yml` enables `spring.main.lazy-initialization` with mail autoconfigure excluded; `DesktopPackBootstrapper` is `@Lazy(false)` so `bootstrapComplete` still runs on `ApplicationReadyEvent`. Production cloud deploy keeps default profile (PostgreSQL + `SecurityConfig`).

### API surface

| Controller | Base path | Notes |
|---|---|---|
| `RootController` | `/` | `GET /` — liveness probe (Render health check); inactive on `desktop` profile (SPA served from `ui/dist-desktop`) |
| `AuthController` | `/api/auth` | Google login/upsert, IRN+password login, forgot/reset password. Unregistered Google users: 403 (frontend first-time setup). Inactive Google users: 423 (not setup). Inactive IRN login: 403. |
| `LabController` | `/api/labs` | `GET /list` — labs (with `deadlineDate`, `urgencyState`, natural name sort); lab stats, lecturer lab statistics/submissions |
| `LecturerRubricController` | `/api/lecturer/labs` | `POST /create` lab; structure read/save, delete, deep-clone (`GET /clone-sources`, `POST /clone`), `PATCH /{labId}/deadline`, `PATCH /{labId}/student-access`, `PATCH /{labId}/prefix`; `POST /{labId}/solution-import` (multipart compile→structure derive, no persist); challenge testcase CRUD + dry-run; `GET /{labId}/desktop-pack` → `Rubric_{labName}.agpack` |
| `LecturerBulkGradingController` | `/api/lecturer/labs` | `POST /{labId}/bulk-grade?mode=LAB\|EXAM` — ephemeral one-student multipart grade; returns score + `lab_result` (lecturer disclosure) + `fileHashes`; **no** submission/progress/plagiarism DB writes; Exam remaps flat files to `challenge_1` |
| `LecturerTermController` | `/api/lecturer/terms` | `GET /list`, `POST /create` term; set current term, delete non-current term (no labs), enroll/remove students, Excel import by IRN or email, `GET /{termId}/roster` (enrolled + available in one call), `GET`/`POST /{termId}/sync-labs` (copy labs from other quarters into selected quarter), `GET /{termId}/desktop-pack` → `Rubric_{yearLabel}_Q{n}.agpack` via `LabRubricCache.getAll` (one batched rubric load for cold misses; requires `DESKTOP_PACK_SIGNING_PRIVATE_KEY`) |
| `LecturerTermMailController` | `/api/lecturer/terms` | (`!desktop`) `POST /mail` — lecturer compose subject+body to each active enrolled student email in the **current** quarter via `TransactionalEmailSender` (SMTP/Brevo). Burst-exempt. |
| `LecturerAnalyticsController` | `/api/lecturer` | Overview, grade overview, `GET /plagiarism/flags`, `GET /labs/{labId}/plagiarism`, `GET /labs/{labId}/students/{studentId}/plagiarism` |
| `LecturerTabBootstrapController` | `/api/lecturer/bootstrap` | One-request first-useful-data GETs per nav tab (`/dashboard`, `/score`, `/ranking`, `/grading`, `/users`, `/quarters`, `/reports`, `/solution`); always-fresh (bypass multi-minute overview/dashboard TTL). `/users` returns the **full** account list (User Management client search). `/score` stays paged (12). `/grading` labs include sidebar `challenges` (required by BulkGradingPanel Lab/Exam mode). `/ranking` is the live weekly board |
| `LecturerRankingController` | `/api/lecturer/ranking` | Lecturer JWT. `GET /current` — live current-week board (same payload as bootstrap `/ranking`) |
| `StudentRankingController` | `/api/students/ranking` | Student JWT (`!desktop` not required; desktop has no UI). `GET /current` — live board for enrolled current-term students; out-of-term students get 200 with empty `rows` |
| `MasterDataController` | `/api/master-data` | `GET /by-category?category=` — master data lookup |
| `TermController` | `/api/terms` | `GET /list` — academic terms for lab creation |
| `StudentAccessController` | `/api/students` | `GET /term-access` (`inCurrentTerm`, `quarterName` = current quarter label when enrolled); `GET /daily-submission-quota` (`!desktop`) — persisted `lab_submission` count for the Vietnam calendar day, limit `app.submission.daily-max-per-student` (default 30), shared across Official and Practice labs; desktop profile returns unlimited; `StudentDesktopPracticeDownloadController` (`!desktop`) `GET /desktop-practice-bundle` — current-quarter students; streams fixed prebuilt zip `target/…` or `/app/OOP-AutoGrader-Practice.zip` (no `.agpack`, no runtime env; image packaging in `DEPLOY_RENDER.md`). SPA hands this GET to the browser via `?access_token=` (JWT filter accepts that query only on this path) so native download progress shows. |
| `StudentStreakController` | `/api/students/streak` | Student JWT (desktop: local student). `GET` marks today's login mission and returns streak, points, equipped mascot, and mission progress. `POST /pet` counts one fox pet (goal 4). `POST /restore` continues a broken streak once per calendar month. A successful upload counts one submit (goal 3). All three on the same Vietnam calendar day add 1 streak day and `50 + 5 * (day / 5)` points (day 5 → 55, day 6 → 55, day 101 → 150) |
| `StudentMascotController` | `/api/students/mascots` | Student JWT (desktop: local student). `GET /list` returns the shop (fox included, five priced mascots, owned, equipped, points). `POST /{id}/purchase` locks the streak row and, in that same transaction, subtracts the catalog price and inserts `student_mascot_purchase`, then equips the mascot. `POST /{id}/equip` wears an owned mascot or the starter fox. Insufficient points or a repeat purchase is HTTP 409 and does not change the balance |
| `AnalyticsController` | `/api/analytics` | Dashboard, lab trend, student overview/report |
| `UserController` | `/api/users` | `getAllUser` returns `UserResponse` list/page with `currentQuarter` (active + enrolled in system current term; blank otherwise; no DoB); `getUser/{id}`, `addUser`, `bulk`, `updateUser/{id}`, `deleteUser/{id}`; `POST /{id}/suspend` and `POST /{id}/unsuspend` for student-only accounts; **lecturer or Admin JWT** on all except self-service `POST /change-password`. Only Admin can grant, remove, or delete Admin. A lecturer save keeps an existing Admin role. Granting or removing Student, Lecturer, or Admin bumps `session_version`. Admin requires a lecturer IRN and does not require a student IRN |
| `SubmissionController` | `/api/submissions` | Upload + grade + student history reads (JWT required) |
| `PresenceController` | `/api/presence` | `GET /count` — public active-user count; a valid JWT records a heartbeat. `DELETE /leave` — signed-in leave (JWT required) |
| `FeedbackController` | `/api/feedback` | (`!desktop`) `POST /create` multipart (`type`, `message`, optional `files`) — signed-in student or lecturer Bug/Contribution; stamps IRN+name from `UserAccount`; PNG/JPG → WebP in `bytea`; at most 5 files and 25 MB total per upload. `GET /replies` — author’s lecturer replies for the notification bell. `DELETE /replies` — author consume (hard-delete own replied items by id; SPA fires on pagehide/logout) |
| `LecturerFeedbackController` | `/api/lecturer/feedback` | (`!desktop`) `GET /list`, `GET /{id}`, `GET /{id}/attachments/{attachmentId}`, `PATCH /{id}/status`, `PATCH /{id}/response` (Resolved, ≤500 chars, shown in student notifications), `DELETE /{id}` (Resolved only; soft-archives when a reply exists, else hard-deletes with attachments) |

Swagger UI: `http://localhost:8002/swagger-ui/index.html` (unauthenticated locally when `SPRINGDOC_ENABLED` is true; omit or set `false` in production)

### Security posture

- Default-deny Spring Security: `JwtAuthenticationFilter` is the only JWT parser and validates claim `sv` via `SessionValidityService` (missing user or version mismatch → unauthenticated); matcher table authorizes by path + method; anonymous → 401, authenticated without role → 403. Bearer `Authorization` is the default; `GET /api/students/desktop-practice-bundle` may also authenticate via `access_token` query (browser-owned download only — ignored on every other path)
- Global API burst guard (`BurstLimitFilter`, `@Profile("!desktop")`, after JWT): separate rolling windows in `app.burst.window-seconds` — mutating methods over `app.burst.max-requests` (default 5), GET/HEAD over `app.burst.max-reads` (default 30), `/api/auth/**` over `app.burst.max-auth-requests` (default 30) → HTTP 429 + `Retry-After` (`app.burst.retry-after-seconds`). Reads, writes, and auth do not share counters: dashboard GET fan-out does not spend uploads, and a classroom login burst on one NAT/Wi‑Fi IP does not spend the write budget. Authenticated key = JWT email; anonymous and all `/api/auth/**` = client IP. Same non-auth budgets for all roles. Not counted: `GET /api/presence/count`, `DELETE /api/presence/leave`, `OPTIONS /**`, `GET /`, swagger/OpenAPI when springdoc is on; intentional bulk POSTs listed in `BurstLimitExemptions` (lecturer `.../bulk-grade`, `.../solution-import`, `.../testcases/dry-run`, `POST /api/lecturer/labs/clone`, `POST /api/lecturer/terms/{id}/students/import`, `POST /api/lecturer/terms/mail`, `POST /api/users/bulk`) — still JWT-gated, only the burst counter is skipped. In-process per instance (no Redis). Desktop profile has no filter bean. Proxy: `server.forward-headers-strategy=framework` so anonymous IP keying sees the real client behind Render
- No role hierarchy. `TEACHER` in a token maps to `LECTURER`. `ADMIN` is its own authority and is not an alias of `LECTURER`. Dual-role accounts need both `STUDENT` and `LECTURER` authorities
- **Lecturer or Admin:** `/api/users/**` except `POST /api/users/change-password`, `/api/lecturer/**`, `/api/analytics/**`, `/api/master-data/**`, `/api/terms/**`, lecturer lab statistics/submissions/export/attempts and challenge student roster under `/api/labs`
- **Student, lecturer, or Admin:** `POST /api/users/change-password`, `/api/labs/**` after the lecturer-specific lab rows, challenge reads, lab list/stats, `/api/feedback/**`
- **Student or Admin:** `/api/submissions/**`, `/api/students/**`
- **Public:** `OPTIONS /**`, `GET /`, `GET /api/presence/count`, `/api/auth/**`, swagger/OpenAPI when springdoc is enabled
- `JwtAuthHelper` is identity only (`requireActiveUser`, `resolveStudentScope`, `resolveDisclosureMode`, `isStudentOnly`) — not authorization
- `JwtService` derives the HS256 signing key once at construction from `jwt.secret` (`JWT_SECRET`); missing, blank, or shorter-than-32-byte values fail startup (no random per-restart key)
- `UserAccount.passwordHash` omitted from JSON (`@JsonIgnore`)
- Google auth enforces `@eiu.edu.vn` domain and configured `GOOGLE_CLIENT_ID` audience via `GoogleTokenVerifier`
- Password-reset request does not reveal whether an email exists (anti-enumeration)
- JWT routes re-check active account via `requireActiveUser`

### Persistence

- JPA entities in `model/`, repositories in `repository/`
- Schema managed externally — no Flyway/Liquibase migrations in repo; startup migrators (`SessionVersionSchemaMigrator`, `DesktopPackVersionSchemaMigrator`, `TestcaseSchemaMigrator`, …) patch older PostgreSQL when needed
- Rubric chain: `Lab` → `Challenge` → `ClassEntity` → `Field`/`Method`/`Constructor`; `ClassRelation` (MMD source→target + `RELATION_TYPE` master data) per challenge. `field_declaration` carries `is_static` and `is_final` (default false). Enum constant fields may omit `field_declaration.data_type` and `scope` (`FieldDeclarationNullabilitySchemaMigrator` drops NOT NULL on older databases)
- Scoring weights (int, min 1, default 1): `challenge.weight`, `challenge.class_weight`, `challenge.mmd_weight`, `challenge.testcase_weight`, `class_entity.weight` — operator SQL `docs/sql/2026-08-19-scoring-weights.sql` and `docs/sql/2026-08-22-testcase-weight.sql`. Labs have no weight. Native lecturer SQL must use `CAST(l.deadline_date AS timestamp)`, not `::timestamp` (Hibernate treats `:` as a parameter).
- Operational testcase persistence (operator SQL `docs/sql/2026-09-23-operational-testcase-unit-composition.sql`; also applied on startup by `TestcaseSchemaMigrator` when leftover types/columns remain): wipe CASCADE of OT graphs, drop `testcase_instance` / `oop_principle_tag` / per-testcase `weight` / `COMPARISON_RESULT`, rewrite `testcase_type` to `UNIT` | `COMPOSITION`. After wipe the migrator calls `LabRubricCache.invalidateAll()`. Kept columns: `testcase_invocation.order_index` (unique `(testcase_id, order_index)`), `instance_name` (constructor/static product or Composition receiver). No per-testcase weight. Challenge `testcase_weight` is unchanged.
- Plagiarism (operator SQL `docs/sql/2026-08-19-plagiarism.sql`): after upload, compare this student to other students in the same lab — git commit hashes in order (100%), git metadata (100%), `.java`/`.mmd` SHA-256 Jaccard `> 90%`. A content match is flagged only if the uploader's **prior** lab best (excluding current attempt) is strictly below the other student's **lab best** and the current attempt scores **> 0** (first-time copy to 100 still flags; already-proven ≥ peer best does not; zero-score uploads never flag). Only the uploader's **latest** attempt stays active — a later original submit clears older copy flags. Signals snapshot on the upload thread; inspect on `persistExecutor` uses two short Neon TXs around in-memory compare (prepare/load, release connection, compare, write matches + peer-side re-eval; one attempts query in prepare). Lecturer flags typically appear within ~1–3s. Missing `.git` skips git/metadata only. Lecturer UI roles: earlier **first** submit in the lab → `ORIGINAL` (victim), later first submit → `PLAGIARIZER` (never both; re-uploads do not invert roles). Exposed on roster rows, `GET /api/lecturer/plagiarism/flags` → `rolesByStudentAndLab`, and `GET /api/lecturer/labs/{labId}/students/{studentId}/plagiarism` (lineage).
- `Lab.deadline_date` (optional `DATE`) — end 23:59:59 Vietnam time; lecturer score SQL uses qualifying submissions on or before cutoff; extend deadline to backfill from history
- `Lab.prefix` (`VARCHAR(16) NOT NULL DEFAULT 'Official'`) — `Official`, `Practice`, or `Midterm`. The entity column definition carries that default so desktop H2 schema update backfills existing rows. New and cloned labs start `Official`. Lecturer changes it with `PATCH /api/lecturer/labs/{labId}/prefix` (`UpdateLabPrefixRequest`); unknown values are 400. Changing the prefix does not regrade submissions. **Midterm** student uploads use flat `IRN_Name/*.java` and `*.mmd` (no `challenge_n`); `SubmissionUploadPathService` remaps into the lab's single challenge before `processUpload`. Score Total averages Official labs only; Practice and Midterm stay column-only. Operator SQL `docs/sql/2026-10-21-lab-prefix.sql`; `LabPrefixSchemaMigrator` (`@Profile("!desktop")`) applies the same `ADD COLUMN IF NOT EXISTS`. Desktop pack import writes the pack prefix onto the lab row and stores `Official` when the pack omits it.
- Display timestamps (`submitted_at`, etc.): format via `TimeUtil.formatDisplayTimestamp` / `formatLatestSubmission` (always `Asia/Ho_Chi_Minh`). Do not format raw `OffsetDateTime` wall clocks or `ZoneId.systemDefault()` — cloud JVMs are UTC and would show the wrong local time
- `Lab.student_visible` (default `true`) — when `false`, lab is hidden from student dashboard and submission APIs return 403
- `Lab.release_date` (optional `DATE`, legacy column) — not used for student gating or lecturer UI; student access is `student_visible` only (`LabDeadlineHelper.isOpenForStudentSubmission`)
- `lab_deadline_email_sent` — ledger for 72h/24h reminder emails to enrolled non-submitters (`LabDeadlineReminderScheduler`, default hourly via `app.deadline-reminder.fixed-rate-ms`). Candidate selection is one anti-join (`findActiveStudentIdsForDeadlineEmail`); save-after-each-send stays for retry safety
- `student_daily_streak` — one row per student (`user_id` PK, `ON DELETE CASCADE`). Daily missions reset on the Vietnam calendar date. `equipped_mascot` is the header mascot (`fox` until a purchase). Operator SQL `docs/sql/2026-10-20-student-daily-streak.sql` and `docs/sql/2026-10-20-student-mascot-shop.sql`; `StudentStreakSchemaMigrator` (`!desktop`) creates the table and shop columns on startup. Desktop profile creates them with `ddl-auto: update`
- `student_mascot_purchase` — one row per student and mascot (`user_id`, `mascot_id`). Written only inside the purchase transaction that deducts `student_daily_streak.points`
- `weekly_ranking_week` / `weekly_ranking_entry` — Sunday 23:59:59 Vietnam freeze ledger (`week_end` PK). Operator SQL `docs/sql/2026-10-21-weekly-ranking-payout.sql`; `WeeklyRankingSchemaMigrator` (`!desktop`) creates the tables on startup. `WeeklyRankingPayoutScheduler` (`cron 59 59 23 * * 0` Asia/Ho_Chi_Minh + ApplicationReady catch-up) pays unique ranks into `student_daily_streak.points` once per user per week (`ON CONFLICT DO NOTHING`). No REST payout. Desktop profile has no scheduler or migrator |
- Soft-delete (inactive login): users set `isActive=false` via suspend or restore; inactive accounts cannot log in
- Lecturer **delete** (`DELETE /api/users/deleteUser/{id}`) permanently removes the user and bulk-deletes related submissions, grading result rows, enrollments, progress, plagiarism rows, deadline-email ledger entries, and password-reset tokens (no per-submission delete loop)
- **Submission retention** (`!desktop`, `app.submission.retention.enabled`, default on cloud): keep every **on-time** `lab_submission` score row per student per lab. Full ticks stay on the **latest attempt before the deadline** **and** that lab’s **highest on-time attempt** (one full row when they coincide). Older on-time rows are stripped to score-only (Class/OT ticks, MMD JSON, disk sidecars). Late rows are always deleted. `student_lab_progress.highest_score` / `best_submission_id` stay the on-time best. Runs after each student upload, on **startup backfill**, and nightly (05:00 Vietnam). Desktop profile disables retention and backfill. Postgres file size may need operator `VACUUM` after large deletes (SQL Editor cannot run `VACUUM` inside a transaction — use `psql` on the direct host)
- Lecturer **suspend** (`POST /api/users/{id}/suspend`) is student-only `isActive=false`; restore via `POST /api/users/{id}/unsuspend`. Lecturer, Admin, and dual-role accounts cannot be suspended this way. An account with Admin is never student-only.
- `term.is_current` — lecturer-selected current term; operator SQL `docs/sql/2026-08-19-term-current.sql`. Students in that term may submit; others only use history.

### Submission resolution

Student-facing challenge scores, Class tab, and stats **current grade** use the student's **latest attempt** for the lab (`LabSubmissionRepository.findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc`). `student_lab_progress.best_submission_id` and `highest_score` update only for on-time uploads and are not used for student dashboard display.

### Submission pipeline (summary)

Upload → `StudentTermAccessService.requireUploadAccess` (one query, cached 10s on success; warmed by `GET /api/labs/list`) → rubric cache load **overlaps** compile on `rubricOverlapExecutor` → `SubmissionStorageService` (parallel in-memory compile per challenge via `compileExecutor`) → assign `lab_submission.id` in memory → `GradingService` compute + `lab_result` assemble → `UploadPersistService` one JDBC statement (insert submission `MAX+1` with final score, challenge-score UPSERT, progress UPSERT; snapshot file; detail UPSERT after that statement on `persistExecutor`) → compile/package/mmd sidecars on `persistExecutor` → snapshot plagiarism signals on the request thread, then `PlagiarismService.inspectUpload(submission, signals)` on `persistExecutor` (failures swallowed) → MMD hook (no-op by default) → cleanup temp folder on `persistExecutor`. Lecturer plagiarism flags typically appear within ~1–3s (live SQL; lab statistics cache invalidated again after inspect).

Class / MMD / Testcase GETs wait on `SubmissionDetailPersistGate` until that submission’s detail UPSERT finishes (or 60s). Challenge sidebar scores use stored `submission_challenge_result` when present.

Grading tuning properties (`application.properties`):

| Property | Default | Purpose |
|---|---|---|
| `app.grading.parallelism` | `2` (`APP_GRADING_PARALLELISM`) | Max concurrent challenge workers during grading (capped at CPU count) |
| `app.compile.parallelism` | `2` (`APP_COMPILE_PARALLELISM`) | Max concurrent per-challenge compile workers during upload (capped at CPU count) |
| `app.grading.testcase-invoke-timeout-seconds` | `5` | Per-invocation timeout for operational testcases; tree-kills the worker JVM |
| `app.grading.worker-jar` | `/app/worker.jar` | Thin isolated worker JAR (`WORKER_JAR`). Local Maven: `target/backend-1.0.0-worker.jar`. Docker/Render image sets `/app/worker.jar`; if the configured path is missing, `WorkerProcessClient` falls back to `/app/worker.jar` then the local Maven path (so a Render env copied from local `target/...` still works). |
| `app.grading.worker-java` | `java` | Java binary used to spawn the worker (`WORKER_JAVA`) |
| `app.grading.worker-idle-ttl-ms` | `0` | Local worker park TTL; `0` closes on release (no warm reuse). Positive values reuse one idle handle. |
| `app.grading.dry-run-compile-idle-ttl-ms` | `5000` | Dry-run compiled classes dir reuse TTL |
| `app.grading.sandbox.enabled` | `false` | Route testcase invoke/dry-run through remote `sandbox-runner` (`SANDBOX_ENABLED`) |
| `app.grading.sandbox.runner-url` | _(empty)_ | Runner base URL (`SANDBOX_RUNNER_URL`) |
| `app.grading.sandbox.runner-token` | _(empty)_ | Bearer token shared with runner (`SANDBOX_RUNNER_TOKEN`) |
| `workerJvmSlot` bean | `Semaphore(1)` | Host-wide isolated worker JVM; acquire/release on the HTTP thread in `TestcaseDryRunService` and `GradingService.gradeSubmission` when OT applies. Capacity stays 1. |
| Pillar order (upload) | class → MMD → OT per challenge | OT challenges: all class+MMD on `gradingExecutor` first, then one worker JVM; pillars are sequential (no MMD∥OT) to spare CPU/RAM on small hosts |
| `rubricOverlapExecutor` bean | 2 threads (not CPU-capped) | Upload-only: `labRubricCache.get` overlapping compile. Isolated so `rubricFuture.join()` never queues behind inspect/detail/sidecars. |
| `persistExecutor` bean | 2 threads (not CPU-capped) | Off-request detail UPSERT, sidecars, plagiarism inspect, and temp-folder delete. Uncapped so 1-CPU Render can wait on Neon without blocking the other persist task. |
| `app.grading.rubric-cache-ttl-minutes` | `2` | In-process lab rubric cache TTL |
| `app.grading.rubric-cache-max-size` | `8` | Hard cap on rubric cache lab entries; swept with other TTL caches |
| `app.grading.timing-log` | `false` | Print aligned `[timing]` blocks (`utility/TimingLog`) for upload (`access`, `rubric`, `compile`, `grade`, `persist`, `plagiarism` = signal snapshot + schedule, `total`), off-thread `Plagiarism inspect`, compile, each challenge, grade submission, structure save, and read paths |
| `app.upload.access-cache-ttl-seconds` | `10` | Successful `requireUploadAccess` cache TTL. `GET /api/labs/list` warms it. Denials are not cached. `0` disables. |
| `app.upload.access-cache-max-size` | `256` | Hard cap on upload-access success cache entries |
| `app.master-data-cache-ttl-minutes` | `5` | In-process master data (scope/type labels) cache TTL |
| `app.master-data-cache-max-size` | `1` | Hard cap on master-data cache entries (single key) |
| `app.analytics.lecturer-overview-cache-ttl-seconds` | `30` | TTL for `/api/lecturer/overview` in-process cache (bound by `@Value`; bootstrap `/api/lecturer/bootstrap/dashboard` bypasses) |
| `app.analytics.dashboard-cache-ttl-seconds` | `60` | TTL for `/api/analytics/dashboard` per filter set (bootstrap `/reports` bypasses) |
| `app.analytics.dashboard-cache-max-size` | `32` | Hard cap on dashboard filter-set entries (incl. free-text `course`) |
| `app.analytics.lab-statistics-cache-ttl-seconds` | `30` | TTL for `/api/labs/{id}/statistics`; invalidated on upload for that lab |
| `app.analytics.lab-statistics-cache-max-size` | `64` | Hard cap on lab-statistics cache entries |
| `app.cache.sweep-interval-ms` | `30000` | Proactive sweep of expired in-process TTL cache entries (`InProcessTtlCacheSweeper`, includes `LabRubricCache`) |
| `app.submission.daily-max-per-student` | `30` | Web/cloud only (`!desktop`): max persisted uploads per student per Vietnam calendar day (all labs); HTTP **429** + `X-Daily-Submission-Limit: 1` when exceeded; Admin test uploads unchanged (discarded before persist) |
| `app.submission.retention.enabled` | `true` (cloud); `false` on `desktop` profile | Strip extra on-time details and delete late rows after upload + nightly job |
| `app.submission.retention.max-per-lab` | `1` | Nightly finder: extra on-time rows beyond the latest attempt that still have ticks (on-time highest keeps ticks in Java) |
| `app.submission.retention.max-per-student` | `1` | Legacy alias for `max-per-lab` when the new key is unset |
| `app.submission.retention.nightly-batch-pairs` | `200` | Max students processed per nightly retention run |
| `app.submission.retention.backfill-on-startup` | `true` (cloud); `false` on `desktop` | Async full trim of existing over-cap pairs after `ApplicationReady` |

**Analytics caches:** In-process only, expire-after-write with hard max size and a scheduled sweep (`InProcessTtlCacheSweeper`) so expired keys leave without a same-key re-hit. Multi-instance Render deploys see independent TTL staleness per instance. Lecturer overview and analytics dashboard may be stale up to configured TTL; lab statistics invalidate on the instance that handled the upload.

**Detail persist gate:** In-process `CompletableFuture` per submission. A Class-tab GET that hits a different instance than the upload may not wait; details should already be in Postgres if the UPSERT finished.

**Schema scripts:** Operator-run SQL in `docs/sql/` (e.g. `docs/sql/2026-08-07-analytics-indexes.sql`). OT rebuild: apply `docs/sql/2026-09-23-operational-testcase-unit-composition.sql` on a copy first, then restart the API so `TestcaseSchemaMigrator` + `LabRubricCache.invalidateAll()` drop stale rubric graphs. Feedback inbox: apply `docs/sql/2026-10-20-feedback-inbox.sql` (tables `feedback_item`, `feedback_attachment`) on Neon and local Postgres before create/list. Lecturer reply columns: `docs/sql/2026-10-20-feedback-lecturer-response.sql` if the tables already exist. Soft-archive column: `docs/sql/2026-10-20-feedback-soft-archive.sql` (`archived_at`; delete with a reply hides from the inbox and keeps author replies). MMD revisit outcomes: `docs/sql/2026-10-20-scr-mmd-outcomes.sql` (`submission_challenge_result.mmd_outcomes`; also applied by `MmdOutcomesSchemaMigrator` on cloud startup).

### Read-path performance

- `SubmissionResultLoader` — single JOIN FETCH load of correct field/method/constructor IDs per submission
- `MasterDataCache` — cached scope/type labels; Class/MMD/Testcase **GET** tabs assemble from `LabRubricCache` + `buildClassDataFromRubric` / `buildMmdDataFromRubric` / challenge rubric by id (same mappers as upload `lab_result`); `loadChallengeStructures` remains for lecturer structure GET; student-facing assembly uses `DisclosureMode.STUDENT` (wrong/missing members use generic placeholders; correct Class members with a missing parsed snapshot fall back to rubric labels, same as MMD); lecturer drawer passes `DisclosureMode.LECTURER`
- `ChallengeService` — sidebar scores from stored `submission_challenge_result` when present; otherwise recompute from element results
- `LabStructureService.saveLabStructure` / insert-only clone — prefetches the full lab tree once (`SaveContext`: challenges, classes, fields/methods/constructors, params, relations, master data), syncs from in-memory maps (no per-entity `findById`; relation deletes use `SaveContext` not per-class SELECTs), skips unchanged challenge trees on light save, uses **phased flushes** under deferral (class shells with `outer_class` null → members → outer UPDATEs → relations) so Hibernate can JDBC-batch instead of falling back to one-statement-per-RTT on circular `outer_class` / relation graphs, accepts optional `replacedChallengeIds` on `LabStructureSaveRequest` for in-TX OT wipe + replace-aware rebuild (removed fields, methods, constructors, and relations also drop their `submission_*_result` rows; `lab_submission` and `submission_challenge_result` stay), prints `[timing] Save lab structure` / `(insert-only)` when `app.grading.timing-log=true`, returns the request payload (no post-save full reload)
- `LabStructureService.loadForEditor` — `findByIdWithTerm` + challenges/classes, then `LabStructureGraphLoader` fans out member/relation/parameter SELECTs on short `REQUIRES_NEW` reads via `lecturerBootstrapExecutor` (wall-clock ≈ slowest query, not sum of Neon RTTs); timing block `Load lab structure (editor)` when timing-log on
- Upload response `challengeResult` is `Map<UUID, Integer>` (scores only); class detail via `GET /challenges/{id}/class`
- `attemptsCount` on progress and upload `totalSubmissions` are the assigned attempt number (`MAX+1` inside the persist statement). Each upload inserts a **new** attempt after grade, with the final score. The path `{attemptNumber}` is not used to upsert.
- Per-challenge compile failures are stored in `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json` and shown on Class tab cards as one `CompileErrorMessage` line (convention: `service/compile/AGENTS.md`)
- Per-challenge package-normalization notices (when student sources include `package` declarations) are stored in `{SUBMISSION_BASE_DIR}/_package_normalization/{submissionId}.json` and shown as a non-blocking warning on the student Class tab
- Per-challenge MMD metadata (file presence, class-in-diagram, relation error labels) is stored in `{SUBMISSION_BASE_DIR}/_mmd_meta/{submissionId}.json` at upload; `ClassStructureService` infers MMD was submitted from persisted DB results when that file is missing (e.g. ephemeral storage wipe)
- Parsed submission display snapshots for Class/MMD tabs are stored in `{SUBMISSION_BASE_DIR}/_parsed_snapshot/{submissionId}.json` at grade time; class shells capture student scope/type/abstract/static plus declared superclass and interfaces. When missing (legacy submissions or storage wipe), class type labels and **correct** Class members fall back to rubric labels (wrong/missing members stay generic); shell checks are omitted. When the class shell fails, member rows are shown as fail even if individual attributes would match. A matching shell with no fields/constructors/methods is card status `success`, not `info`. Inheritance/realization pairs are graded on the Java shell independently of the MMD pillar.
- `GET /api/labs/list` — lab list (`deadlineDate`, `urgencyState`, `prefix` = `Official` or `Practice`) scoped to the **current quarter** for lecturers and enrolled students; students also require `student_visible` (`LabDeadlineHelper.isOpenForStudentSubmission`); empty when no current quarter is set. Student JWT also receives per-lab `challenges` (names/ids/weights, scores omitted) plus `totalSubmissions` / `latestSubmission` in the same response so the dashboard does not wait on follow-up `GET .../challenges/list` and `/stats` calls. Upload access is `UserAccountRepository.findUploadAccess` (user + lab + term + enrollment in one query).
- `GET /api/labs/{labId}/challenges/list` — challenge list for a lab (same payload as before when the path was the collection root).
- `GET /api/labs/{labId}/statistics` — lecturer lab analytics (scores, completion from active term enrollees, grade distribution, `plagiarismRate` = unique flagged students ÷ students submitted); `submissionCount` = `lab_submission` rows for the lab; `totalAttempts` = sum of each student's highest `lab_submission.attempt_number` for the lab (lifetime uploads; not the retained-row `attempts_count`)
- `GET /api/labs/{labId}/submissions` — paginated roster of students with an **on-time** lab score (default page size 5); **score** is stored `student_lab_progress.highest_score` (on-time best; does not disappear if extra attempts are pruned) with MAX of remaining on-time rows as fallback; **attempt** / **submittedAt** / View target the on-time best, not a later expired upload; sort by `studentName` or `score`; optional `search` filters by name or student/teacher code (case-insensitive)
- `GET /api/labs/{labId}/submissions/export` — full submitter roster in one query (lecturer export); same score semantics and `sort` param
- `GET /api/labs/{labId}/students/{studentId}/attempts` — lab attempt history for lecturer roster View
- `GET /api/submissions/my-labs` — per-lab performance summary; optional `scope=current` (dashboard: current quarter only) or default `all` (history: every quarter, includes `termLabel`)
- `GET /api/submissions/my-history` — student's submission list + stats (optional `labId` filter; `page`, `size`, `sort` for pagination). Scope stats (`labsAttempted`, `totalSubmissions`, `averageScore` scale-2 DOWN, `bestScore`) come from one aggregate query
- `GET /api/labs/{labId}/challenges/{challengeId}/students` — paginated roster of students with a graded submission for that challenge (submitters only; **score** is highest qualifying challenge score before deadline; **attempts** / **submittedAt** from latest graded attempt; score from `submission_challenge_result` or computed from element results when legacy rows are missing)
- `TermEnrollmentSyncService` — on startup, backfills `term_enrollment` from existing `student_lab_progress` (idempotent)
- `GET /api/lecturer/overview` — lecturer dashboard overview cards scoped to the **current quarter** (enrolled active students, labs in that quarter, submissions for those labs); **at-risk count** uses the same total-score rule as grade overview (average of highest lab scores, missing labs as 0; threshold < 70); payload also includes `students` (name, code, total, `atRisk`), `labs` (name, average, submitters), and `scoreRows` (best qualifying score per student per lab, the rows behind the average); card counts for students, labs, and at-risk match those lists; empty when no current quarter is set
- `GET /api/lecturer/grade-overview` — current-quarter grade matrix (paginated, default page size 12): **active students enrolled in the current term** × **labs in that term**; per-lab score from qualifying submissions before deadline (Practice and Midterm cells stay visible); total = sum of Official labs ÷ Official lab count (missing Official = 0; zero Official labs → null total); sort by `studentName`, `irn`, `score`, or `labScore,<labUuid>,<asc|desc>` uses that same Official total; optional `search` filters by name or student/teacher code (case-insensitive); empty when no current term is set. Dashboard average, at-risk, and reports still average every lab.
- `GET /api/analytics/dashboard` — reports page analytics (returns 200 with empty/null fields when no data); at-risk **labs** are one row per lab (not per challenge)
- `GET /api/lecturer/bootstrap/*` — tab first-useful-data aggregates for the SPA visit store (`dashboard`, `score`, `grading`, `users`, `quarters`, `reports`, `solution`); lecturer JWT; fresh (no overview/dashboard TTL)

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
- Desktop pack: `DesktopPackCryptoTest` and `DesktopPackSerializerTest` live in `src/test/java/com/eiu/capstone/backend/desktop/` (same package as the code they test); `unit` `DesktopPackExportServiceTest` (term export uses `LabRubricCache.getAll`), `DesktopMasterDataResolverTest` (restart-safe master_data id allocation), `LabRubricCacheTest` / `LabRubricServiceBatchLoadTest` (multi-lab batch load), `GlobalExceptionHandlerTest` (413 for oversize); `authorization` `LecturerTermDesktopPackTest`; `integration` `DesktopProfileContextTest`, `DesktopPackBootstrapIntegrationTest`, `DesktopPackImportIntegrationTest`, `DesktopPackMultipartSizeIntegrationTest` (RANDOM_PORT >1MB pack reaches validation), `DesktopSubmissionPipelineIntegrationTest` (`@ActiveProfiles("desktop")`)
- Desktop profile: `spring.profiles.active=desktop`, `APP_DESKTOP_HOME`, packs under `rubric/` (`Rubric_{year}_Q{n}.agpack`, `Rubric_{name}.agpack`). Pack lab meta includes `prefix`; a pack that omits it imports as `Official`. UI term import deletes lab packs on disk; lab name conflicts return 409 with `conflicts`; pack term ids are inserted via JDBC so `@GeneratedValue` cannot replace them; `DesktopMasterDataResolver` allocates new master_data ids past `max(existing)+1` so file-H2 restarts cannot merge-overwrite shared SCOPE/DECLARING_TYPE/RELATION_TYPE rows — see `docs/DESKTOP_STUDENT_DIST.md`

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` | Reflection parser, isolated testcase worker, rubric comparison, scoring |
| `src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md` | Git / metadata / file-hash plagiarism checks |
| `src/main/java/com/eiu/capstone/backend/service/AGENTS.md` | Submission storage, Java compilation, auth, user services |
