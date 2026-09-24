---
title: Force Logout on Access Revoke - Plan
type: feat
date: 2026-09-25
topic: force-logout-on-access-revoke
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Force Logout on Access Revoke - Plan

## Goal Capsule

- **Objective:** When an admin deletes a user, removes a student from the current term, or suspends them, revoke that person's live session and force their open client to the login screen within seconds — even if they never click — so they cannot keep submitting on the old session.
- **Product authority:** This plan owns access-revoke session kill and client hard-cut logout for those three admin actions. Broader session management (password change, role change, multi-device revoke-all) is not active scope.
- **Open blockers:** None — ready for implementation.
- **Stop conditions:** Do not ship live push / WebSocket eviction as the primary path. Do not block re-login after term removal. Do not replace forced logout with access-gating alone.

## Product Contract

### Summary

On hard delete, remove-from-current-term, or suspend, the affected user's session is revoked server-side immediately, and any open client forces logout to the login screen within seconds via a short heartbeat. Between revoke and the next heartbeat, protected APIs already reject the old session so submit cannot continue.

### Problem Frame

Today a student removed from the current term can still submit if they remain logged in. JWT sessions are not revoked on admin access changes, and the SPA keeps the session on many forbidden responses. Lecturers expect a kick to stick while the student is still online.

### Key Decisions

- **Hard cut within seconds** over next-action-only logout. Idle open tabs must leave. `(session-settled: user-directed — chosen over next-action: even with no click)` — Governs R1, R4
- **Forced logout required** over blocking submit without logging out. `(session-settled: user-directed — chosen over access-gate-only: kick itself is the outcome)` — Governs R1, R5
- **Triggers: delete + remove-from-term + suspend** over delete/remove only. `(session-settled: user-directed — chosen over delete+remove only: same hard cut for all three)` — Governs R2
- **After term removal, re-login allowed** (out-of-term as today) over stay-out-until-reenrolled. `(session-settled: user-directed — chosen over ban-until-reenroll: kick the session, not the account)` — Governs R3
- **Mechanism: session revoke + short heartbeat** over live push. `(session-settled: user-directed — chosen over WebSocket push: hard cut without a dedicated push channel)` — Governs R1, R4, R6

### Actors

- **A1. Admin / lecturer** — deletes a user, removes a student from the current term, or suspends a student.
- **A2. Affected user (typically student)** — has an open signed-in SPA session when access is revoked.
- **A3. System** — revokes the session and drives the client hard cut.

### Requirements

**Revoke and hard cut**

- R1. When A1 completes delete, remove-from-current-term, or suspend for A2, A2's live session is revoked immediately and A2's open client is forced to the login screen within seconds even if A2 never clicks.
- R2. The three triggers in R1 are the only admin actions in this scope that must force logout.
- R3. After remove-from-current-term, A2 may sign in again and receive the existing out-of-term experience; the account is not banned by this feature.
- R4. Between server revoke and the next client heartbeat, any protected API call using the revoked session fails authentication so A2 cannot keep submitting on that session.
- R5. Access-gating alone (blocking submit while leaving the SPA signed in) does not satisfy this feature.

**Client and messaging**

- R6. The open client discovers revocation via a short heartbeat (or equivalent short-interval session check) and clears local session state then lands on login.
- R7. After hard delete, re-login fails because the account is gone (existing behavior). After suspend, re-login remains blocked while inactive (existing behavior).

### Key Flows

- F1. Kick while online
  - **Trigger:** A1 deletes, removes from current term, or suspends A2 while A2's SPA is open.
  - **Actors:** A1, A2, A3
  - **Steps:** Admin action succeeds → session revoked → heartbeat (or next protected call) detects revoke → SPA clears session → login screen.
  - **Covered by:** R1, R4, R6
- F2. Term removal then re-login
  - **Trigger:** A2 was removed from the current term and later signs in again.
  - **Actors:** A2
  - **Steps:** Hard cut completed → A2 authenticates successfully → out-of-term experience as today (history, no current-term submit).
  - **Covered by:** R3, R7

### Acceptance Examples

- AE1. Online student removed from term
  - **Covers:** R1, R4, R6
  - **Given:** Student S is signed in on the dashboard and enrolled in the current term.
  - **When:** A lecturer removes S from the current term.
  - **Then:** Within seconds S is on the login screen without needing to click; any submit attempt with the old session fails.
- AE2. Re-login after term removal
  - **Covers:** R3
  - **Given:** S was removed from the current term and hard-cut logged out.
  - **When:** S signs in again with a valid account.
  - **Then:** Login succeeds and S gets the existing out-of-term experience (not a permanent ban).
- AE3. Suspend while online
  - **Covers:** R1, R2, R7
  - **Given:** Student S is signed in.
  - **When:** A lecturer suspends S.
  - **Then:** S is hard-cut to login within seconds; further login stays blocked while inactive.
- AE4. Gate-only is insufficient
  - **Covers:** R5
  - **Given:** A design that only returns forbidden on submit and leaves the SPA signed in.
  - **When:** Evaluated against this contract.
  - **Then:** It does not meet acceptance; forced logout is required.

### Success Criteria

- A kicked online student cannot complete a submit on the revoked session after the admin action.
- Open clients reach login within seconds of revoke without user interaction.
- Term-removed students can still re-authenticate into the existing out-of-term path.

### Scope Boundaries

- **In:** Session revoke + hard-cut logout for hard delete, remove-from-current-term, and suspend.
- **Out / deferred:** Live push / WebSocket as primary eviction; blocking re-login after term removal; revoke on password reset, role edits, or other admin changes; multi-device "logout everywhere" UX beyond what revoke naturally causes; redesign of no-access (403) vs login (401) for unrelated forbidden cases.

### Dependencies / Assumptions

- Assumption: Lecturer workaround cost for today's submit-after-kick leak was skipped in dialogue; the submit leak itself is the motivating evidence.
- Assumption: "Within seconds" means a short heartbeat interval acceptable for classroom use (exact interval chosen in planning).
- Dependency: Existing SPA clears session on unauthenticated failure paths; planning must align revoke signaling with that behavior so hard cut actually clears storage and navigates to login.
- Fact (verified): Today there is no JWT revocation / force-logout channel; claims-only JWT filter; remove-from-term deletes enrollment only; suspend sets inactive; SPA clears on 401 and keeps session on 403; `GET /term-access` can return 200 with `isInCurrentTerm: false` without clearing session.

### Outstanding Questions

**Resolve Before Planning**

- None.

**Deferred to Planning** — resolved in Planning Contract KTDs below.

### Sources / Research

- Grounding: no revocation/push today; `JwtAuthenticationFilter` claims-only; JWT ~1h; hard delete / removeStudent / suspend paths; SPA `apiFetch` 401 clears / 403 keeps; presence poll every 10s is Active Users only.
- Claim verification (2026-09-25): ten repo claims confirmed; nuance that `GET /term-access` returns 200 + `false` for not-enrolled rather than 401.

## Planning Contract

### Key Technical Decisions

- KTD1. **`user_account.session_version` + JWT claim `sv`** — mint tokens with the account's current version; bump version on suspend and on remove-from-current-term; missing/mismatched `sv` (legacy missing → treat as 0) fails auth at the filter. Governs R1, R4.
- KTD2. **Hard delete rejects missing account in the filter** — after delete there is no row; filter clears SecurityContext so requests are unauthenticated (401), without relying on `requireActiveUser`. Governs R1, R4, R7.
- KTD3. **Revoke only when removing from the current term** — `TermService.removeStudent` bumps version iff that term is current; non-current term removals stay enrollment-only. Governs R2.
- KTD4. **Reuse the 10s presence poll as heartbeat** — `GET /api/presence` with a revoked JWT becomes unauthenticated; SPA presence fetch on 401 clears session and goes to login (same as `apiFetch`). No new poll endpoint. Governs R1, R6.
- KTD5. **Filter reject is enough for upload** — revoked JWTs never reach `requireUploadAccess`, so the 30s upload success cache cannot keep a kicked student submitting. Governs R4.
- KTD6. **Hard delete of any deletable user uses the same reject-missing path** — existing lecturer delete permissions unchanged; no student-only carve-out. Governs R2.

### Technical Design

- Schema: `session_version INTEGER NOT NULL DEFAULT 0` on `user_account` (operator SQL + startup migrator).
- `SessionValidityService`: resolve expected version by email (short in-memory cache); bump + invalidate cache; mark deleted emails rejected until natural expiry of cache miss → DB miss.
- `JwtService.createToken(..., sessionVersion)` adds claim `sv`.
- `JwtAuthenticationFilter`: after parse, if email missing from DB or claim `sv` ≠ expected → clear context (do not set Authentication).
- Triggers call bump (suspend; remove when term current) or rely on missing row (delete).
- Frontend: `GET /api/presence` stays public for anonymous counts, but a Bearer that fails session validity returns **401**; SPA presence fetch clears session and redirects to login.

### Assumptions and Dependencies

- Presence poll already runs every 10s on signed-in AppShell footers — acceptable “within seconds.”
- Suspend today returns 403 on gated APIs (session kept); version bump is required so filter yields 401 instead.
- Multi-instance: DB `session_version` is source of truth; per-instance caches may lag briefly until miss → DB read.

### Execution Order

1. U1 — Schema + entity + session validity service + JWT claim + filter check  
2. U2 — Wire revoke on delete / suspend / current-term remove + upload-cache invalidate  
3. U3 — Frontend presence 401 → hard cut logout  
4. U4 — Tests + AGENTS / CONCEPTS  

## Implementation Units

### U1. Session version claim and filter enforcement

- **Goal:** Tokens carry `sv`; filter rejects missing users and version mismatches so revoked sessions fail immediately on any authenticated call.
- **Requirements:** R1, R4, R7
- **Files:** `docs/sql/2026-09-25-user-session-version.sql`, `backend/.../config/SessionVersionSchemaMigrator.java`, `backend/.../model/UserAccount.java`, `backend/.../repository/UserAccountRepository.java`, `backend/.../service/JwtService.java`, `backend/.../service/SessionValidityService.java` (new), `backend/.../security/JwtAuthenticationFilter.java`, `backend/.../controller/AuthController.java`, `backend/.../service/AGENTS.md`, `backend/AGENTS.md`
- **Approach:** Add column + migrator; mint with version; filter validates via `SessionValidityService`.
- **Test scenarios:**
  - Token with matching `sv` authenticates.
  - Token with stale `sv` does not set Authentication (subsequent call 401).
  - Token for deleted/missing email does not set Authentication.
  - Legacy token without `sv` treated as version 0.

### U2. Revoke on admin access actions

- **Goal:** Suspend and remove-from-current-term bump `session_version`; delete leaves no row; upload access cache cleared for that email.
- **Requirements:** R1, R2, R3, R4
- **Files:** `backend/.../service/UserService.java`, `backend/.../service/TermService.java`, `backend/.../service/StudentTermAccessService.java`
- **Approach:** Call bump + cache invalidate from suspend and current-term remove; delete remains purge + delete (filter handles missing).
- **Test scenarios:**
  - `suspendStudent` increments version.
  - `removeStudent` on current term increments version; on non-current does not.
  - After bump, old JWT fails filter check.
  - Re-login after remove mints new `sv` and succeeds (account still active).

### U3. Presence heartbeat hard cut

- **Goal:** Open SPA forces login within ~10s when the session is revoked, without user click.
- **Requirements:** R1, R5, R6
- **Files:** `frontend/src/utils/presence.js`, `frontend/src/components/Footer.jsx`, `frontend/AGENTS.md`
- **Approach:** When Authorization was sent and presence returns 401, clear session and redirect to login; keep ignoring other errors for count display.
- **Test scenarios:** Manual / build: signed-in footer poll with revoked token lands on login. No automated frontend runner.

### U4. Docs and regression coverage

- **Goal:** Durable docs + focused tests for revoke/filter/presence auth behavior.
- **Requirements:** R1–R7
- **Files:** `CONCEPTS.md`, `authorization/.../JwtServiceTest.java`, new unit/support tests for `SessionValidityService` / UserService bump / TermService current-term bump, optional `authorization` filter web test if cheap
- **Approach:** Extend existing Jwt and UserService tests; document session revoke vocabulary.
- **Test scenarios:** Cover U1–U2 automated cases; CONCEPTS entry for forced logout on access revoke.

## Verification Contract

- `mvn test` from `backend/` covers new/changed authorization and support tests.
- `npm run build` from `frontend/` succeeds.
- Manual: login as student → lecturer remove from current term → within ~10s student SPA is on login; re-login yields out-of-term; suspend while online also hard-cuts; delete while online hard-cuts.

## Definition of Done

- [x] R1–R7 satisfied for delete, current-term remove, and suspend
- [x] Filter rejects revoked/missing sessions before controllers run
- [x] Presence Bearer-without-principal returns 401; SPA clears session
- [x] Docs (AGENTS + CONCEPTS + solutions) updated
- [x] Backend focused tests green; frontend build green
