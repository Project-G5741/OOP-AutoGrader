# Plagiarism

## Purpose

Detect copied lab submissions with three independent checks: ordered git history, git metadata, and file-byte hashes.

## Ownership

| File | Role |
|---|---|
| `GitHistoryReader.java` | Read `.git/logs/HEAD` (in-memory, or reconstructed dir + `git log` fallback) plus `.git/config` |
| `PlagiarismFingerprintExtractor.java` | Build signals from the upload multipart, including `.git/**` |
| `PlagiarismComparator.java` | Git 100% ordered-hash match; metadata 100% match; file SHA-256 Jaccard `> 0.90` |
| `PlagiarismService.java` | Persist fingerprint, compare other students in the same lab, score-gate flags, lecturer report / investigation |

## Local Contracts

- Flag when a content check fires **and** the uploader's **prior** lab best (excluding the current attempt) is **strictly lower** than the other student's **lab best**, and the current attempt scores **> 0**. A first-time copy that grades to 100 still flags (prior=0). Already-proven ability (prior ≥ peer best) does not. A 0-point upload never flags.
- Only the uploader's **latest** lab attempt can remain flagged. If B copied A, then later submits original code, older copy matches stop counting — B is no longer treated as plagiarizing
- After each upload inspect, re-evaluate match rows **where this uploader is the other side** (batched; not on lecturer flag reads — those must stay fast). Lecturer reads also ignore non-latest uploader matches even if `flagged` is still true in DB
- Snapshot `PlagiarismSignals` on the **upload HTTP thread** via `PlagiarismService.snapshotSignals` (SHA-256 of `.java`/`.mmd` plus in-memory git `config`+reflog). Compare/persist runs on **`persistExecutor` after persist**. Failures (snapshot or inspect) are swallowed; upload stays 200. Lecturer flags are live SQL and typically appear within ~1–3s (queued behind other persist-executor work). Lab statistics cache is invalidated again after inspect. JVM crash before inspect can omit flags for that attempt.
- Same-student attempts are not compared
- Missing `.git` skips git and metadata; hash still runs on `.java` / `.mmd`
- Lecturer-only: roster `plagiarismFlagged` + `plagiarismRole` (`ORIGINAL` = victim / earlier first submit in lab; `PLAGIARIZER` = later first submit — never both). Yellow warning = victim / lab presence; red = plagiarizer. `GET /api/lecturer/plagiarism/flags` includes `overlapByStudentAndLab` and `rolesByStudentAndLab`; `GET /api/lecturer/labs/{labId}/plagiarism`; `GET /api/lecturer/labs/{labId}/students/{studentId}/plagiarism` (lineage tree + match signals, no code diffs); lab statistics `plagiarismRate`
- Students are never notified of a plagiarism flag
- Schema: `docs/sql/2026-08-19-plagiarism.sql`

## Work Guidance

- Reconstruct `.git` to a temp dir only when `logs/HEAD` is missing; never run hooks; delete the temp tree after read
- Do not fail student upload if snapshot or inspect throws
- Inspect compares against every other lab fingerprint (all prior attempts of other students), not latest-only
- Persist a match row only when a content check fires (git, metadata, or hash Jaccard `> 0.90`); non-matches are omitted
- Score-gate promotion when the original author's lab best rises re-evaluates existing matches whose `other_submission_id` is this uploader
- Snapshot via `snapshotSignals` on the upload thread, then call `inspectUpload(submission, signals)` on the Spring bean from `persistExecutor` so `@Transactional` applies; do not read multipart off-thread

## Verification

- `PlagiarismComparatorTest`, `GitHistoryReaderTest`, `PlagiarismFingerprintExtractorTest`, `PlagiarismServiceInspectTest`

## Child DOX Index

No child docs.
