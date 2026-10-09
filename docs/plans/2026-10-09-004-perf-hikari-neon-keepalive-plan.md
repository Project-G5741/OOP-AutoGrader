---
title: Hikari Neon keepalive and fail-fast - Plan
date: 2026-10-09
type: perf
topic: hikari-neon-keepalive
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Hikari Neon keepalive and fail-fast - Plan

## Goal Capsule

**Objective:** Make Neon/Postgres pool connections fail fast after suspend and stay healthier while idle, by adding Hikari keepalive and JDBC socket/TCP keepalive settings shared by all Postgres deploys.

**Product authority:** Session 2026-10-09 brainstorm. Region pinning, DB-touching health probes, borrow-time connection validation, and whole-pool retunes are not active scope for this plan.

**Open blockers:** None.

## Product Contract

### Summary

Add additive Hikari `keepalive-time` and JDBC `socketTimeout` / `tcpKeepAlive` (committed properties + example URL) so local Neon and Render share the same readiness. Do not change pool size, `max-lifetime`, `minimum-idle`, checkout validation, or health endpoints. Desktop H2 keeps its own profile datasource overrides.

### Key Decisions

- **Keepalive and fail-fast together are the finish line** — not keepalive-only or fail-fast-only. (session-settled: user-directed — chosen over single-sided fixes) Governs R1, R2.
- **All Postgres / Neon deploys share settings** — committed `application.properties` plus example JDBC URL for local and Render. (session-settled: user-directed — chosen over production-only or docs-only) Governs R3, R4.
- **Additive timeouts only** — no borrow-time connection test/validation and no retune of pool size / `max-lifetime` / `minimum-idle`. (session-settled: user-directed — chosen over checkout validation or whole-pool retune) Governs R5, R6.
- **No DB-touching health probe** — Neon may still suspend; this work detects/recovers faster, it does not keep Neon warm via `GET /`. (session-settled: user-directed — chosen over warm-from-health) Governs R7.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns only Hikari / JDBC readiness for Neon suspend.

The broader Neon / DB-slowness checklist from the 2026-10-09 analysis is the current understanding, not a committed roadmap:

- Plagiarism Neon connection release (`docs/plans/2026-10-09-001-perf-plagiarism-neon-connection-release-plan.md`)
  - Independent; already planned/shipped
- Rubric overlap isolation (`docs/plans/2026-10-09-002-perf-persist-executor-rubric-isolation-plan.md`)
  - Independent; already planned/shipped
- Detail UPSERT single transaction (`docs/plans/2026-10-09-003-perf-detail-upsert-single-transaction-plan.md`)
  - Independent; already planned/shipped
- Render region vs Neon Singapore
  - Can proceed independently; deferred
- Detail-gate still sharing `persistExecutor` with inspect
  - Deferred earlier; unaffected by this plan

### Problem Frame

Today `application.properties` sets Hikari `maximum-pool-size=10`, `minimum-idle=2`, `connection-timeout=30s`, `max-lifetime=30min`, but not `keepalive-time`, JDBC `socketTimeout`, or `tcpKeepAlive`. Idle TCP does not count as Neon query activity, so the database can suspend. After suspend, Hikari can hand out a dead connection; the PostgreSQL driver’s default socket timeout is infinite, so the first persist after idle can hang until TCP gives up. Render’s `GET /` health check does not touch the database. Desktop H2 uses a separate profile and is not the Neon suspend path.

### Requirements

**Readiness behavior**

- R1. Hikari must run keepalive probes on idle pooled connections (configured `keepalive-time`) for the default Postgres datasource.
- R2. JDBC clients must use a finite `socketTimeout` and enable `tcpKeepAlive` so a dead connection fails within a bounded wait instead of hanging indefinitely.
- R3. Those settings apply to all non-desktop Postgres/Neon deploys that use the shared `application.properties` / example URL (local Neon and Render).

**Operator discoverability**

- R4. `backend/.env.backend.example` (and deploy docs that document the JDBC URL, if they omit these params) must show the socket/TCP keepalive query params alongside the existing pooler URL guidance.

**Boundaries**

- R5. Do not add Hikari connection-test-query / validation-timeout (or equivalent borrow-time validation) as part of this work.
- R6. Do not change `maximum-pool-size`, `minimum-idle`, `connection-timeout`, or `max-lifetime` as part of this work.
- R7. Do not change health endpoints to run SQL, and do not pin Render region.
- R8. Desktop (`application-desktop.yml` H2) remains on its own datasource overrides; this plan does not require H2 keepalive parity.

### Key Flows

- F1. First DB use after Neon idle/suspend
  - **Trigger:** Upload or other Postgres work after the compute/pool has been idle long enough for Neon to suspend.
  - **Steps:** Pool may still hold a dead TCP socket; socket timeout / keepalive cause failure or refresh within a bounded time; next borrow proceeds on a live connection.
  - **Outcome:** Request fails or retries within the configured timeout budget instead of hanging until OS TCP gives up.
  - **Covered by:** R1, R2

- F2. Steady-state local or Render Postgres
  - **Trigger:** Normal traffic with shared properties and example URL params applied.
  - **Steps:** Idle connections receive Hikari keepalive; TCP keepalive remains enabled.
  - **Outcome:** Fewer surprise dead connections; no change to pool size or health-check behavior.
  - **Covered by:** R1, R3, R7

### Acceptance Examples

- AE1. Covers R1, R3. Given a non-desktop Spring Boot start against Postgres, When Hikari config is inspected, Then `keepalive-time` is set (non-zero) and existing pool size / max-lifetime values are unchanged.
- AE2. Covers R2, R4. Given `backend/.env.backend.example` (and deploy JDBC docs if updated), When an operator copies the example URL, Then it includes finite `socketTimeout` and `tcpKeepAlive=true` (or equivalent documented properties) on the pooler URL.
- AE3. Covers R5–R8. Given desktop profile and this change set, When reviewing diffs, Then H2 datasource overrides are untouched and no health-check SQL or borrow validation knobs were added.

### Success Criteria

- Idle Postgres pool connections use Hikari keepalive (R1).
- Dead connections fail within a finite socket timeout rather than hanging indefinitely (R2).
- Local Neon and Render share the same committed/example settings (R3, R4).
- Pool size / lifetime / borrow validation / health / region / desktop H2 stay out of the change set (R5–R8).

### Scope Boundaries

**In scope:** Hikari `keepalive-time`; JDBC `socketTimeout` and `tcpKeepAlive` via properties and/or example URL; DOX / deploy example updates that document those params; leave existing Hikari size/lifetime knobs as-is.

**Deferred for later:** Render region next to Neon Singapore; DB-touching readiness/liveness probes; Hikari borrow validation; retuning pool size or `max-lifetime` after observing Neon resets in staging.

**Out of scope:** Changing grading SQL, executors, detail UPSERT, plagiarism inspect; schema migrations; desktop H2 pool redesign.

### Dependencies / Assumptions

- Production and local cloud DB continue to use Neon **pooler** hostname (`-pooler`) as already documented.
- `SPRING_DATASOURCE_URL` remains the operator-controlled JDBC URL; example file must stay copy-paste safe.
- Desktop profile overrides datasource URL to H2 and does not need Neon suspend behavior.
- Exact numeric values for keepalive and socket timeout are planning/implementation choices within “finite and Neon-sensible,” not fixed in this Product Contract.

### Sources / Research

- `backend/src/main/resources/application.properties` — current Hikari knobs; no keepalive / socket timeout.
- `backend/.env.backend.example` — Neon pooler URL without `socketTimeout` / `tcpKeepAlive`.
- `backend/src/main/resources/application-desktop.yml` — H2 datasource overrides (out of scope).
- `docs/HOW_IT_RUNS.md` — Hikari max 10; Neon pooler guidance.
- `backend/DEPLOY_RENDER.md` — deploy JDBC / pooler notes (update if URL params are documented there).
- HikariCP wiki Rapid Recovery / TCP keepalive — driver `socketTimeout` and `tcpKeepAlive`; `keepaliveTime` must be &lt; `maxLifetime`.
- 2026-10-09 Neon slowness analysis — item 4: pool not set up for Neon suspending.

## Planning Contract

### Key Technical Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| KTD1 | Set `spring.datasource.hikari.keepalive-time=120000` (2 min) explicitly; leave `max-lifetime=1800000` unchanged | Matches Hikari’s recommended idle ping band; must stay &lt; max-lifetime; under typical Neon idle-suspend windows. Governs R1, R6. |
| KTD2 | Set pgjdbc via Hikari `data-source-properties`: `socketTimeout=30` (seconds) and `tcpKeepAlive=true` | Hikari Rapid Recovery guidance; applies even when an operator’s live `SPRING_DATASOURCE_URL` omits query params. Governs R2, R3. |
| KTD3 | Also append `&socketTimeout=30&tcpKeepAlive=true` on the example pooler URL in `.env.backend.example` and mention them in `DEPLOY_RENDER.md` | Satisfies R4 discoverability; keep values identical to KTD2. |
| KTD4 | Do not set `connection-test-query`, `validation-timeout`, or change health endpoints | Honors R5, R7; keepalive uses JDBC4 `isValid()` by default. |
| KTD5 | Prove AE1/AE2 with a small classpath property + example-file assertion test; prove AE3 by leaving `application-desktop.yml` untouched and reviewing the diff | Avoids heavy Spring Boot context for config-only work. |

### Assumptions

- Spring Boot 3.2 / Hikari in this repo accept `keepalive-time` and nested `data-source-properties.*` as today for `reWriteBatchedInserts`.
- pgjdbc `socketTimeout` is in **seconds**; Hikari `keepalive-time` is in **milliseconds**.
- Existing operator `.env` files without URL params still pick up KTD2 from `application.properties` after deploy/restart.

### High-Level Technical Design

Directional only — not implementation specification.

```text
application.properties (Postgres default)
  hikari.keepalive-time=120000
  hikari.data-source-properties.socketTimeout=30
  hikari.data-source-properties.tcpKeepAlive=true
  (existing pool size / max-lifetime unchanged)

.env.backend.example + DEPLOY_RENDER.md
  pooler URL …&socketTimeout=30&tcpKeepAlive=true

application-desktop.yml
  unchanged
```

### Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `socketTimeout=30` aborts a legitimately long single statement | Current score persist is one CTE; detail UPSERT is one TX of short statements; 30s matches Hikari Rapid Recovery floor. Raise later only if timing logs show false timeouts. |
| Operators keep stale URL without params | Properties path (KTD2) still applies; docs/example catch new copies. |
| Keepalive pings count as Neon activity and may delay suspend | Intentional for readiness; not a warm-forever health probe. |

### Open Questions

**Deferred (non-blocking):** None. Numeric values are KTDs above.

## Implementation Units

### U1. Hikari keepalive + JDBC fail-fast properties

- **Goal:** Commit shared Postgres pool readiness knobs without changing pool size/lifetime or adding borrow validation.
- **Requirements:** R1, R2, R3, R5, R6
- **Approach:** In `application.properties`, add `keepalive-time=120000` and `data-source-properties.socketTimeout=30` / `tcpKeepAlive=true` beside existing Hikari keys. Do not touch `maximum-pool-size`, `minimum-idle`, `connection-timeout`, `max-lifetime`, or add `connection-test-query` / `validation-timeout`. Honors KTD1, KTD2, KTD4.
- **Files:** `backend/src/main/resources/application.properties`
- **Patterns:** Existing `spring.datasource.hikari.*` and `data-source-properties.reWriteBatchedInserts` style in the same file.
- **Test scenarios:**
  - Happy / AE1: classpath `application.properties` asserts `keepalive-time=120000` and unchanged `maximum-pool-size=10`, `max-lifetime=1800000`.
  - Happy / R2: asserts `data-source-properties.socketTimeout=30` and `tcpKeepAlive=true`.
  - Boundary / AE3: test does not require desktop profile; confirm no new validation/test-query keys.
- **Files (tests):** `backend/src/test/java/unit/com/eiu/capstone/backend/config/HikariNeonReadinessPropertiesTest.java` (new)
- **Verification:** `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.config.HikariNeonReadinessPropertiesTest" test`
- **Dependencies:** None

### U2. Operator example URL + deploy/DOX notes

- **Goal:** Make socket/TCP keepalive discoverable for local Neon and Render operators.
- **Requirements:** R4, R7, R8
- **Approach:** Append `&socketTimeout=30&tcpKeepAlive=true` to the example pooler URL in `.env.backend.example`. Update `DEPLOY_RENDER.md` pooler bullet to mention those params (and that `application.properties` also sets them). Brief note in `docs/HOW_IT_RUNS.md` and/or `backend/AGENTS.md` env table — keepalive + finite socket timeout for Neon suspend. Leave `application-desktop.yml` untouched. Honors KTD3, KTD5.
- **Files:** `backend/.env.backend.example`; `backend/DEPLOY_RENDER.md`; `docs/HOW_IT_RUNS.md`; `backend/AGENTS.md` (only if env/pooler table needs the params)
- **Patterns:** Existing pooler wording in `DEPLOY_RENDER.md` and `backend/AGENTS.md` env table.
- **Test scenarios:**
  - Happy / AE2: unit test or same properties test reads `.env.backend.example` and asserts URL contains `socketTimeout=30` and `tcpKeepAlive=true`.
  - Boundary / AE3: `application-desktop.yml` not in the change set.
- **Files (tests):** extend `HikariNeonReadinessPropertiesTest` (or sibling) to assert example URL substrings
- **Verification:** same Maven test target as U1; diff review for desktop yml / health endpoints
- **Dependencies:** U1 (shared test home optional; can land in one commit)

## Verification Contract

- `mvn -f backend/pom.xml "-Dtest=unit.com.eiu.capstone.backend.config.HikariNeonReadinessPropertiesTest" test`
- AE1: `keepalive-time` present; pool size / max-lifetime unchanged
- AE2: example URL (and deploy note) document `socketTimeout` + `tcpKeepAlive`
- AE3: no desktop H2 edits; no health SQL; no `connection-test-query` / validation knobs
- Prefer smoke: local `mvn spring-boot:run` still connects to Neon with updated properties (manual)

## Definition of Done

- Postgres Hikari keepalive + JDBC socket/TCP keepalive committed (R1–R3).
- Example URL and deploy docs show the params (R4).
- Pool size/lifetime, borrow validation, health probes, region, and desktop H2 unchanged (R5–R8).
- Property/example assertion test green; DOX notes current.
