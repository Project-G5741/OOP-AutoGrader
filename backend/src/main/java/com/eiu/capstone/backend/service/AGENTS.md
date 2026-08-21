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
| `TermService` | Create terms by year, set current term, enroll/remove students |
| `StudentTermAccessService` | Current-term enrollment check; blocks submit when the student is inactive or out of term |
| `StudentHistoryService` | Student `my-history` / `my-labs` read APIs |
| `ChallengeService` | Challenge sidebar scores + per-submission breakdown (stored or recomputed from element results) |
| `ParsedSubmissionSnapshotStore` | Per-challenge parsed Class/MMD display snapshots (`_parsed_snapshot/`) for result tabs |

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

- `JavaCompilerService.compileSources(sources, outputDir)` compiles in-memory `JavaFileObject` sources to `classes/` via `javax.tools.JavaCompiler` (JDK required)
- Reuses one `JavaCompiler` instance and a per-thread `StandardJavaFileManager`
- Compiler options: `-d <outputDir>`, `-encoding UTF-8`
- Compile failures for a challenge folder are captured per challenge (upload continues); diagnostics appear on Class tab cards via `ClassDetailDTO.error`
- Empty source list returns without invoking the compiler
- With `app.grading.timing-log=true`, `SubmissionStorageService` prints a `[timing] Compile <challenge>` block (`build sources`, `javac`, `count`, `total`)

### Authentication

- `GoogleTokenVerifier`: calls `https://oauth2.googleapis.com/tokeninfo`, checks audience, expiry, `email_verified`, domain `eiu.edu.vn`
- `JwtService`: signing key generated in-memory on startup — **not** loaded from `jwt.secret` in config
- `UserService.authenticateByIrn()`: maps DB roles to `STUDENT` / `LECTURER` strings; inactive accounts are rejected
- `PasswordResetService`: `POST /api/auth/forgot-password` (email lookup, inactive rejected) and `POST /api/auth/reset-password` (opaque token in body; inactive users rejected at complete as well as request); tokens stored hashed in `password_reset_token` (see `docs/plans/sql/password_reset_token.sql`)

### User management

- Bulk create inserts rows with 1-second delay between each
- Soft delete sets `isActive=false`
- Lecturer suspend/restore (`suspendStudent` / `restoreStudent`) toggles `isActive` for student-only accounts; lecturer and dual-role accounts are rejected
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
- Set current term uses one bulk `UPDATE` (`clearOtherCurrent`) instead of loading every current row

## Work Guidance

- Submission pipeline changes must keep folder naming compatible with `GradingService` challenge regex
- Compile errors should surface via `SubmissionProcessingException` — `GlobalExceptionHandler` returns HTTP 422 with the message
- MMD-only challenge folders (no `.java`) still produce a `ChallengeResult` with `classFileCount=0` so grading records 0% for that challenge
- `processUpload` deletes the submission folder when any parallel challenge task fails
- Do not persist submission temp files beyond the upload request lifecycle
- Auth service changes affect both `AuthController` and `SubmissionController` JWT parsing

## Verification

- Compile path: upload `.java` files via frontend `DropZone`, confirm `classes/` populated before cleanup
- Auth: `POST /api/auth/google` and `POST /api/auth/login` via Swagger or frontend login
- Term access: `StudentTermAccessServiceTest` (inactive and out-of-term submit rejected)
- Term import: `TermServiceImportTest` (IRN+email match enrolls; email mismatch skipped)
- Term current membership: `TermServiceCurrentTermTest`
- User suspend: `UserServiceTest` (student inactive; lecturer/dual-role rejected)
- Password reset: `PasswordResetServiceTest` (inactive `completeReset` is 404 and does not write the hash)

## Child DOX Index

No child docs.
