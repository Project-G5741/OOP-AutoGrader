---
title: Assemble lab_result from in-memory rubric snapshot
date: 2026-08-27
category: architecture-patterns
module: grading engine
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "Upload Grade submission assemble is a large share of wall time after persist is fast"
  - "LabRubricSnapshot is already in memory at assemble"
  - "Render free tier has 1–2 CPUs and must not add assemble thread pools"
tags:
  - grading-assemble
  - neon
  - performance
  - lab-result
---

# Assemble lab_result from in-memory rubric snapshot

## Context

After challenge-score UPSERT, upload `assemble` still reloaded the full class/member/relation graph from Neon (`loadChallengeStructures`) even though `LabRubricSnapshot` already held the same display strings. Inapplicable MMD/testcase pillars still built full trees the UI hides via `scoreApplicability`.

## Guidance

1. Map `ChallengeRubric` + parsed snapshot + correct ids in `ClassStructureService.buildClassDataFromRubric` / `buildMmdDataFromRubric`.
2. Skip MMD and testcase DTO builders when `PillarScoreBreakdown` marks the pillar not applicable; keep empty arrays and `scoreApplicability` false.
3. On upload assemble, always pass an explicit `mmdSubmitted` override so `buildMmdDataFromRubric` never calls `SubmissionResultLoader` (GET tabs may still infer).
4. Leave GET `/class` `/mmd` `/testcases` on the JPA structure load after `SubmissionDetailPersistGate.await`.
5. Do **not** parallelize assemble or build the full DTO tree on `gradingExecutor` workers (Render 1–2 CPU; heap).

## Do not

- Call `loadChallengeStructures` on the upload assemble path.
- Treat a 0 pillar score as “not applicable”; use the applicability flags.
