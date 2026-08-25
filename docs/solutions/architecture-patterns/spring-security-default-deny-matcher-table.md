---
title: Spring Security default-deny matcher table with JWT filter and 401/403 handlers
date: 2026-08-25
category: architecture-patterns
module: backend-security
problem_type: architecture_pattern
component: authentication
severity: high
applies_when:
  - "Replacing permitAll or scattered controller role checks with centralized authorization"
  - "Distinguishing 401 unauthenticated from 403 wrong-role in a JWT-backed API"
  - "Ordering lecturer-specific lab paths before broader /api/labs/** matchers"
  - "Dual-role users need URL-scoped student access without Spring role hierarchy"
  - "Testing authorization without loading production controllers or Mockito-mocking domain types"
related_components:
  - testing_framework
tags:
  - spring-security
  - jwt
  - default-deny
  - authorization-matcher
  - "401-vs-403"
  - dual-role
  - api-fetch
  - security-authorization-probes
---

# Spring Security default-deny matcher table with JWT filter and 401/403 handlers

> **Branch note:** This work is on `Kha/Development` (pending merge as of 2026-08-25; no PR number cited). Behavior below is grounded in that branch's tree.

## Context

Before default-deny, Spring Security admitted all requests at the filter level (`anyRequest().permitAll()`) and controllers optionally re-checked JWT roles. A missed controller guard could leave a path reachable without the intended role. The implemented pattern moves route-level authorization into a single `SecurityFilterChain` matcher table, authenticates once in `JwtAuthenticationFilter`, and leaves controllers with identity and data-scoping helpers only (`JwtAuthHelper`).

The request flow is:

1. **Public paths** — `OPTIONS /**`, `GET /`, `/api/auth/**`, and local Swagger paths are `permitAll()`.
2. **JWT filter** — If `Authorization: Bearer …` is present, parse claims and populate `SecurityContextHolder`. Invalid or expired tokens are swallowed (context cleared); the filter always continues the chain.
3. **Matcher table** — Each `requestMatchers(...)` line grants a required role or `authenticated()`.
4. **Denial** — No authentication → **401** via `HttpStatusEntryPoint`. Authenticated but wrong role → **403** via `AccessDeniedHandlerImpl`.
5. **Controller** — Handlers read `JwtUserPrincipal` for user identity and call `JwtAuthHelper` for active-account checks and student scoping, not for route-level role gates.

**Honest scope note:** Lecturer analytics and overview (`/api/analytics/**`, `/api/lecturer/**`) were already lecturer-only in the product contract before this work. `SecurityConfig` centralizes that enforcement at the perimeter. Do **not** document this as discovering and closing a brand-new anonymous analytics hole—the observable change for anonymous callers is *where* denial happens (security-layer 401) rather than introducing lecturer-only semantics for the first time.

## Guidance

### 1. Default-deny matcher table lives in `SecurityConfig`

All non-public routes require a valid JWT. The table ends with `.anyRequest().authenticated()` so nothing falls through open:

```38:57:backend/src/main/java/com/eiu/capstone/backend/config/SecurityConfig.java
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/change-password")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER)
                    .requestMatchers("/api/users/**").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/lecturer/**").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/analytics/**", "/api/master-data/**", "/api/terms/**")
                        .hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/statistics").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions/export").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/students/*/attempts").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/challenges/*/students")
                        .hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/labs/**").hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER)
                    .requestMatchers("/api/submissions/**", "/api/students/**").hasRole(JwtRoleNames.STUDENT)
                    .anyRequest().authenticated())
```

**Matcher order matters.** Lecturer-only `GET` lab paths (`/statistics`, `/submissions`, `/export`, `/attempts`, `/challenges/*/students`) are declared **before** the broader `/api/labs/**` rule that admits `STUDENT` or `LECTURER`. Spring evaluates first match; reversing order would let students reach lecturer analytics paths.

Production Swagger stays closed with `SPRINGDOC_ENABLED=false`; local default remains enabled and unauthenticated via the matchers above.

### 2. JWT filter: authenticate, never throw on bad tokens

`JwtAuthenticationFilter` parses the bearer token and sets `UsernamePasswordAuthenticationToken` with `ROLE_`-prefixed authorities. On `JwtException` or `IllegalArgumentException`, it **clears** the security context and continues—**do not rethrow** from the filter. Throwing would short-circuit the chain and bypass the configured entry point.

```36:53:backend/src/main/java/com/eiu/capstone/backend/security/JwtAuthenticationFilter.java
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseToken(header.substring(7));
                JwtUserPrincipal principal = new JwtUserPrincipal(
                        claims.get("email", String.class),
                        claims.get("irn", String.class),
                        extractRoles(claims));
                Collection<SimpleGrantedAuthority> authorities = principal.roles().stream()
                        .map(name -> new SimpleGrantedAuthority("ROLE_" + name))
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
```

Roles are normalized via `JwtRoleNames.normalize` inside `JwtUserPrincipal` (`TEACHER` → `LECTURER`).

### 3. 401 vs 403: entry point vs access-denied handler

```35:37:backend/src/main/java/com/eiu/capstone/backend/config/SecurityConfig.java
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new AccessDeniedHandlerImpl()))
```

| Situation | HTTP status | Mechanism |
|---|---|---|
| No bearer token, or token failed parsing (empty context) | **401 Unauthorized** | `authenticationEntryPoint` when `authenticated()` or role check finds no principal |
| Valid token, wrong role for path | **403 Forbidden** | `AccessDeniedHandlerImpl` when `hasRole` / `hasAnyRole` fails |

`SecurityAuthorizationTest` encodes this contract: anonymous `/api/lecturer/overview` → 401; student token on same path → 403; anonymous `/api/submissions/my-history` → 401.

### 4. No role hierarchy

`STUDENT` and `LECTURER` are independent. `hasRole(LECTURER)` does **not** satisfy a `hasRole(STUDENT)` rule and vice versa. Dual-role JWTs must include **both** role strings where an endpoint uses `hasAnyRole(STUDENT, LECTURER)`—for example `/api/labs/**` list reads.

Consequences verified in tests:

- Lecturer-only token on `POST /api/submissions/{labId}/{attempt}/upload` → **403** (student-only path).
- Student-only token on `GET /api/labs/{id}/statistics` → **403** (lecturer-only path).
- Token with both `STUDENT` and `LECTURER` on lecturer overview and student my-history → **200**.

### 5. `JwtAuthHelper` is identity-only, not authorization

After the security layer admits a request, controllers use `JwtAuthHelper` for:

- **`requireActiveUser`** — resolve DB user, reject missing/unknown principal (401), inactive account (403).
- **`resolveStudentScope`** — students may only see their own `studentId`; lecturers may supply any id.

```21:30:backend/src/main/java/com/eiu/capstone/backend/security/JwtAuthHelper.java
    public UserAccount requireActiveUser(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        UserAccount user = userAccountRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        return user;
    }
```

Do not reintroduce route-level role checks here; the matcher table is the source of truth for "may this role hit this path."

### 6. SPA: split denial UX — URL routes vs API responses

**Route guard (`RequireRole`):** Wrong role for a React route redirects to the user's default dashboard (lecturer-first for dual-role), not `/no-access`.

```12:14:frontend/src/components/auth/RequireRole.jsx
  if (!hasAnyRole(user.roles, anyOf)) {
    return <Navigate to={defaultDashboardPath(user.roles, user.inCurrentTerm)} replace />;
  }
```

**API wrapper (`apiFetch`):** Global fetch side effects differ by status:

```9:19:frontend/src/utils/apiFetch.js
export async function apiFetch(input, init = {}) {
  const { authHandling = 'gated', ...fetchInit } = init;
  const response = await fetch(input, fetchInit);
  if (authHandling === 'gated') {
    if (response.status === 401) {
      clearSessionAndRedirectToLogin();
    } else if (response.status === 403 && window.location.pathname !== ROUTES.noAccess) {
      window.location.assign(ROUTES.noAccess);
    }
  }
  return response;
}
```

- **401** — session cleared, redirect to login (expired/invalid token).
- **403** — session **kept**, navigate to `/no-access` (authenticated but insufficient API role).
- **`authHandling: 'self'`** — used for change-password so 401/403 stay on the form with field-level messages from `apiError.js`.

`readFriendlyApiError` maps 401 and 403 for non-auth contexts without putting raw backend `message`/`detail` on the UI. It does **not** perform the session redirect; only `apiFetch` does.

### 7. Prove the matcher table with probe controllers

`SecurityAuthorizationTest` uses `@WebMvcTest` with minimal **probe** controllers in `SecurityAuthorizationProbes` that mirror production paths but return stub bodies. Production controllers are not loaded, so tests isolate `SecurityConfig` + `JwtAuthenticationFilter`:

```22:28:backend/src/test/java/com/eiu/capstone/backend/security/SecurityAuthorizationTest.java
@WebMvcTest(controllers = {
        RootController.class,
        SecurityAuthorizationProbes.AuthProbeController.class,
        SecurityAuthorizationProbes.LecturerProbeController.class,
        SecurityAuthorizationProbes.SubmissionProbeController.class,
        SecurityAuthorizationProbes.LabProbeController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
```

Add a probe endpoint before adding a production route when the matcher rule is non-obvious (mixed prefix, method-specific role, dual-role admission). Probes avoid Mockito-mocking domain types that failed to mock in this suite (`pom.xml` compiles the backend for Java 17).

## Why This Matters

- **One auditable table** replaces scattered controller role checks that are easy to omit on new endpoints.
- **Correct HTTP semantics** let the SPA distinguish "sign in again" (401) from "signed in but not allowed" (403) without logging out dual-role users who hit a lecturer-only API from a student bookmark.
- **Filter resilience** — swallowing JWT parse errors prevents malformed tokens from becoming 500s and preserves the 401 entry point for unauthenticated access.
- **Matcher ordering** prevents a broad `hasAnyRole` rule from shadowing a stricter lecturer-only path on the same prefix.
- **No role hierarchy** keeps dual-role behavior explicit: the token must carry every role the path admits; lecturer does not implicitly include student (so lecturer-only accounts still cannot upload).

## When to Apply

- Adding a new API prefix or HTTP method with a different role than its parent path → insert a **more specific** `requestMatchers` line **above** the broader rule in `SecurityConfig`.
- Endpoint needs "own data only" beyond role → keep scoping in `JwtAuthHelper.resolveStudentScope` or handler logic; do not duplicate role gates.
- New frontend API call from a signed-in page → use `apiFetch` unless the screen must handle 401/403 locally (`authHandling: 'self'`).
- New dual-role surface → ensure JWT issuance includes both roles; verify with `SecurityAuthorizationTest`-style cases for both single-role denial and dual-role admission.
- Regressions in 401/403 → check the filter catch block first, then matcher order, then whether the test uses a probe path that matches production.

## Examples

### Anonymous vs student vs dual-role on lecturer overview

| Caller | `GET /api/lecturer/overview` |
|---|---|
| No `Authorization` header | 401 |
| Bearer with `STUDENT` only | 403 |
| Bearer with `STUDENT` + `LECTURER` | 200 |

Source: `SecurityAuthorizationTest.anonymousOverview_is401Not403`, `studentOverview_is403Not401`, `dualRoleOverview_isNotDenied`.

### Lab statistics: specificity before `/api/labs/**`

| Caller | `GET /api/labs/{id}/statistics` |
|---|---|
| Anonymous | 401 (matcher + entry point; no dedicated test named in this suite) |
| `STUDENT` only | 403 (`studentLabStatistics_is403`) |
| `LECTURER` only | admitted by the lecturer-only matcher before `/api/labs/**` (probe exists; this suite does not assert 200) |

`GET /api/labs` with `STUDENT` only → 200 (`studentLabList_isNot403`), because the list rule is the broader `/api/labs/**` matcher.

### Lecturer cannot upload

`POST /api/submissions/{labId}/1/upload` with lecturer-only token → 403 (`lecturerUpload_is403`). Upload paths require `STUDENT` at the security layer; dual-role users include `STUDENT` in the token.

### SPA API 403 without logout

A student-only user who triggers a lecturer API from the SPA gets 403, `apiFetch` navigates to `/no-access`, and the stored token remains so they can return to their dashboard without re-authenticating.

### Change-password stays on the modal

Change-password passes `authHandling: 'self'` so a 401 on that form stays on the modal with a field-level message instead of a global login redirect.

## Related

- Plan: `docs/plans/2026-08-25-001-feat-spring-security-default-deny-plan.md`
- Backend contract: `backend/AGENTS.md` (Security posture)
- Frontend contract: `frontend/AGENTS.md` (`apiFetch`, `RequireRole`, `/no-access`)
- Domain: `CONCEPTS.md` — Dual-role user, default-deny, no-access
- Older plans that still describe `permitAll` plus per-controller JWT guards are historical (`docs/plans/2026-08-08-005-feat-user-management-multi-role-routing-plan.md` and similar); they are not current authorization contracts
- Adjacent learnings (auth surface only): [student-history pagination JPQL](../database-issues/student-history-pagination-nullable-jpql.md), [latest vs highest score](../logic-errors/grading-tab-latest-vs-highest-score.md)
