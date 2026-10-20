---
title: Persist MMD member outcomes on submission_challenge_result
date: 2026-10-20
status: implemented
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
execution: code
---

# Persist MMD member outcomes (JSONB on SCR)

## Problem

GET `/mmd` rebuilds member ticks from **Class** `correctIds` when `mmdOutcome` is null. Challenge score uses real MMD compare at grade time. Result: Overall can be 96% while the MMD tab shows all green.

## Decision

Add nullable `submission_challenge_result.mmd_outcomes JSONB` (versioned document). Write at grade/persist time from `MmdGradingOutcome`. GET `/mmd` reconstructs outcome from that column when present; legacy null keeps today’s Class-`correctIds` fallback.

No new element tables.

## Document shape (`v: 1`)

```json
{
  "v": 1,
  "classes": { "<uuid>": { "present": true, "typeOk": true } },
  "fields": { "<uuid>": true },
  "methods": { "<uuid>": false },
  "constructors": { "<uuid>": true },
  "relations": { "<uuid>": true }
}
```

Omit or null when MMD pillar not applicable.

## Scope

**In**

- Operator SQL + cloud startup migrator; desktop via `ddl-auto: update`
- Entity + Postgres/H2 SCR upsert/persistUpload
- `GradingService` attach outcomes when building SCR rows
- `ClassStructureService` GET prefer reconstructed `MmdGradingOutcome`
- Unit/support tests for codec + assembly preference
- DOX / AGENTS + baseline SQL note

**Out**

- Regrading historical submissions (null → degraded ticks)
- Changing scoring math
- Frontend changes

## Key files

- `docs/sql/2026-10-20-scr-mmd-outcomes.sql`
- `docs/sql/2026-10-13-schema-baseline-reconstructed.sql`
- `backend/src/main/java/config/MmdOutcomesSchemaMigrator.java`
- `backend/src/main/java/model/SubmissionChallengeResult.java`
- `backend/src/main/java/grading/MmdOutcomesCodec.java` (new)
- `backend/src/main/java/grading/PostgresGradingResultJdbcWriter.java`
- `backend/src/main/java/grading/H2GradingResultJdbcWriter.java`
- `backend/src/main/java/grading/GradingService.java`
- `backend/src/main/java/service/ClassStructureService.java`
- `backend/src/main/java/repository/SubmissionChallengeResultRepository.java`

## Test scenarios

1. Codec round-trip: outcome → JSON → outcome preserves present/typeOk and member flags
2. `buildMmdDataFromRubric` with reconstructed outcome shows failed MMD member even when Class `correctIds` mark it correct
3. Null outcomes still uses Class `correctIds` (legacy)

## Operator step

Run `docs/sql/2026-10-20-scr-mmd-outcomes.sql` on Neon (or rely on startup migrator). Re-upload to populate outcomes for a student.
