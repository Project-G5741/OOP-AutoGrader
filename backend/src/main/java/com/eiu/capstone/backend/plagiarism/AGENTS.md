# Plagiarism

## Purpose

Detect copied lab submissions with three independent checks: ordered git history, git metadata, and file-byte hashes.

## Ownership

| File | Role |
|---|---|
| `GitHistoryReader.java` | Read `.git/logs/HEAD` (fallback `git log`) plus `.git/config` |
| `PlagiarismFingerprintExtractor.java` | Build signals from the upload multipart, including `.git/**` |
| `PlagiarismComparator.java` | Git 100% ordered-hash match; metadata 100% match; file SHA-256 Jaccard `> 0.90` |
| `PlagiarismService.java` | Persist fingerprint, compare other students in the same lab, score-gate flags, lecturer report / investigation |

## Local Contracts

- Flag when a content check fires **and** the uploader's **prior** lab best (excluding the current attempt) is **strictly lower** than the other student's **lab best**, and the current attempt scores **> 0**. A first-time copy that grades to 100 still flags (prior=0). Already-proven ability (prior ≥ peer best) does not. A 0-point upload never flags.
- Only the uploader's **latest** lab attempt can remain flagged. If B copied A, then later submits original code, older copy matches stop counting — B is no longer treated as plagiarizing
- After each upload inspect, re-evaluate match rows **for that lab only** (batched; not on lecturer flag reads — those must stay fast). Lecturer reads also ignore non-latest uploader matches even if `flagged` is still true in DB
- `inspectUpload` runs on the **upload HTTP thread** after grading (SHA-256 + pairwise compare vs every other fingerprint in the lab + *O(U)* best-score queries). Do not fail student upload if inspect throws
- Same-student attempts are not compared
- Missing `.git` skips git and metadata; hash still runs on `.java` / `.mmd`
- Lecturer-only: roster `plagiarismFlagged` + `plagiarismRole` (`ORIGINAL` = victim / earlier first submit in lab; `PLAGIARIZER` = later first submit — never both). Yellow warning = victim / lab presence; red = plagiarizer. `GET /api/lecturer/plagiarism/flags` includes `overlapByStudentAndLab` and `rolesByStudentAndLab`; `GET /api/lecturer/labs/{labId}/plagiarism`; `GET /api/lecturer/labs/{labId}/students/{studentId}/plagiarism` (lineage tree + match signals, no code diffs); lab statistics `plagiarismRate`
- Students are never notified of a plagiarism flag
- Schema: `docs/sql/2026-08-19-plagiarism.sql`

## Work Guidance

- Reconstruct `.git` to a temp dir; never run hooks; delete the temp tree after read
- Do not fail student upload if inspect throws
- Inspect compares against every other lab fingerprint (all prior attempts of other students), not latest-only

## Verification

- `PlagiarismComparatorTest`, `GitHistoryReaderTest`

## Child DOX Index

No child docs.
