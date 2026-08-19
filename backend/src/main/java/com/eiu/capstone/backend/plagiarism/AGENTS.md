# Plagiarism

## Purpose

Detect copied lab submissions with three independent checks: ordered git history, git metadata, and file-byte hashes.

## Ownership

| File | Role |
|---|---|
| `GitHistoryReader.java` | Read `.git/logs/HEAD` (fallback `git log`) plus `.git/config` |
| `PlagiarismFingerprintExtractor.java` | Build signals from the upload multipart, including `.git/**` |
| `PlagiarismComparator.java` | Git 100% ordered-hash match; metadata 100% match; file SHA-256 Jaccard `> 0.90` |
| `PlagiarismService.java` | Persist fingerprint, compare other students in the same lab, lecturer report |

## Local Contracts

- Same-student attempts are not compared
- Missing `.git` skips git and metadata; hash still runs on `.java` / `.mmd`
- Flag if any check fires
- Lecturer-only: roster `plagiarismFlagged`, danger mark after flagged lab names, `GET /api/lecturer/plagiarism/flags`, `GET /api/lecturer/labs/{labId}/plagiarism`
- Students are never notified of a plagiarism flag
- Schema: `docs/sql/2026-08-19-plagiarism.sql`

## Work Guidance

- Reconstruct `.git` to a temp dir; never run hooks; delete the temp tree after read
- Do not fail student upload if inspect throws

## Verification

- `PlagiarismComparatorTest`, `GitHistoryReaderTest`, `SubmissionStorageServiceTest` git-path case

## Child DOX Index

No child docs.
