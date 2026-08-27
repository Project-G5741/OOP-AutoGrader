---
title: Assemble lab_result from in-memory rubric
date: 2026-08-27
type: perf
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Assemble lab_result from in-memory rubric

## Goal Capsule

**Objective:** Cut upload `assemble` wall time by building `lab_result` from the already-loaded `LabRubricSnapshot` (no Neon rubric reload), and skip MMD/testcase trees when those pillars are not applicable.

**Product authority:** Session 2026-08-27. User directed options (1) and (5) for Render free tier (512 MB, 1–2 CPUs, Neon). Explicitly out of scope: parallel DTO formatting, thin upload JSON, building bundles inside parallel `compute` workers.

**Open blockers:** None.

## Product Contract

### Summary

Students still receive a full `lab_result` for applicable pillars. Upload no longer re-queries class/member/relation/parameter rows. Inapplicable MMD or testcase pillars get empty trees with `scoreApplicability` false (tabs already hide on that flag). Class-tab GET paths are unchanged (still load structure from DB after persist wait).

### Requirements

- R1. Upload assemble must not call `ClassStructureService.loadChallengeStructures`.
- R2. Class / MMD / testcase display for applicable pillars matches current snapshot + correct-id semantics.
- R3. When `mmdApplicable` is false, do not build MMD class/relation trees; bundle still includes `mmd: { classes: [], parseError }` and `scoreApplicability.mmd = false`.
- R4. When `testcaseApplicable` is false, do not map testcase I/O cards; bundle `testcases` is `[]` and `scoreApplicability.testcase = false`.
- R5. No new executor or extra parallel assemble work (Render 1–2 CPU).
- R6. GET `/class` `/mmd` `/testcases` behavior unchanged.

### Acceptance Examples

- AE1. Large lab upload: `[timing] Grade submission` `assemble` no longer includes a full rubric reload; local/Render `assemble` drops vs pre-change.
- AE2. Challenge with `has_mmd=false`: MMD tab still hidden; Class tab still populated from snapshot.
- AE3. Challenge with no operational testcases: Operation Test tab hidden; example I/O not built.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Map `ChallengeRubric` + snapshot + `SubmissionCorrectIds` in `ClassStructureService` (`buildClassDataFromRubric` / `buildMmdDataFromRubric`) | Rubric already has display strings; GET path keeps JPA `loadChallengeStructures`. |
| KTD2 | Skip MMD/testcase builders using `PillarScoreBreakdown` applicability | Same flags the frontend already uses. |
| KTD3 | Keep assemble on the upload thread, sequential per challenge | Avoid nested pools on Render free tier. |

### Implementation Units

- **U1.** `ClassStructureService` — from-rubric Class and MMD DTO builders (snapshot-first grades, rubric-label fallback).
- **U2.** `LabResultAssembler` — drop structure load; call from-rubric builders; skip inapplicable pillars.
- **U3.** Tests + `grading/AGENTS.md`.

## Verification Contract

- Unit: assembler does not load structures; skips MMD/testcase mappers when not applicable.
- Unit: from-rubric class builder uses snapshot shell type (extend shell display test).
- `mvn test` from `backend/` for touched tests.

## Work Guidance

- Do not parallelize assemble.
- Do not change upload JSON shape except empty arrays for skipped pillars.
