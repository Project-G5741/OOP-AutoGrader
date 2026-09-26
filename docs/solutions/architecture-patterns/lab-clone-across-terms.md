---
title: "Deep-clone labs across academic quarters"
date: 2026-09-26
category: architecture-patterns
module: labs
problem_type: architecture_pattern
component: service_object
symptoms: medium
applies_when:
  - "Lecturers need to reuse a prior quarter's lab rubric without moving history"
  - "Structure and operational testcases must copy while deadlines/visibility reset"
  - "Clone must remap every entity UUID so save paths treat the copy as inserts"
tags: [lab-clone, term, copyLabIds, operational-testcase, previous-current]
related_components:
  - backend
  - frontend
---

# Deep-clone labs across academic quarters

## Context

Labs are bound to one `term_id`. Switching the current quarter hides prior labs from Solution Management and student submit. There was no clone API—only frontend `cloneDraft` for editor state—so lecturers rebuilt rubrics and OT by hand each quarter.

## Guidance

1. **Clone, never move** — Create a new `lab` row on the target term. Leave the source lab and all submissions/progress on the old term.
2. **DTO remap + RTT-aware persist** — Batch-load source with `loadForEditor` + `loadDtosGroupedByChallengeIds` → fresh UUIDs for challenges/classes/members/relations/testcases/invocations/assertions → rewrite cross-refs → `createLab(name, termId)` (null deadline → term end) → `saveLabStructureInsertOnly` (one flush) → `persistClonedTestcasesBatch` (insert-only, one flush; no per-entity find/delete/reload). Do not share entity IDs across labs.
3. **Reset access settings** — Do not copy deadline, `student_visible`, or `release_date`; blank-lab defaults apply.
4. **Two entry points, one engine** — New quarter optional `copyLabIds` (source = outgoing current at submit). Copy lab UI uses `GET /clone-sources` (previous-current by year/term ordinal) + `POST /clone`.
5. **Best-effort multi-clone after term create** — Commit the term in a short `TransactionTemplate` first; each lab clones in `REQUIRES_NEW`. Multi-lab clone overlaps up to 4 short transactions. Per-lab failures return as `cloneErrors` without rolling back the quarter.
6. **Static clone routes before `/{labId}`** — Register `/clone-sources` and `/clone` above path-variable mappings so Spring does not treat `clone` as a UUID.

## Why this works

Rubric and OT already persist through lecturer save APIs that upsert by client UUID. Remapping IDs turns a loaded graph into a clean insert graph without a second persistence stack. Previous-current ordinal avoids a stored predecessor while matching sequential quarter rollover.

Holding one transaction across create-term + multi-lab OT upserts (with per-entity `findById`/`flush`) times out on Neon: round-trip latency dominates. Short term commit, per-lab `REQUIRES_NEW`, batched OT load, insert-only structure+OT write (deferred flush), and parallel lab clones keep the request under the proxy/client failure window ("Server Busy").

Term/lab delete must also stay set-based: `deleteLabsCascadeBulk` runs a fixed sequence of SQL `DELETE`s for all labs in the quarter (not per-field/per-class entity cascades). Entity-by-entity wipe was >1 minute for a few cloned labs on Neon.

Do not load the source rubric inside the write `REQUIRES_NEW` with `@Transactional(readOnly=true)` — joining marks the connection read-only and every INSERT fails (quarter created, all lab copies fail). Flush before OT `findAllById` so unflushed challenge inserts from `saveLabStructure` are visible in the same TX.

New structure rows with client/clone UUIDs must use `EntityManager.persist` and must **not** use `@GeneratedValue` on those entities (`Challenge`, `ClassEntity`, `ClassRelation`, `Field`, `Method`, `Constructor`). With `@GeneratedValue`, `persist` of a preset UUID throws `detached entity passed to persist`; with Spring Data `save`, `merge` can replace the id and break remapped relations ("Relation classes must belong to the same problem"). Same pattern as OT (`Testcase` / invocations / assertions).

Relation save allows multiple `IMPLEMENTATION`/`REALIZATION` edges from one class (Java multi-implements). Only multiple `INHERITANCE` edges from one class are rejected. Lab `23c0240f-…` challenge 6 (Vehicle Rental) has `Bike`/`Car`/`Truck` each implementing two interfaces — that is valid source data and must clone.

## Related

- Plan: `docs/plans/2026-09-26-001-feat-lab-clone-across-terms-plan.md`
- Service: `backend/src/main/java/com/eiu/capstone/backend/service/LabCloneService.java`
- Vocabulary: `CONCEPTS.md` → Lab clone
