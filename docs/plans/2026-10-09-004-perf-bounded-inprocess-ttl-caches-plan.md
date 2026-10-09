---
title: Bounded in-process TTL caches - Plan
date: 2026-10-09
type: perf
topic: bounded-inprocess-ttl-caches
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Bounded in-process TTL caches - Plan

## Goal Capsule

**Objective:** Make the backend’s in-process TTL caches drop expired entries without a same-key re-hit, and hard-cap growth so free-text or historical keyspaces cannot grow without bound — covering analytics dashboard, lab statistics, master data, and student upload-access caches (audit F-1, F-2, F-3).

**Product authority:** Session 2026-10-09 brainstorm from the Whole-System Memory Leak Audit. sandbox-runner heap flags (F-5), sandbox soak (F-4), CI leak gates, and runtime memory metrics are not active scope.

**Open blockers:** None.

**Product Contract preservation:** unchanged (R/F/AE IDs preserved). Outstanding “Deferred to Planning” items resolved in Planning Contract KTDs.

## Product Contract

### Summary

Replace the lazy-only TTL pattern with expire-after-write plus a hard max size across the four in-process caches named in the audit. Lecturer/student-visible cache semantics and existing TTL lengths stay as today; this is eviction hygiene, not a product behavior change. Promote the temporary audit test into a permanent regression that asserts both “expired keys leave without re-query” and “distinct-key floods stay under the cap.”

### Key Decisions

- **Own only the shared lazy-TTL cache family (F-1 + F-2 + F-3)** — not F-5, F-4 soak, CI, or metrics. (session-settled: user-directed — chosen over cache+F-5 or all-findings+prevention) Governs R7.
- **All three findings must be fixed** — dashboard, upload-access, and the same-pattern lab-statistics / master-data caches. (session-settled: user-directed — chosen over F-1-only smallest cut) Governs R1, R2, R3.
- **Expire-after-write + hard max size** — not sweep-only (leaves within-TTL free-text floods) and not size-only (leaves dead never-revisited keys). (session-settled: user-approved — agent recommended approach C; user confirmed scoping synthesis) Governs R1, R4, R5.
- **TTL lengths and visible endpoints unchanged** — same configured TTLs and cache hit/miss meaning for callers; only eviction hygiene changes. Governs R6.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns only bounding the backend in-process TTL cache family called out as F-1 / F-2 / F-3 in the Whole-System Memory Leak Audit.

The broader audit remediation set is the current understanding, not a committed roadmap:

- sandbox-runner Dockerfile explicit heap cap (F-5)
  - Can proceed independently of this plan; deferred by choice
- CI leak gates / Micrometer memory gauges / metaspace soak of OT `URLClassLoader`
  - Can proceed independently; deferred as prevention work
- Temporary `CacheMemoryLeakAuditTest` evidence
  - Shares the same caches; this plan promotes that pattern into permanent assertions rather than deleting coverage

### Problem Frame

`InProcessTtlCache` and the structurally identical `StudentTermAccessService.accessCache` / `MasterDataCache` maps remove an expired entry only when that exact key is looked up again. Entries that are never revisited stay resident for the life of the JVM. For `AnalyticsDashboardCache`, one key dimension is free-text `course` from `GET /api/analytics/dashboard`, so distinct values create unbounded growth even after TTL. Lab statistics and master data use the same lazy shape but inherently smaller keyspaces; they are included so the anti-pattern is not left half-fixed.

### Requirements

**Eviction policy**

- R1. After an entry’s TTL elapses, it must leave the in-process cache without requiring another lookup of that same key.
- R4. Each covered cache must enforce a hard maximum entry count so distinct keys cannot grow without bound for the life of the JVM.
- R5. Under size pressure, eviction of still-fresh entries is acceptable; correctness is served by reload-on-miss, not by retaining every historical key.

**Coverage**

- R2. The policy in R1 and R4 applies to analytics dashboard cache, lab statistics cache, master data cache, and student upload-access success cache.
- R3. Explicit invalidation already used by lab statistics (and any equivalent hooks) must continue to drop the targeted entry immediately.

**Compatibility**

- R6. Configured TTL lengths and lecturer/student-visible cache behavior stay as today aside from earlier eviction under size pressure; public routes and filter semantics are unchanged.
- R7. This work does not change sandbox-runner packaging, add CI leak workflows, or expose new memory metrics.

**Verification**

- R8. Permanent unit coverage asserts that after TTL, entry count collapses without re-querying the same keys, and that inserting many distinct keys keeps size at or under the configured max.

### Key Flows

- F1. Expired key leaves without re-hit
  - **Trigger:** Many distinct cache keys are written, then TTL elapses with no repeat lookups.
  - **Steps:** Entries age past TTL; proactive expiry removes them; a size probe shows near-empty / only still-fresh entries.
  - **Outcome:** Dead keys do not accumulate for the JVM lifetime.
  - **Covered by:** R1, R2, R8

- F2. Distinct-key flood stays capped
  - **Trigger:** Many distinct keys arrive inside one TTL window (e.g. varying free-text `course` on the dashboard cache).
  - **Steps:** Cache accepts inserts until max size; further inserts evict per the size policy; size never exceeds the max.
  - **Outcome:** Heap growth from key diversity is bounded.
  - **Covered by:** R4, R5, R8

- F3. Normal hit path and explicit invalidate
  - **Trigger:** Repeat lookup within TTL, or lab-statistics invalidate after upload.
  - **Steps:** Fresh hit returns cached value without reload; invalidate removes that lab’s entry so the next read reloads.
  - **Outcome:** Existing warm-path and invalidate contracts still hold.
  - **Covered by:** R3, R6

### Acceptance Examples

- AE1. Expired keys without re-query
  - **Covers:** R1, R8
  - **Given:** N distinct entries inserted with a short TTL
  - **When:** TTL elapses and no key is looked up again
  - **Then:** Observed entry count is ~0 (or only non-expired survivors), not N

- AE2. Size cap under distinct-key flood
  - **Covers:** R4, R8
  - **Given:** Max size M configured for a covered cache
  - **When:** More than M distinct keys are inserted inside one TTL window
  - **Then:** Observed entry count is ≤ M

- AE3. Lab statistics invalidate still works
  - **Covers:** R3, R6
  - **Given:** A lab statistics entry is cached
  - **When:** Upload (or equivalent) invalidates that lab id
  - **Then:** The next read for that lab reloads rather than returning the stale entry

### Success Criteria

- Audit F-1 / F-2 / F-3 are closed by the same expire + max-size policy on all four caches.
- Permanent tests replace the temporary audit-only assertions and pass offline without DB or network.
- No change to dashboard filters, upload-access rules, or configured TTL property defaults.

### Scope Boundaries

**In scope**

- Analytics dashboard in-process cache (incl. free-text `course` key dimension)
- Lab statistics in-process cache
- Master data in-process cache
- Student upload-access success cache

**Deferred for later**

- sandbox-runner Dockerfile heap / RAM percentage (audit F-5)
- Live sandbox soak (audit F-4 gap)
- CI leak gates, Actuator/Micrometer memory alerts, OT worker metaspace soak

**Out of scope**

- Changing `course` query semantics or removing the parameter
- Frontend wiring of `course` search
- Other caches already bounded or actively swept (`LabRubricCache`, `SharedLocalWorkerCache`, `PresenceService`, sandbox session map)
- `LecturerOverviewCache` (single volatile slot; not named in F-1–F-3)

### Dependencies / Assumptions

- Existing TTL properties remain the source of write-expiry duration (`app.analytics.*`, `app.master-data-cache-ttl-minutes`, `app.upload.access-cache-ttl-seconds`).
- Frontend currently does not send `course` to the dashboard API; F-1 remains reachable by any lecturer JWT caller.
- `@EnableScheduling` is already on (`EiuCapstoneBackendApplication`); a periodic sweep can reuse that, mirroring `LabDeadlineReminderScheduler`.

### Sources / Research

- Whole-System Memory Leak Audit (local docx, 2026-10-09): findings F-1 / F-2 / F-3 and suggested bounded-cache remediation.
- Temporary evidence: `backend/src/test/java/unit/com/eiu/capstone/backend/analytics/cache/CacheMemoryLeakAuditTest.java`
- Lazy eviction: `backend/src/main/java/analytics/cache/InProcessTtlCache.java`
- Consumers: `AnalyticsDashboardCache`, `LabStatisticsCache`, `MasterDataCache`, `StudentTermAccessService`
- TTL docs: `backend/AGENTS.md` (analytics and upload access cache properties)
- Permanent access tests to preserve: `backend/src/test/java/support/com/eiu/capstone/backend/service/StudentTermAccessServiceTest.java`

---

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Extend hand-rolled `InProcessTtlCache` with `maxSize` + `sweepExpired()`; do **not** add Caffeine or another cache library. (session-settled: user-approved — plan synthesis call-out; user confirmed) | Satisfies R1/R4 without a new Maven dep that would risk worker shade / desktop footprint. Governs R1, R4, R7. |
| KTD2 | Make the shared utility usable from `service` (today package-private under `analytics.cache`) — prefer `public` type in place, or a one-line move to a shared package if package boundaries fight. | Required for R2 (`MasterDataCache`, `StudentTermAccessService`). |
| KTD3 | Default max sizes: dashboard **256**, lab statistics **512**, master data **1**, upload access **4096**; expose as Spring `@Value` properties with those defaults. | Dashboard is free-text-keyed (F-1); access is organic student×lab (F-2); master data is a single key; lab stats track finite labs. Governs R4. |
| KTD4 | Proactive expiry via `@Scheduled` sweep (interval ~30s, configurable) calling `sweepExpired()` on each covered instance; size eviction also runs on insert when over max (prefer dropping expired first, then an arbitrary/oldest eligible entry). | R1 needs a timer; R4 needs insert-time cap even between sweeps. Reuse existing `@EnableScheduling`. |
| KTD5 | Leave `LecturerOverviewCache` unchanged. (session-settled: user-approved — synthesis call-out; user confirmed) | Single volatile slot; not in F-1–F-3. Deferred consistency only. |
| KTD6 | Replace `CacheMemoryLeakAuditTest` in place with permanent unit tests (assert AE1/AE2; keep offline / no Spring DB). (session-settled: user-approved — synthesis call-out; user confirmed) | R8; delete “TEMPORARY audit-only” framing. Preserve/extend `StudentTermAccessServiceTest` behavior. Governs R8. |

### Assumptions

- Exact eviction order when all entries are fresh and over max (FIFO vs arbitrary map iteration) is an implementation detail as long as size never exceeds max (R4, R5).
- Sweep interval need not equal TTL; shorter than the smallest TTL (30s access cache) is enough for R1 in practice.
- Existing `app.upload.access-cache-ttl-seconds=0` disable behavior remains: when TTL ≤ 0, do not cache (today’s contract).

### High-Level Technical Design

Directional only — not implementation specification.

```text
Before: ConcurrentHashMap + lazy remove-on-get of expired key only
After:
  InProcessTtlCache(ttl, maxSize)
    get → same loader lock pattern; expired on hit still removed
    put path → if size > max: sweepExpired, then evict until ≤ max
    sweepExpired() → remove all expired (no key re-hit required)
  @Scheduled (~30s) → dashboard / labStats / masterData / access.sweepExpired()

Consumers:
  AnalyticsDashboardCache  → InProcessTtlCache(CacheKey, …)
  LabStatisticsCache       → InProcessTtlCache(UUID, …) + invalidate
  MasterDataCache          → InProcessTtlCache(String, …) single key
  StudentTermAccessService → InProcessTtlCache(String, UploadAccess)
```

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Size eviction of still-fresh dashboard keys under attack → more Neon reads | Accepted (R5); cap stops heap fill |
| Package-private cache blocks service migration | KTD2 — make public / shared |
| Sweep races with get/put | Keep ConcurrentHashMap + existing per-key load locks; sweep uses remove-if-expired safely |
| `StudentTermAccessService` constructor signature change breaks tests | Update `StudentTermAccessServiceTest` setUp for new max-size arg |

### Open Questions

**Deferred (non-blocking):** Exact property names / sweep interval default fine-tuning during implement.

---

## Implementation Units

### U1. Bound `InProcessTtlCache` core

- **Goal:** Shared map supports expire-without-rehit and hard max size.
- **Requirements:** R1, R4, R5; KTD1, KTD2
- **Dependencies:** None
- **Files:**
  - `backend/src/main/java/analytics/cache/InProcessTtlCache.java` (modify; visibility for service use)
  - `backend/src/test/java/unit/com/eiu/capstone/backend/analytics/cache/InProcessTtlCacheTest.java` (create; or fold into U3 if preferred)
- **Approach:**
  1. Add `maxSize` constructor arg (require ≥ 1).
  2. Add `sweepExpired()` that removes expired entries without a caller key.
  3. On insert, if `size > maxSize`, sweep then evict until ≤ max.
  4. Keep existing per-key load-lock + `invalidate(key)` behavior.
  5. Expose a package-test-friendly `size()` (or test via reflection as the audit did) — prefer a package-visible `size()` for assertions.
- **Patterns to follow:** Current `InProcessTtlCache` loader + lock shape; do not invent a second map type.
- **Execution note:** Characterization-first — keep a failing permanent test for “expired keys remain” before changing eviction, then green it.
- **Test scenarios:**
  - Happy: get within TTL returns cached value without second loader call.
  - Covers AE1: after TTL + `sweepExpired()`, size is 0 with no re-get of those keys.
  - Covers AE2: inserting `maxSize + N` distinct keys keeps `size() ≤ maxSize`.
  - Edge: `invalidate` removes a live key immediately.
  - Edge: concurrent gets for the same missing key still single-load (existing lock semantics).

### U2. Wire four consumers, config, and scheduled sweep

- **Goal:** Apply the shared policy to all R2 caches; start proactive sweeps.
- **Requirements:** R2, R3, R6, R7; KTD3, KTD4, KTD5
- **Dependencies:** U1
- **Files:**
  - `backend/src/main/java/analytics/cache/AnalyticsDashboardCache.java`
  - `backend/src/main/java/analytics/cache/LabStatisticsCache.java`
  - `backend/src/main/java/service/MasterDataCache.java`
  - `backend/src/main/java/service/StudentTermAccessService.java`
  - `backend/src/main/resources/application.properties`
  - `backend/.env.backend.example` (document new props if other caches are listed there)
  - New small `@Component` sweeper under `analytics/cache/` or `service/` (e.g. `InProcessTtlCacheSweeper`) — exact name left to implementer
  - `backend/AGENTS.md` (TTL / max-size / sweep row)
- **Approach:**
  1. Inject max-size `@Value` defaults per KTD3 into each consumer constructor; pass into `InProcessTtlCache`.
  2. Replace `MasterDataCache` and `StudentTermAccessService` private maps with the shared utility; preserve TTL=0 disable for access cache; preserve `invalidate()` on master data and lab stats.
  3. Add `@Scheduled` sweeper that calls `sweepExpired()` on the four instances (inject the four Spring beans / service).
  4. Do not touch `LecturerOverviewCache` (KTD5).
  5. Document new properties next to existing TTL props in `application.properties` and `backend/AGENTS.md`.
- **Patterns to follow:** `@Value` TTL injection on existing cache constructors; `LabDeadlineReminderScheduler` for `@Scheduled` style.
- **Test scenarios:**
  - Covers AE3: lab statistics `invalidate(labId)` then `get` invokes loader again.
  - Happy: `StudentTermAccessService` success path still caches and reuses within TTL (`StudentTermAccessServiceTest` updates).
  - Edge: access TTL `0` still skips cache put/get.
  - Integration: sweeper bean exists when Spring context loads (optional thin smoke if a context test already exists; otherwise unit-level sweep on injected fakes).

### U3. Permanent regression tests and DOX closeout

- **Goal:** R8 coverage replaces the temporary audit artifact; docs match behavior.
- **Requirements:** R8; KTD6
- **Dependencies:** U1, U2
- **Files:**
  - Replace `backend/src/test/java/unit/com/eiu/capstone/backend/analytics/cache/CacheMemoryLeakAuditTest.java` with permanent tests (rename class to e.g. `InProcessTtlCacheBoundTest` / keep path under `unit/.../analytics/cache/`)
  - `backend/src/test/java/support/com/eiu/capstone/backend/service/StudentTermAccessServiceTest.java` (constructor / cache assertions as needed)
  - Optional thin `MasterDataCache` unit test if not covered via U1
  - `backend/AGENTS.md` (analytics cache note: bounded + sweep)
- **Approach:**
  1. Delete “TEMPORARY audit-only / safe to delete” framing.
  2. Assert AE1 (post-TTL sweep → size collapses) and AE2 (flood ≤ max) against real `AnalyticsDashboardCache` / shared utility and access-cache migration.
  3. Drop the slow heap-snapshot microbench unless it stays useful as `@Disabled` / optional — prefer fast size assertions for CI.
  4. Update AGENTS analytics-cache bullet to mention max size + sweep.
- **Execution note:** Prefer test-first against U1/U2; keep suite offline (`mvn -o` friendly).
- **Test scenarios:**
  - Covers AE1: dashboard (or shared cache) after TTL + sweep → size ~0.
  - Covers AE2: > max distinct course keys → size ≤ max.
  - Happy: existing access-cache reuse within TTL still passes.
  - Edge: expired access entries removed by sweep without re-upload for that pair.

---

## Verification Contract

- `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.analytics.cache.**,support.com.eiu.capstone.backend.service.StudentTermAccessServiceTest" test` (adjust class names to the final permanent test names)
- Prefer full `mvn -f backend/pom.xml test` before merge when practical
- Manual checklist: lab statistics invalidate still wired from upload; no Caffeine/new dep in `backend/pom.xml`; `LecturerOverviewCache` untouched

## Definition of Done

- All four covered caches use expire-without-rehit + hard max size (R1, R2, R4).
- Scheduled sweep runs in the API JVM (R1).
- Lab statistics / master-data invalidate hooks still work (R3, AE3).
- Permanent unit tests cover AE1 and AE2; temporary audit framing is gone (R8, KTD6).
- TTL property defaults unchanged; new max-size props documented (R6).
- No sandbox Dockerfile, CI leak gates, or Micrometer work (R7).
- `LecturerOverviewCache` unchanged (KTD5).
