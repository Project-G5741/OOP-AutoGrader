# Services

## Purpose

Business logic layer: submission file handling, Java compilation, authentication, and user/lab management.

## Ownership

| Service | Responsibility |
|---|---|
| `SubmissionStorageService` | Upload pipeline: group files by challenge, write `.mmd`/`.java`, compile, return metadata |
| `JavaCompilerService` | Compile submitted `.java` files to `classes/` via `javax.tools.JavaCompiler` |
| `JwtService` | Create/parse JWTs (claims: email, name, domain, roles, irn) |
| `GoogleTokenVerifier` | Validate Google ID tokens; enforce verified email + allowed domain |
| `UserService` | CRUD, bulk create, Google upsert, IRN/password auth, role resolution, soft delete |
| `LabService` | Lab CRUD helpers (not used by `LabController` currently) |

## Local Contracts

### Submission folder layout

Per upload request (unique `requestId` prevents collisions):

```
<SUBMISSION_BASE_DIR>/<sanitized_irn>/<requestId>/challenge_<N>/
  mmd/           → uploaded .mmd files
  classes/       → compiled .class output
  _sources_tmp/  → temp .java sources (deleted after compile)
```

- Multipart filenames carry relative paths from the dropped folder (see `DropZone.jsx`)
- Challenge detection regex: `challenge[_-]?(\d+)` (case-insensitive)
- Only `.mmd` and `.java` files inside recognized challenge folders are processed
- `SubmissionStorageService.deleteFolder()` removes the entire request folder after grading

### Java compilation

- `JavaCompilerService.compile(sources, outputDir)` requires JDK (`ToolProvider.getSystemJavaCompiler()`)
- Compiler options: `-d <outputDir>`, `-encoding UTF-8`
- Failures throw `SubmissionProcessingException` with diagnostic messages
- Empty source list returns empty diagnostics (no-op)

### Authentication

- `GoogleTokenVerifier`: calls `https://oauth2.googleapis.com/tokeninfo`, checks audience, expiry, `email_verified`, domain `eiu.edu.vn`
- `JwtService`: signing key generated in-memory on startup — **not** loaded from `jwt.secret` in config
- `UserService.resolveRoles()`: maps DB roles to `STUDENT` / `LECTURER` strings

### User management

- Bulk create inserts rows with 1-second delay between each
- Soft delete sets `isActive=false`
- Google upsert creates or updates user on first login

## Work Guidance

- Submission pipeline changes must keep folder naming compatible with `GradingService` challenge regex
- Compile errors should surface via `SubmissionProcessingException` — controller returns these to the client
- Do not persist submission temp files beyond the upload request lifecycle
- Auth service changes affect both `AuthController` and `SubmissionController` JWT parsing

## Verification

- Compile path: upload `.java` files via frontend `DropZone`, confirm `classes/` populated before cleanup
- Auth: `POST /api/auth/google` and `POST /api/auth/login` via Swagger or frontend login

## Child DOX Index

No child docs.
