---
title: "Backend JUnit aspect-home packages: package-private access and WebMvcTest configuration search"
date: 2026-08-25
category: conventions
module: backend-tests
problem_type: convention
component: testing_framework
severity: high
applies_when:
  - "Relocating or adding backend JUnit under src/test/java aspect homes with packages like unit.com.eiu.capstone.backend..."
  - "Calling package-private production members from tests"
  - "Writing `@WebMvcTest` after tests no longer live under the production package tree"
  - "Running `mvn test` in Docker without Postgres"
symptoms:
  - "`mvn test-compile` failed: tests could not call package-private production members"
  - "`@WebMvcTest` failed with Unable to find a @SpringBootConfiguration"
  - "Docker image must run the same suite without Postgres"
root_cause: scope_issue
resolution_type: test_fix
related_components:
  - tooling
  - development_workflow
tags:
  - junit
  - maven-surefire
  - aspect-homes
  - package-private
  - webmvctest
  - spring-boot-configuration
  - docker-test
  - no-postgres
---

# Backend JUnit aspect-home packages: package-private access and WebMvcTest configuration search

## Context

Backend JUnit tests in this repo live under **aspect-root homes**: the first-level folders under `backend/src/test/java` named `unit`, `integration`, `authorization`, `regression`, and `support`. Each test's Java package **starts with that home name**, then mirrors the production type path. Production code stays in `com.eiu.capstone.backend...`. Those are **not the same package**.

That split is already how the current tree is laid out. Authorization slices declare `package authorization.com.eiu.capstone.backend.security` (`backend/src/test/java/authorization/com/eiu/capstone/backend/security/SecurityAuthorizationTest.java:1`). Pipeline integration declares `package integration.com.eiu.capstone.backend.pipeline` (`backend/src/test/java/integration/com/eiu/capstone/backend/pipeline/SubmissionPipelineIntegrationTest.java:1`). Unit grading tests declare `package unit.com.eiu.capstone.backend.grading` (`backend/src/test/java/unit/com/eiu/capstone/backend/grading/MmdParserTest.java:1`). The production facade they exercise is `package com.eiu.capstone.backend.grading` (`backend/src/main/java/com/eiu/capstone/backend/grading/MmdParser.java:1`).

The Spring Boot application class that `@WebMvcTest` would normally discover by walking parent packages is annotated `@SpringBootApplication` on `EiuCapstoneBackendApplication` (`backend/src/main/java/com/eiu/capstone/backend/EiuCapstoneBackendApplication.java:7-9`). An aspect-root test package is **not** a child of `com.eiu.capstone.backend`, so that walk never finds it.

This learning is the convention that emerged while making the five-home layout compile and run. The work is **pending on `Kha/Development` (uncommitted as of 2026-08-25)**; do not treat it as merged or as "fixed in commit X."

## Guidance

Keep tests in the aspect homes. **Do not move tests back under `com.eiu...`** to regain package-private access or Boot scanning. Import production types; treat the home prefix as a deliberate package boundary.

**1. Visibility: widen production members that tests must call -- do not relocate tests.**

Java package-private methods and constructors are visible only to types in the **same** package. Aspect-root tests are not in `com.eiu.capstone.backend...`, so they cannot call package-private production members. When a test needs a production entry point, make that member `public` with **no behavior change**. Leave members that tests do not need package-private.

Grounded examples in the current tree:

- `MmdParser.parse(String)` is `public` (`backend/src/main/java/com/eiu/capstone/backend/grading/MmdParser.java:37-40`). Unit tests in `unit.com.eiu.capstone.backend.grading` can call it.
- `GradingResultStore.save(...)` is `public` (`backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java:61-68`).
- The same class still has package-private `loadExisting(...)` (`backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java:42-58`). That is only visible to other types in `com.eiu.capstone.backend.grading`, not to `unit.*` / `support.*` tests. If a test later needs it, widen that method to `public` rather than moving the test.

Pipeline tests already depend on public orchestration APIs: `SubmissionStorageService.processUpload(...)` (`backend/src/main/java/com/eiu/capstone/backend/service/SubmissionStorageService.java:111`) and `GradingPipeline.gradeChallenge(...)` (`backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java:50`).

**2. `@WebMvcTest` cannot find `EiuCapstoneBackendApplication` by walking packages.**

On an authorization slice, nest a **static inner class** (for example `AuthorizationSliceApp`) annotated `@SpringBootApplication` that **excludes** `DataSourceAutoConfiguration` and `HibernateJpaAutoConfiguration`, and `@Import` the security stack plus `RootController`. Keep `RootController` in both `@WebMvcTest(controllers = ...)` and `@Import` so the slice includes the live mapping `@GetMapping("/")` returning `"ok"` (`backend/src/main/java/com/eiu/capstone/backend/controller/RootController.java:10-12`). Omitting `RootController` from `@Import` left `GET /` as 404 in this session's slice runs.

The current pattern is in `SecurityAuthorizationTest.java:27-47`: `@WebMvcTest` lists `RootController` and the probe controllers; `@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, RootController.class})`; nested `AuthorizationSliceApp` with `@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })`. `anonymousRoot_is200` asserts `GET /` is 200 (`SecurityAuthorizationTest.java:102-105`).

**3. Local JDK 23 Mockito / Byte Buddy vs Docker JDK 17.**

Surefire on the backend POM sets `-Dnet.bytebuddy.experimental=true` so local JDK 23 can mock JPA entities (`backend/pom.xml:96-100`, comment at line 98). The Docker **build** stage is `maven:3.9.4-eclipse-temurin-17` (`backend/Dockerfile:2`) and runs `mvn -B test package` (`backend/Dockerfile:10`) -- tests run in the image on JDK 17. Keep the Surefire `argLine` so developer machines on a newer JDK still run `mvn test`; do not assume Docker's JDK 17 makes the flag optional for local runs.

**4. Pipeline integration is in-process, not `@SpringBootTest` / Postgres.**

`SubmissionPipelineIntegrationTest` constructs `SubmissionStorageService` and `GradingPipeline` in the test (`SubmissionPipelineIntegrationTest.java:136-154`), calls `processUpload` then `gradeChallenge` (`SubmissionPipelineIntegrationTest.java:67-77`, `86-96`, `104-116`), and loads zip-shaped fixtures from `backend/src/test/resources/integration/` via the classloader (`SubmissionPipelineIntegrationTest.java:165-170`, resources such as `backend/src/test/resources/integration/happy/2331200082_Nguyen_Van_A_lab_1/challenge_1/Animal.java`). There is no `@SpringBootTest` and no database in this class. New pipeline coverage should follow that harness, not a full application context.

## Why This Matters

Aspect homes make `mvn test` and Docker `mvn -B test package` report **what kind of check failed** (unit vs authorization vs integration) without mixing them into one production-shaped tree. That only works if the Java package matches the folder, which **breaks** the two defaults Spring and javac otherwise give you: same-package access and `@SpringBootApplication` discovery from the test package upward.

If someone "fixes" compile errors by moving tests under `com.eiu.capstone.backend...`, the five-home layout is gone and CI/Docker still compile whatever is on the classpath -- the contract in root `AGENTS.md` (tests under those five homes) is violated. If someone "fixes" `@WebMvcTest` by pointing at the real `EiuCapstoneBackendApplication` without excluding JPA, the slice pulls a DataSource. If `RootController` is omitted from `@Import`, this session saw liveness `GET /` return 404 even when security matchers were correct. If Surefire lacks Byte Buddy experimental, **local** `mvn test` on JDK 23 fails on entity mocks while Docker JDK 17 stays green -- a split-brain that wastes debugging time.

Widening a constructor or method to `public` without changing behavior is the cheap, local fix. Relocating tests or standing up Postgres for pipeline checks is the expensive wrong fix.

## When to Apply

- Adding or moving a test under `backend/src/test/java/{unit,integration,authorization,regression,support}/`.
- A test fails to compile with "method/constructor is not visible" against production code that used to be package-private.
- A new `@WebMvcTest` (or similar slice) fails because it cannot find a `@SpringBootApplication`, tries to auto-configure Hibernate/DataSource, or `GET /` returns 404.
- Mockito/Byte Buddy errors when mocking JPA entities on a **local JDK newer than 17**, while Docker still uses Temurin 17.
- Covering compile → grade without wanting `@SpringBootTest` or a real Postgres.

## Examples

**Package names (do not "correct" the prefix away).**

Aspect-root tests:

```
package authorization.com.eiu.capstone.backend.security;  // SecurityAuthorizationTest.java:1
package integration.com.eiu.capstone.backend.pipeline;    // SubmissionPipelineIntegrationTest.java:1
package unit.com.eiu.capstone.backend.grading;            // MmdParserTest.java:1
```

Production:

```
package com.eiu.capstone.backend.grading;  // MmdParser.java:1
```

Import production types (`import com.eiu.capstone.backend.grading.*;` in `MmdParserTest.java:3`); keep the test package on the home prefix.

**Visibility: publicize the API tests call.**

`parse` is public so `unit.*` tests can use it (`MmdParser.java:37`). `save` is public (`GradingResultStore.java:61`). `loadExisting` remains package-private (`GradingResultStore.java:42`) until a cross-package test needs it -- then widen, do not nest the test in `com.eiu.capstone.backend.grading`.

**Authorization slice: nested Boot app + explicit `@Import`.**

Match `SecurityAuthorizationTest.java:27-47`: exclude DataSource/HibernateJpa on the nested `@SpringBootApplication`; `@Import` `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, and `RootController`. Keep `RootController` in both `controllers` and `@Import` so `anonymousRoot_is200` (`SecurityAuthorizationTest.java:102-105`) hits `RootController.root()` (`RootController.java:10-12`).

**Surefire vs Docker JDK.**

Local: `backend/pom.xml:99` `<argLine>-Dnet.bytebuddy.experimental=true</argLine>`. Image: `backend/Dockerfile:2` Temurin 17, `backend/Dockerfile:10` `RUN mvn -B test package`.

**Pipeline integration: services + classpath fixtures.**

`happyPathCompilesThenGradesClassPillar` (`SubmissionPipelineIntegrationTest.java:64-80`) calls `processUpload` with `integration/happy/.../Animal.java`, then `gradeChallenge`. Compile-fail and fatal-MMD cases use `integration/compile-fail` and `integration/mmd-fail` the same way (`SubmissionPipelineIntegrationTest.java:83-119`). No Spring context, no Postgres.

## Related

- [Spring Security default-deny matcher table](../architecture-patterns/spring-security-default-deny-matcher-table.md) -- same `@WebMvcTest` probe surface; test file paths in that doc may still cite `backend/src/test/java/com/eiu/capstone/backend/security/` (removed by this layout change; tests now live under `backend/src/test/java/authorization/`).
- [In-memory per-challenge compile path](../architecture-patterns/in-memory-challenge-compile-path.md) -- production compile path exercised by in-process pipeline tests.
- [Duplicate key on submission result re-upload](../database-issues/submission-result-reupload-duplicate-key.md) -- natural topic for the `backend/src/test/java/regression/` home; Docker `mvn test` still must not require live Postgres.
