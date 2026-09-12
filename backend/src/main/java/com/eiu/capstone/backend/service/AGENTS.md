# Services

## Purpose

Business logic layer: submission file handling, Java compilation, authentication, and user/lab management.

## Ownership

| Service | Responsibility |
|---|---|
| `SubmissionStorageService` | Upload pipeline: group files by challenge, parallel compile `.java`, return metadata |
| `MmdPersistenceHook` | Extension point for `.mmd` archival (default `NoOpMmdPersistenceHook`) |
| `JavaCompilerService` | Compile submitted `.java` files to `classes/` via `javax.tools.JavaCompiler` |
| `JwtService` | Create/parse JWTs (claims: email, name, domain, roles, irn) |
| `GoogleTokenVerifier` | Validate Google ID tokens; enforce verified email + allowed domain |
| `UserService` | CRUD, bulk create, Google upsert, IRN/password auth, role resolution, soft delete, student suspend/restore |
| `PasswordResetService` | Forgot-password token issuance (15m, single-use) and password reset completion |
| `PasswordResetEmailService` | Sends reset links via `TransactionalEmailSender` (`smtp` locally, `brevo` on Render free tier) |
| `LabService` | Lab CRUD helpers (not used by `LabController` currently) |
| `TermService` | Create terms by year, set current term, enroll/remove students, delete empty non-current quarters |
| `StudentAccountExpiryService` | Hard-deletes student-only accounts three quarters after first enrollment |
| `StudentAccountExpiryScheduler` | Daily purge job (`Asia/Ho_Chi_Minh`, 04:00) |
| `StudentTermAccessService` | Current-term enrollment check; blocks submit when the student is inactive or out of term |
| `PresenceService` | In-process last-seen map of signed-in emails; `GET /api/presence` heartbeats when a JWT is present; `DELETE /api/presence` removes that email; unique count within 30s |
| `StudentHistoryService` | Student `my-history` / `my-labs` read APIs |
| `SubmissionAttemptNumbers` | Next `lab_submission.attempt_number` (`MAX+1`; not the client path value) |
| `ChallengeService` | Challenge sidebar scores + per-submission breakdown (stored or recomputed from element results) |
| `ParsedSubmissionSnapshotStore` | Per-challenge parsed Class/MMD display snapshots (`_parsed_snapshot/`) for result tabs |
| `ClassStructureService` | Class / MMD / testcase tabs: GET and upload `lab_result` use `LabRubricCache` + `buildClassDataFromRubric` / `buildMmdDataFromRubric`; **`DisclosureMode.STUDENT`** redacts rubric fallbacks for student JWT and upload paths; **`DisclosureMode.LECTURER`** when lecturer passes `studentId`; class-shell display includes declared Extends/Implements via `HeritageShellMatcher` |

## Local Contracts

### Submission folder layout

Per upload request (unique `requestId` prevents collisions):

```
<SUBMISSION_BASE_DIR>/<sanitized_irn>/<requestId>/challenge_<N>/
  classes/       → compiled .class output (sources compiled from memory; no _sources_tmp)
```

- `.mmd` files are accepted in uploads but not written to disk on the hot path; use `MmdPersistenceHook` for near-future archival
- Challenges are compiled in parallel (`app.compile.parallelism`, default 4) on the dedicated `compileExecutor` pool

- Multipart filenames carry relative paths from the dropped folder (see `DropZone.jsx`)
- Challenge detection regex: `challenge[_-]?(\d+)` (case-insensitive)
- Only `.mmd` and `.java` files inside recognized challenge folders are compiled; `root/.git/**` is accepted for plagiarism and ignored by compile grouping
- Student Java sources with `package` declarations are normalized to the default package before compile (`StudentSourceNormalizer`); same-challenge cross-imports are stripped, JDK imports preserved
- `SubmissionStorageService.deleteFolder()` removes the entire request folder after grading

### Java compilation

- `JavaCompilerService.compileSources(sources, outputDir)` returns `CompileOutcome`: one group javac on the happy path; mixed failure may remainder-compile sources that have no ERROR diagnostic and are not attributed dependents into the same `classes/`
- Reuses one `JavaCompiler` instance and a per-thread `StandardJavaFileManager`
- Compiler options: `-d <outputDir>`, `-encoding UTF-8`
- Mixed javac does not throw. `SubmissionStorageService` keeps survivor `.class` files and records per-class diagnostics (`ChallengeCompileErrors`). I/O/setup still uses `failedChallenge`
- Compile diagnostics appear on Class tab cards via `ClassDetailDTO.error`. Lines follow the convention in `compile/AGENTS.md` (`CompileErrorMessage`).
- Empty source list returns without invoking the compiler
- Lecturer dry-run uses the same `CompileOutcome` + `CompileClassAttribution`; mixed reference compile is a preview DTO (`ERROR` if the testcase touches a failed type), not HTTP 422
- With `app.grading.timing-log=true`, `SubmissionStorageService` prints a `[timing] Compile <challenge>` block (`build sources`, `javac`, `count`, `total`)

### Authentication

- `GoogleTokenVerifier`: calls `https://oauth2.googleapis.com/tokeninfo`, checks audience, expiry, `email_verified`, domain `eiu.edu.vn`
- `JwtService`: HS256 signing key derived once at construction from `jwt.secret` / `JWT_SECRET` (≥32 bytes); missing or too-short secrets fail startup
- `UserService.authenticateByIrn()`: maps DB roles to `STUDENT` / `LECTURER` strings; inactive accounts are rejected
- `PasswordResetService`: `POST /api/auth/forgot-password` (email lookup, inactive rejected) and `POST /api/auth/reset-password` (opaque token in body; inactive users rejected at complete as well as request); tokens stored hashed in `password_reset_token` (see `docs/plans/sql/password_reset_token.sql`)

### User management

- Bulk create inserts rows with 1-second delay between each
- Hard delete (`deleteUser`) removes progress, enrollments, ledger, and tokens, then bulk-deletes plagiarism rows, grading result rows, and `lab_submission` for that user, then the `user_account` row
- Google upsert creates or updates user on first login
- Inactive users cannot log in (IRN or Google)
- Google inactive login returns HTTP 423 so the SPA does not treat it as first-time setup (unregistered remains 403)

### Terms

- Lecturers create a term under an academic year label (reused if it exists) and optional dates
- One term is current (`is_current`); set via `POST /api/lecturer/terms/{id}/current`
- Enroll only active students; out-of-term active students can still log in and read history, not submit
- Excel import matches **IRN (`student_code`) and email** to an existing user, then enrolls; extra columns ignored; unmatched rows are skipped
- Import, enroll, and term list use batched queries (user lookup by IRN list, enrollment ids, grouped student counts, `saveAll`)
- Current-term membership is `existsByUser_IdAndTerm_CurrentTrue` (no extra current-term fetch)
- `findCurrentTerm()` loads the current term row only (no academic-year join); year is fetched on term list
- `GET /{termId}/roster` loads enrolled + available students in one enrollment fetch plus `findActiveStudents`
- Set current term uses one bulk `UPDATE` (`clearOtherCurrent`) instead of loading every current row; student account expiry purge runs on the daily scheduler only (not when the current quarter changes)
- Term roster enrolled list includes only active students; available list is active students not yet enrolled
- `DELETE /api/lecturer/terms/{termId}` removes enrollments then the quarter; blocked when the quarter is current or still has labs
- Lecturer grade overview (`GET /api/lecturer/grade-overview`) scopes to the current quarter: active enrolled students and that quarter's labs only

## Work Guidance

- Submission pipeline changes must keep folder naming compatible with `GradingService` challenge regex
- Invalid upload structure / I/O setup failures may still use `SubmissionProcessingException` (`GlobalExceptionHandler` HTTP 422). Mixed javac keeps survivors and records `ChallengeCompileErrors`; lecturer dry-run mixed compile returns preview ERROR DTOs — do not throw HTTP 422 for javac diagnostics
- MMD-only challenge folders (no `.java`) still produce a `ChallengeResult` with `classFileCount=0` so grading records 0% for that challenge
- `processUpload` deletes the submission folder when any parallel challenge task fails
- Do not persist submission temp files beyond the upload request lifecycle
- Auth service changes affect both `AuthController` and `SubmissionController` JWT parsing

## Verification

- Compile path: upload `.java` files via frontend `DropZone`, confirm `classes/` populated before cleanup
- Auth: `POST /api/auth/google` and `POST /api/auth/login` via Swagger or frontend login
- Term access: `support` `StudentTermAccessServiceTest` (inactive and out-of-term submit rejected)
- Term import: `support` `TermServiceImportTest` (IRN+email match enrolls; email mismatch skipped)
- Term current membership: `support` `TermServiceCurrentTermTest`
- User suspend: `support` `UserServiceTest` (student inactive; lecturer/dual-role rejected; hard-delete bulk-purges grading rows)
- Password reset: `support` `PasswordResetServiceTest` (inactive `completeReset` is 404 and does not write the hash)
- JWT signing key: `authorization` `JwtServiceTest` (same secret verifies across re-init; missing/blank/short secrets fail at construction)
- Active users: `unit` `PresenceServiceTest` (unique email, expiry, leave); `authorization` `PresenceControllerTest` (public GET, JWT heartbeat, JWT leave)
- Class tab display: `support` `ClassStructureServiceShellDisplayTest` (JPA bundle and from-rubric snapshot shells, including Extends/Implements; `challengeById`)
- Compile-error convention: `support` `CompileErrorMessageTest`, `support` `CompileClassAttributionTest`
- Class/MMD disclosure: `support` `ClassStructureServiceDisclosureTest` (student mode redacts missing/wrong rubric labels; lecturer mode keeps them)
- History stats: `support` `StudentHistoryServiceTest` (one aggregate row for scope stats)
- Deadline email: `support` `LabDeadlineEmailServiceTest` (anti-join candidates, no per-student ledger exists)
- Structure save: `support` `LabStructureServiceSaveTest` (one inheritance/realization pair per source class)
- Structure save: `support` `LabStructureServiceSaveTest` (one inheritance/realization pair per source class)

## Child DOX Index

| Path | Scope |
|---|---|
| `compile/AGENTS.md` | In-memory javac, per-class attribution, Class-card compile-error convention |
