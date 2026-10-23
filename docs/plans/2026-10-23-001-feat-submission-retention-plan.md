---
title: "feat: Per-lab submission retention (10 attempts)"
date: 2026-10-23
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: user-dialogue
execution: code
---

# feat: Per-lab submission retention (10 attempts) - Plan

## Goal Capsule

**Objective:** Stop unbounded growth of submission result tables by retaining at most **10** graded attempts per **(student, lab)**. Prune older attempts automatically after each cloud upload and via a nightly batch job. Student history stats reflect **only retained rows** (no separate lifetime counters).

**Product authority:** User dialogue (2026-10-23) — decisions locked below.

**Open blockers:** None for implementation. Operator may run optional one-time batch sizing on Neon after deploy (documented; not required for code merge).

---

## Locked product decisions

| Topic | Decision |
|--------|----------|
| Scope | **10 newest** submissions per student **per lab** (`submitted_at` descending; tie-break `attempt_number` desc). |
| Extra keepers | **Per-lab highest** — keep the 10 newest attempts per student (all labs) **and** the highest-scoring `lab_submission` for each lab when that row is older. User reversed the v1 “strict 10 only” tradeoff on 2026-10-23. |
| Backfill | **Post-upload prune** + **nightly scheduler** for existing `(user_id, lab_id)` pairs with count &gt; 10. |
| History stats | **`my-history` stats** (`labsAttempted`, `totalSubmissions`, `averageScore`, `bestScore`) computed from **retained** `lab_submission` rows only (supersedes R7 “full scope” intent in `docs/plans/2026-08-19-001-feat-student-history-pagination-plan.md` for stats only; pagination behavior unchanged). |
| Desktop profile | **No retention** — `@Profile("!desktop")` on scheduler and upload hook (local H2 practice; small data). |

---

## Product Contract

### Summary

After each successful student upload on cloud PostgreSQL, delete submission rows (and all dependent grading/plagiarism rows) that fall outside the 10 newest for that student and lab. A nightly job catches drift and backfills historical data. `student_lab_progress` is **reconciled** from remaining submissions so dashboard fields do not reference deleted UUIDs.

### Problem Frame

Each upload inserts `lab_submission` plus thousands of `submission_*_result` rows (fields, methods, testcases, assertions, JSONB). Pagination reduced API payload; the database still grows linearly with every re-upload. Neon storage and write amplification scale with enrollments × attempts × rubric size.

### Actors

- **A1. Student** — uploads labs; views history (max ~10 rows per lab in DB); stats match visible history.
- **A2. Lecturer** — roster, grade matrix, attempt list, analytics read `lab_submission` aggregates; scores may reflect only retained attempts after prune.
- **A3. Operator** — optional tuning via env/properties; no required SQL migration for v1 (application-level deletes).

### Requirements

**Retention policy**

- **R1.** Configurable cap `app.submission.retention.max-per-lab` (default **10**). When `app.submission.retention.enabled=false`, no prune (local debugging).
- **R2.** For a given `(user_id, lab_id)`, **keepers** = the 10 rows with greatest `submitted_at` (then `attempt_number` desc). All other `lab_submission` rows for that pair are **eligible for delete**.
- **R3.** The submission just uploaded in the current request is never deleted by that request’s prune (it is newest by definition when prune runs after persist).
- **R4.** Prune deletes the full subgraph for each removed submission: plagiarism fingerprint/matches, testcase assertions/results, field/method/constructor/relation/challenge results, then `lab_submission` — same semantics as `UserService.purgeUserData` but scoped to submission IDs.
- **R5.** After prune for `(user_id, lab_id)`, **reconcile** `student_lab_progress`: `highest_score`, `best_submission_id`, `first_submitted_at`, `last_submitted_at`, `attempts_count` derived from **remaining** rows (`attempts_count` = count of retained rows, not lifetime `MAX(attempt_number)`).
- **R6.** Remove ephemeral disk sidecars for deleted submission IDs (`_compile_errors`, `_package_normalization`, `_mmd_meta`, `_parsed_snapshot` under `SUBMISSION_BASE_DIR`) when paths exist.

**Triggers**

- **R7.** **Post-upload:** schedule prune on `persistExecutor` after `SubmissionDetailPersistGate` completes for the new submission ID (do not delete before detail UPSERT finishes; do not block HTTP response).
- **R8.** **Nightly:** `@Scheduled` job (`Asia/Ho_Chi_Minh`, offset from student expiry e.g. **05:00**) processes batches of `(user_id, lab_id)` where submission count &gt; cap; bounded rows per run (`app.submission.retention.nightly-batch-pairs`, default e.g. **200**) to protect Neon.

**API / UX**

- **R9.** No new REST endpoints in v1.
- **R10.** Student `GET /api/submissions/my-history` pagination unchanged; `totalElements` cannot exceed retained count.
- **R11.** Lecturer attempt history and deep Class/MMD/Testcase GETs for pruned submission IDs return **404** (existing not-found behavior).

**Correctness caveats (documented, not “fixed” in v1)**

- **R12.** Lecturer qualifying score (`MAX(score)` before deadline over `lab_submission`) may drop when the best attempt is pruned — **by product choice**.
- **R13.** Plagiarism compares against remaining fingerprints only; deleting old attempts may remove evidence of past copies — acceptable with strict retention.
- **R14.** `attempt_number` on retained rows keeps original values (gaps allowed); UI may show “Attempt 47” with fewer than 47 stored rows until progress reconcile sets `attempts_count` to retained count (product: stats/history aligned to retained set).

### Acceptance examples

- **AE1.** Student with 11 attempts on Lab A: after upload #11, exactly **10** `lab_submission` rows remain for (student, Lab A); oldest deleted with all detail rows.
- **AE2.** Nightly job: pair with 25 attempts trimmed to 10 without a new upload.
- **AE3.** `my-history` stats `totalSubmissions` equals count of retained rows in scope (not lifetime 25).
- **AE4.** `student_lab_progress.best_submission_id` always references an existing row or null when no submissions left.
- **AE5.** Desktop profile upload: row count unrestricted (retention disabled).

---

## Planning Contract

### Approach

Introduce **`SubmissionRetentionService`** (orchestration + SQL for keeper selection) and **`SubmissionDeletionService`** (shared bulk delete by submission ID list, extracted/refactored from `UserService.purgeUserData` patterns). Wire post-upload scheduling from `SubmissionController` (or `UploadPersistService` callback) on `persistExecutor`. Add **`SubmissionRetentionScheduler`** mirroring `StudentAccountExpiryScheduler`.

Defer **FK `ON DELETE CASCADE`** operator SQL to a follow-up unless deletes prove too slow in practice — v1 uses explicit deletes like user purge.

### Patterns to follow

- Delete order: `backend/src/main/java/com/eiu/capstone/backend/service/UserService.java` → `purgeUserData`
- Per-submission plagiarism cleanup: `PlagiarismService` / `deleteInvolvingSubmission`
- Scheduler profile: `StudentAccountExpiryScheduler` (`!desktop`, cron + try/catch log)
- Post-upload async: `SubmissionController.schedulePlagiarismInspect` on `persistExecutor`
- Detail gate: `SubmissionDetailPersistGate.await(submissionId)`

### Out of scope (v1)

- Archiving pruned payloads to object storage
- Lecturer-configurable retention per lab
- Summary-only retention (strip detail rows but keep score row)
- Changing pagination page size or UI copy (optional doc note in `docs/USER_GUIDE.md` only if user requests later)
- `ce-work` execution until user approves this plan

---

## Implementation Units

### U1. Submission deletion primitive

**Goal:** One transactional method `deleteSubmissionsFully(Collection<UUID> submissionIds)` safe for 1..N IDs.

**Files:**

- New: `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionDeletionService.java`
- Touch: `backend/src/main/java/com/eiu/capstone/backend/service/UserService.java` (delegate purge path to shared service — minimal refactor)
- Repositories (add `@Modifying` native deletes by submission id list if missing): `SubmissionTestcaseResultRepository`, `SubmissionPlagiarismFingerprintRepository`, `SubmissionPlagiarismMatchRepository`, others as needed
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/SubmissionDeletionServiceTest.java` (H2 or `@DataJpaTest` with cascade tables)

**Approach:** For each id (or batched `IN` clauses): assertion results → testcase results → field/method/constructor/relation/challenge → plagiarism → `lab_submission`. Reuse existing `deleteBySubmission_Id` / `deleteInvolvingSubmission` where possible.

**Test scenarios:** Delete one submission with challenge + field rows leaves no orphans; delete empty list no-op; fingerprint and match rows removed.

### U2. Retention selection + progress reconcile

**Goal:** Given `(userId, labId)`, compute IDs to delete and reconcile progress.

**Files:**

- New: `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionRetentionService.java`
- `backend/src/main/java/com/eiu/capstone/backend/repository/LabSubmissionRepository.java` (native query: ids to delete using `ROW_NUMBER() OVER (PARTITION BY user_id, lab_id ORDER BY submitted_at DESC, attempt_number DESC)`)
- `backend/src/main/java/com/eiu/capstone/backend/repository/StudentLabProgressRepository.java` (update reconcile method or service-level JPQL)
- `backend/src/main/resources/application.properties` (+ `application-desktop.yml` note: property ignored on desktop)
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/SubmissionRetentionServiceTest.java`

**Approach:**

```text
keepers = top N by submitted_at for (user, lab)
delete = all ids for (user, lab) minus keepers
if delete non-empty: SubmissionDeletionService + reconcileProgress + sidecar file delete
```

Reconcile progress from `SELECT MAX(score), COUNT(*), MIN(submitted_at), MAX(submitted_at), argmax(score)` on remaining rows.

**Test scenarios:** 11 submissions → 10 remain, oldest id gone; progress `attempts_count=10`; strict cap with best score on deleted row → progress `highest_score` matches new MAX.

### U3. Post-upload scheduling

**Goal:** R7 without blocking upload response.

**Files:**

- `backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java`
- Optional: `backend/src/main/java/com/eiu/capstone/backend/grading/SubmissionDetailPersistGate.java` (await in retention task)

**Approach:** After successful persist (non-Admin), `CompletableFuture.runAsync` on `persistExecutor`: await detail gate for new id → `retentionService.enforceForLab(userId, labId)` → invalidate `labStatisticsCache` / `lecturerOverviewCache` if anything deleted.

**Test scenarios:** Integration-style test or controller unit with mocked retention verify scheduled once per upload.

### U4. Nightly batch scheduler

**Goal:** R8 backfill.

**Files:**

- New: `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionRetentionScheduler.java`
- `SubmissionRetentionService.enforceAllOverCap()` with pair batching

**Approach:** Query `(user_id, lab_id)` groups having `COUNT(*) > maxPerLab` LIMIT batch; foreach call `enforceForLab`. Log pruned pair count; swallow errors per pair like expiry job.

**Test scenarios:** Service test with seeded 12 rows → nightly method leaves 10.

### U5. Sidecar cleanup

**Goal:** R6.

**Files:**

- New small helper or methods on existing stores: `SubmissionCompileErrorStore`, `SubmissionPackageNormalizationStore`, `SubmissionMmdMetaStore`, `ParsedSubmissionSnapshotStore` — `deleteIfExists(UUID submissionId)`

**Test scenarios:** File removed when present; missing file no-op.

### U6. Docs and config contract

**Goal:** DOX pass for operators and agents.

**Files:**

- `backend/AGENTS.md` — retention summary, properties, lecturer score caveat
- `docs/HOW_IT_RUNS.md` — persistence subsection note
- `CONCEPTS.md` — optional one-line **Submission retention** process
- `docs/USER_GUIDE.md` — only if product wants student-facing “last 10 attempts stored” (defer unless requested)
- Root `AGENTS.md` Child index — link operator note if `docs/sql/` entry added later

**Test scenarios:** N/A (review only).

---

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `app.submission.retention.enabled` | `true` on cloud, `false` on desktop profile override | Master switch |
| `app.submission.retention.max-per-lab` | `10` | Keeper count |
| `app.submission.retention.nightly-batch-pairs` | `200` | Max (user, lab) pairs per nightly run |

---

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Long delete transactions on Neon | Delete in small ID batches; nightly cap; run after upload async |
| `persistExecutor` saturation | Retention task is light compared to detail UPSERT; monitor timing log |
| Lecturer grade drops | Documented R12; user accepted |
| Stale caches after nightly prune | Nightly job invalidates overview + affected lab statistics caches (or full invalidate if simpler) |
| Concurrent uploads same lab | Transaction per enforce; unique attempt insert already handles races; prune re-runs safely |

---

## Verification Contract

- `mvn test` from `backend/` — new retention/deletion tests pass; existing suite green.
- Manual cloud: 11 uploads same lab → DB count 10; history stats match.
- Manual: lecturer roster score may change when best attempt aged out — confirm expected.
- `npm run build` — no frontend change required for v1.

## Definition of Done

- Retention enforced post-upload and nightly on `!desktop` profile.
- No orphaned `submission_*` or plagiarism rows after prune.
- `student_lab_progress` consistent with retained submissions.
- AGENTS.md / HOW_IT_RUNS updated; plan decisions traceable in this file.

---

## Implementation handoff

**Do not start `ce-work` until the user explicitly approves implementation** (per user request 2026-10-23).

After approval, implement units **U1 → U2 → U3 → U4 → U5 → U6** in order; U1 blocks U2.
