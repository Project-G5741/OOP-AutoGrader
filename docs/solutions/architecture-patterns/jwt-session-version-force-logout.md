---
title: Force logout via JWT session_version and presence 401
date: 2026-09-25
category: architecture-patterns
module: backend-security
problem_type: architecture_pattern
component: authentication
severity: high
applies_when:
  - "Admin deletes, suspends, or removes a student from the current term while that user is still signed in"
  - "JWTs remain cryptographically valid after access is revoked"
  - "SPA keeps the session on HTTP 403 and only clears on 401"
  - "GET /api/presence is permitAll but signed-in clients poll it as a heartbeat"
tags:
  - jwt
  - session-revoke
  - force-logout
  - presence
  - session-version
---

# Force logout via JWT session_version and presence 401

## Problem

Lecturer hard-delete, suspend, and remove-from-current-term left the student's Bearer JWT usable until natural expiry (~1h). The SPA treated many forbidden responses as “stay signed in,” and a kicked student could still submit on an open session.

## Solution

1. Add `user_account.session_version` (default 0) and mint JWT claim `sv`.
2. On **suspend** and **remove from current term**, bump `session_version`. On **hard delete**, drop the row and clear the in-process version cache.
3. `JwtAuthenticationFilter` accepts the token only when the account exists and claim `sv` matches the DB (via `SessionValidityService` with a short email→version cache). Mismatch or missing user → clear `SecurityContext` (request is anonymous).
4. Keep `GET /api/presence` public for anonymous Active Users counts, but if the request carries `Authorization: Bearer …` and the principal is null (failed session check), return **401**.
5. SPA `fetchPresenceCount` (10s poll): when a Bearer was sent and the response is 401, call `clearSessionAndRedirectToLogin`.

## Why presence must 401

`GET /api/presence` is `permitAll`. A revoked Bearer otherwise becomes an anonymous 200 with a count, so the SPA never hard-cuts. The Bearer-without-principal → 401 rule preserves public counts while making the existing poll the force-logout heartbeat.

## Related

- Plan: `docs/plans/2026-09-25-003-feat-force-logout-on-access-revoke-plan.md`
- Default-deny / 401 vs 403 SPA behavior: `docs/solutions/architecture-patterns/spring-security-default-deny-matcher-table.md`
- Operator SQL: `docs/sql/2026-09-25-user-session-version.sql` (also `SessionVersionSchemaMigrator` on startup)
