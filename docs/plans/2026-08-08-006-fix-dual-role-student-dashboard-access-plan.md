---
title: "fix: Dual-role users can access student dashboard by URL"
date: 2026-08-08
type: fix
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# fix: Dual-role users can access student dashboard by URL - Plan

## Goal Capsule

**Objective:** Restore dual-role dashboard access so authenticated users with both `STUDENT` and `LECTURER` roles can open `/student-dashboard` and `/student-history` by URL and use those pages without being redirected to `/lecturer-dashboard`. Post-login routing continues to send dual-role users to the lecturer dashboard first.

**Product authority:** Session brainstorm (2026-08-08). Supersedes the broken runtime behavior relative to the already-documented contract in `docs/plans/2026-08-08-005-feat-user-management-multi-role-routing-plan.md` (AE4, F3).

**Stop conditions:** Do not add an in-app role switcher, change lecturer-first login default, or expand backend API authorization scope beyond what is needed to ensure login responses include complete role data.

---

## Product Contract

### Actors

- A1. **Dual-role user** — authenticated account with both `STUDENT` and `LECTURER` in session roles.
- A2. **Student-only user** — unchanged; still blocked from lecturer routes.
- A3. **Lecturer-only user** — unchanged; still blocked from student routes.

### Requirements

- R1. A dual-role user who navigates to `/student-dashboard` or `/student-history` sees the student UI and remains on that URL.
- R2. A dual-role user who navigates to any `/lecturer-*` route sees the lecturer UI when they have the `LECTURER` role.
- R3. After login, dual-role users are still routed to `/lecturer-dashboard` (lecturer-first default).
- R4. Student-only users who open a lecturer route are redirected to `/student-dashboard`.
- R5. Lecturer-only users who open a student route are redirected to `/lecturer-dashboard`.
- R6. The signed-in session used by route guards must include every role assigned to the user at login time (both `STUDENT` and `LECTURER` when applicable).
- R7. Role checks for route access treat `TEACHER` as equivalent to `LECTURER` (existing normalization preserved).

### Key Flows

- F1. **Dual-role URL access** — User with `[STUDENT, LECTURER]` logs in → lands on `/lecturer-dashboard` → enters `/student-dashboard` in the address bar → student dashboard loads and stays loaded.
- F2. **Dual-role history** — Same user opens `/student-history` → student history view loads.
- F3. **Wrong-role guard unchanged** — Student-only user opens `/lecturer-users` → redirected to `/student-dashboard`.

### Acceptance Examples

- AE1. Dual-role user with roles `["STUDENT", "LECTURER"]` in session visits `/student-dashboard` → no redirect to lecturer dashboard; student main view renders.
- AE2. Same user visits `/student-history` → student history view renders.
- AE3. Same user visits `/lecturer-grading` → lecturer grading view renders.
- AE4. Same user logs in → browser navigates to `/lecturer-dashboard`.
- AE5. Student-only user visits `/student-dashboard` → renders; visits `/lecturer-dashboard` → redirected to `/student-dashboard`.
- AE6. Lecturer-only user visits `/lecturer-dashboard` → renders; visits `/student-dashboard` → redirected to `/lecturer-dashboard`.

### Scope Boundaries

**In scope:** Frontend route guard logic (`RequireRole`, `authRoutes`), session role persistence after login, any backend login/auth response fix needed so dual-role users receive a complete `roles` array, targeted tests for role guard behavior, AGENTS.md updates if behavior contract changes.

**Deferred:** In-app role switcher or dashboard picker, changing post-login default for dual-role users, securing non-user APIs, automated E2E browser tests.

**Outside this product's identity:** User Management UI for assigning roles (already shipped in plan 005), student self-service password change, Google OAuth role policy.

### Key Decisions

- KD1. **URL access fix only** (session-settled) — dual-role users reach student areas by direct URL; no new navigation affordance.
  Governs R1, R2.

- KD2. **Lecturer-first login preserved** (session-settled) — post-login default stays `/lecturer-dashboard` when `LECTURER` is present.
  Governs R3.

- KD3. **Redirect on denied route unchanged** (session-settled) — wrong-role access still sends users to their default dashboard, not an access-denied page.
  Governs R4, R5.

---

## Planning Contract

### Summary

The intended behavior from plan 005 is already coded: `RequireRole` should allow `/student-dashboard` when `STUDENT` is in `sessionStorage.user.roles`, and `defaultDashboardPath` should only redirect dual-role users away from student routes when `STUDENT` is absent. The bug is therefore a **data-shape or normalization gap** — not a missing route. Fix by centralizing role normalization (string and `{ name }` object shapes), writing normalized roles at login, and verifying the backend login response includes the full role set for dual-role accounts.

**Product Contract preservation:** Unchanged from brainstorm.

### Key Technical Decisions

- KTD1. **Centralize role normalization in `authRoutes.js`** — add `normalizeRoleList(roles)` that accepts `string[]`, `{ name: string }[]`, or mixed input, uppercases names, maps `TEACHER` → `LECTURER`, and deduplicates. Use it in `hasRole`, `defaultDashboardPath`, and export for `RequireRole`. Matches the defensive pattern already used in `UserManagement.jsx` for API role objects.
- KTD2. **Normalize at login write** — in `App.jsx` `handleLoginSuccess`, normalize `data.roles` before `sessionStorage.setItem('user', …)` and `setUser`. Ensures guards and post-login redirect read the same canonical shape.
- KTD3. **DRY session reader** — extract `readStoredUser()` from `App.jsx` / `RequireRole.jsx` into `authRoutes.js` (or `utils/sessionUser.js`) so both paths parse roles identically.
- KTD4. **Backend only if repro shows incomplete roles** — `AuthController` already maps `userAccount.getRoles()` to uppercase strings and `findByStudentCodeOrTeacherCode` uses `@EntityGraph(attributePaths = "roles")`. Add a focused test only if manual repro shows missing `STUDENT` in the login JSON despite both roles in DB.
- KTD5. **No new test framework** — project has no frontend test runner; add pure-function coverage via a minimal Node-runnable test file only if implementer can run it without new deps, otherwise rely on the manual matrix below.

### Assumptions

- Dual-role users have both roles persisted in `user_role` (plan 005 U2 shipped).
- Stale sessions missing `STUDENT` are fixed by re-login after normalization lands; no migration of existing `sessionStorage` beyond the next login.
- `RequireRole` reading `sessionStorage` directly (not React state) is intentional and stays.

### Risks & Dependencies

| Risk | Mitigation |
|---|---|
| Normalization masks a backend bug returning only one role | Manual repro: inspect login JSON before coding; add backend test if incomplete |
| Object-shaped roles in session from legacy code paths | `normalizeRoleList` handles `{ name }` entries |
| Dual `hasAnyRole` implementations drift | Delete duplicate in `RequireRole.jsx`; import shared helpers |

### High-Level Technical Design

```mermaid
flowchart TD
  Login[POST /api/auth/login] --> Normalize[handleLoginSuccess normalizes roles]
  Normalize --> Session[sessionStorage.user.roles string array]
  Session --> RR[RequireRole reads session]
  RR --> Check{hasRole STUDENT?}
  Check -->|yes| StudentUI[StudentDashboard renders]
  Check -->|no| Default[Navigate defaultDashboardPath]
  Default -->|LECTURER present| LecturerUI[/lecturer-dashboard]
```

---

## Implementation Units

### U1. Centralize role normalization helpers

**Goal:** One canonical role parser for guards, login, and default-dashboard logic.

**Requirements:** R1, R6, R7, AE1–AE3

**Dependencies:** None

**Files:**
- `frontend/src/utils/authRoutes.js`
- `frontend/src/components/auth/RequireRole.jsx`
- `frontend/src/App.jsx`

**Approach:**
1. Add `normalizeRoleList(roles)` — return `string[]` of uppercase role names; accept string or `{ name }` elements; map `TEACHER` → `LECTURER`.
2. Update `hasRole` and add exported `hasAnyRole(userRoles, requiredRoles)` using normalized lists.
3. Move `readStoredUser()` into `authRoutes.js`; validate `roles` normalizes to a non-empty array (or treat empty as unauthenticated).
4. Simplify `RequireRole.jsx` to import shared helpers; remove local duplicates.

**Patterns to follow:** `UserManagement.jsx` role name extraction (`role.name?.toUpperCase()`).

**Test scenarios (manual or runnable):**
- `normalizeRoleList(['STUDENT', 'LECTURER'])` → both present.
- `normalizeRoleList([{ name: 'Student' }, { name: 'Lecturer' }])` → `['STUDENT', 'LECTURER']`.
- `hasAnyRole(['STUDENT', 'LECTURER'], ['STUDENT'])` → true.
- `hasAnyRole(['LECTURER'], ['STUDENT'])` → false.
- `defaultDashboardPath(['STUDENT', 'LECTURER'])` → `/lecturer-dashboard`.

**Verification:** Manual matrix in Verification Contract.

---

### U2. Normalize roles at login

**Goal:** Session always stores complete, canonical roles after auth.

**Requirements:** R3, R6, AE4

**Dependencies:** U1

**Files:**
- `frontend/src/App.jsx`

**Approach:**
1. In `handleLoginSuccess`, build user payload with `roles: normalizeRoleList(data.roles)`.
2. Persist normalized payload to `sessionStorage` and `setUser`.
3. `navigate(defaultDashboardPath(normalizedRoles))` unchanged in behavior.

**Test scenarios:**
- Dual-role login response → `sessionStorage.user.roles` is `['STUDENT', 'LECTURER']` (order may vary).
- Lecturer-only login → `['LECTURER']`.

**Verification:** Browser devtools → Application → sessionStorage after login.

---

### U3. Backend verification (conditional)

**Goal:** Confirm login API returns all roles for dual-role accounts; fix only if repro fails.

**Requirements:** R6

**Dependencies:** U1 (for frontend-first repro)

**Files (only if needed):**
- `backend/src/main/java/com/eiu/capstone/backend/controller/AuthController.java`
- `backend/src/test/java/com/eiu/capstone/backend/service/UserServiceTest.java` (or new auth integration test)

**Approach:**
1. Repro with a dual-role test user: `POST /api/auth/login` → assert `roles` contains both `STUDENT` and `LECTURER`.
2. If incomplete: ensure `authenticateByIrn` path loads roles (EntityGraph already on `findByStudentCodeOrTeacherCode`); check for transactional/lazy-load issues outside the repository call.
3. Add regression test only when a backend defect is confirmed.

**Verification:** curl/Swagger login for dual-role user; optional `mvn test` for new test.

---

### U4. Documentation pass

**Goal:** DOX matches fixed behavior.

**Requirements:** (supporting)

**Dependencies:** U1, U2

**Files:**
- `frontend/AGENTS.md`
- `frontend/src/pages/AGENTS.md`

**Approach:** Confirm dual-role URL access note remains accurate; mention normalized role storage if relevant.

**Verification:** Read updated AGENTS sections.

---

## Verification Contract

| Check | Command / action |
|---|---|
| Frontend build | `cd frontend && npm run build` |
| Manual — dual-role URL | Login as dual-role user → `/lecturer-dashboard` → navigate to `/student-dashboard` → stays on student UI |
| Manual — dual-role history | Same user → `/student-history` → history renders |
| Manual — dual-role lecturer | Same user → `/lecturer-grading` → lecturer UI renders |
| Manual — student-only guard | Student-only → `/lecturer-users` → `/student-dashboard` |
| Manual — lecturer-only guard | Lecturer-only → `/student-dashboard` → `/lecturer-dashboard` |
| Manual — login default | Dual-role login → lands on `/lecturer-dashboard` |
| Backend (conditional) | Login JSON includes both roles for dual-role DB user |

---

## Definition of Done

- [ ] AE1–AE6 pass in manual testing
- [ ] `npm run build` succeeds
- [ ] Shared role normalization used by `RequireRole`, `defaultDashboardPath`, and login
- [ ] No in-app role switcher added
- [ ] AGENTS.md updated if contracts changed

---

## How This Work Fits Together

This fix closes a gap against the multi-role routing work in plan 005. User management, dual IRN, and lecturer JWT guards remain as implemented; only dual-role student route access is in active scope.
