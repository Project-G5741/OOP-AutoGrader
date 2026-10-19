---
title: Remove Date of Birth; Users Current Quarter - Plan
type: feat
date: 2026-10-19
topic: remove-dob-current-quarter-users
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Remove Date of Birth; Users Current Quarter - Plan

## Goal Capsule

- **Objective:** Remove Date of Birth from the system for privacy, and show **Current Quarter** on the lecturer Users table as the system current-quarter label when that user is enrolled in it (blank otherwise).
- **Product authority:** this plan (Product Contract below); Product Contract preservation: unchanged from the originating brainstorm (Outstanding Questions resolved into Planning Contract KTDs only).
- **Execution profile:** software implementation — operator SQL column drop + backend list DTO with read-time Current Quarter + Users table column swap.
- **Stop conditions:** none outstanding — deferred brainstorm questions resolved in KTDs below.
- **Tail ownership:** implementer runs `mvn test` (backend) and `npm run build` (frontend) before calling units done; operator applies new SQL on Neon/local before or with API deploy that removes the JPA mapping.

## Product Contract

### Summary

Strip Date of Birth from database, backend APIs/models, and frontend. On lecturer User Management, replace the Date of Birth column with **Current Quarter**, derived from existing term enrollment against the system’s current quarter—not a new stored user field.

### Problem Frame

Date of Birth is privacy-sensitive and unused in practice (Users column empty; create/edit UI already omits it). Lecturers still need a useful third column; enrollment in the current quarter is the operational signal they already manage on Quarters.

### Key Decisions

- **Remove Date of Birth completely** (DB column, entity, create/Google upsert inputs, list serialization, frontend mapping/column). *(session-settled: user-directed — chosen over keeping or redacting the field: privacy.)* Governs R1–R3.
- **Users column shows current-quarter-if-enrolled only** — same meaning as student **Enrolled Quarter**; blank when not enrolled in the system current quarter (including lecturers and unenrolled students). *(session-settled: user-directed — chosen over latest enrolled or all enrolled quarters.)* Governs R4–R6.
- **Derive at read time from term enrollment** — no denormalized quarter field on the user row. *(session-settled: user-approved — chosen over a stored user field or client-side join: one source of truth.)* Governs R4, R7.
- **Enrollment remains owned by Quarters** — Users does not edit enrollment. Governs R8.

### Actors

- A1. **Lecturer** — views Users table; manages enrollment on Quarters (unchanged).
- A2. **User list API** — stops exposing Date of Birth; supplies Current Quarter for the Users table.
- A3. **Student / dual-role account** — appears with Current Quarter when enrolled in the system current quarter.

### Requirements

**Privacy removal**

- R1. `date_of_birth` / `dateOfBirth` is removed from durable schema documentation and from the live database (column dropped; existing values discarded).
- R2. Backend models, create/bulk/Google upsert request shapes, and any user list/detail serialization no longer accept or return Date of Birth.
- R3. Frontend User Management and related client code no longer map, display, sort by, or send Date of Birth.

**Users Current Quarter**

- R4. The Users table replaces the former Date of Birth column with a column labeled **Current Quarter**.
- R5. When the listed user is enrolled in the system’s current quarter, the cell shows that quarter’s display label (same label family as student **Enrolled Quarter** / term label).
- R6. When the user is not enrolled in the system’s current quarter—or no current quarter is set—the cell is blank.
- R7. Current Quarter on Users is derived from existing term enrollment and the current-term flag; it is not a separately edited user attribute.
- R8. Lecturers continue to enroll/unenroll students only via Quarters (and existing import flows); Users does not add enrollment controls.

### Key Flows

- F1. **View Users with Current Quarter**
  - **Trigger:** Lecturer opens User Management.
  - **Actors:** A1, A2
  - **Steps:** Load user list; for each row, show Current Quarter per R5–R6; Date of Birth is absent.
  - **Covered by:** R3–R6
- F2. **Enrollment change reflected on Users**
  - **Trigger:** Lecturer enrolls or removes a student from the current quarter on Quarters (or sets a different current quarter).
  - **Actors:** A1, A3
  - **Steps:** After the change, refreshing Users shows or clears Current Quarter for that student without a separate Users edit.
  - **Covered by:** R7, R8

### Acceptance Examples

- AE1. **Covers R5.** Given student S enrolled in the current quarter labeled `2026 — Quarter 1`, when a lecturer opens Users, then S’s Current Quarter cell shows `2026 — Quarter 1` (or the same label the system uses for that term elsewhere).
- AE2. **Covers R6.** Given student T not enrolled in the current quarter, when a lecturer opens Users, then T’s Current Quarter cell is blank.
- AE3. **Covers R6.** Given a lecturer-only account L, when a lecturer opens Users, then L’s Current Quarter cell is blank.
- AE4. **Covers R1–R3.** Given any create, update, Google upsert, or list/detail user payload, when the API or UI is used, then Date of Birth is neither accepted nor displayed.
- AE5. **Covers R7, R8.** Given student S enrolled in the current quarter, when the lecturer removes S from that quarter on Quarters and refreshes Users, then S’s Current Quarter cell becomes blank without editing the user on Users.

### Scope Boundaries

**In scope**

- Drop Date of Birth across DB, backend, and frontend.
- Users table column swap to Current Quarter with current-quarter-if-enrolled semantics.

**Out of scope**

- Editing enrollment from the Users page.
- Showing historical or all enrolled quarters in this column.
- Changing Quarters enrollment UX beyond what Users needs to display.
- Broader PII audit beyond Date of Birth.
- Startup auto-drop migrator for `date_of_birth` (operator SQL only; see KTD1).

### Dependencies / Assumptions

- Term enrollment and a single system current quarter already exist and drive student **Enrolled Quarter**.
- Operator/schema practice remains external SQL (no Flyway in-repo).

### Outstanding Questions

None blocking. Former deferred items resolved in KTD1–KTD5.

### Sources / Research

- Schema: `docs/sql/2026-10-13-schema-baseline-reconstructed.sql` (`user_account.date_of_birth`); drop pattern `docs/sql/2026-08-19-drop-lab-weight.sql`.
- Backend: `UserAccount`, `CreateUserRequest`, `GoogleLoginUpsertRequest`, `UserService` create/Google upsert; `getAllUser` returns `UserAccount`; bootstrap `LecturerTabBootstrapService.users` / `UsersBootstrapResponse`.
- Frontend: `UserTable.jsx` Date of Birth column; `UserManagement.jsx` `dob`/`dateOfBirth` mapping; `UserModal` has no DoB field.
- Quarter label: `StudentTermAccessService.termAccessFor` / `TermService.currentTermLabel` / `buildTermLabel`.
- Prior plan naming DoB as a sortable Users column: `docs/plans/2026-08-12-004-feat-column-header-sort-plan.md` (superseded for this column by this plan).
- Institutional learning: user_account column drops are operator SQL; startup migrators in this repo are typically additive.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Drop `date_of_birth` via operator SQL only** — new `docs/sql/2026-10-19-drop-user-account-date-of-birth.sql` with `ALTER TABLE user_account DROP COLUMN IF EXISTS date_of_birth;`; update reconstructed baseline; no startup `SchemaMigrator` for this drop. Ship JPA field removal in the same release as the operator run. Governs R1.
- KTD2. **List endpoints return a list DTO (not raw `UserAccount`)** — extend `UserDTO.UserResponse` (or a sibling list record) with `String currentQuarter`; wire `GET /api/users/getAllUser` (list + page content) and `UsersBootstrapResponse` through one mapping path so bootstrap and truncation fallback stay shape-identical. Mutations may keep today’s `UserResponse` without inventing enrollment on write. Governs R2, R4–R7.
- KTD3. **Current Quarter cell matches student Enrolled Quarter** — same rule as `StudentTermAccessService.termAccessFor` / `enrolledQuarterName`: label only when the user is **active** and enrolled in the system current term; otherwise blank. Label string from `TermService.currentTermLabel()` / `buildTermLabel`. Governs R5, R6.
- KTD4. **Batch enrollment for list mapping** — one `TermService.currentTermLabel()` (or empty → all blank) plus one repository query for user IDs enrolled in the current term (new method beside `findUserIdsByTermId` / `existsByUser_IdAndTerm_CurrentTrue`); map rows with `active && enrolledIds.contains(id) ? label : null`. Do not call `existsByUser_IdAndTerm_CurrentTrue` per row. Governs R7.
- KTD5. **Frontend column key `currentQuarter`** — replace `dob` in `UserTable` / `normalizeUser`; client-side string sort only (do not `Date.parse` quarter labels). `UserModal` unchanged. Governs R3, R4.

### Assumptions

- Desktop H2 follows the entity after DoB removal (no separate desktop drop script beyond baseline/docs alignment).
- `BulkCreateResult` may keep embedding `UserAccount`; create responses omit DoB after entity change and do not need `currentQuarter` until list refresh.
- Jackson continues to ignore unknown JSON properties on Google upsert if a stale client still sends `dateOfBirth`.
- No automated frontend tests; U4 verification is `npm run build` + manual Users tab check.

### Sequencing

1. U1 — operator SQL + baseline + strip DoB from entity/writes.
2. U2 — list DTO + batch Current Quarter mapping on `getAllUser` / bootstrap.
3. U3 — backend unit tests for mapping + DoB absence.
4. U4 — frontend column + normalize; docs/DOX if contracts change.

### Risks

- **Deploy skew:** new JAR mapping a dropped column, or old JAR after drop — mitigate by shipping entity removal and operator SQL together (KTD1).
- **Dual list APIs:** bootstrap vs `getAllUser` fallback diverge — mitigate with one mapper (KTD2).
- **N+1:** per-user enrollment exists — forbidden by KTD4.
- **Inactive enrolled student:** blank Current Quarter (matches student Enrolled Quarter via KTD3); do not show label for suspended accounts.

### System-Wide Impact

- Privacy: DoB values discarded on drop; list JSON must not leak the field via entity serialization.
- Auth/roles unchanged; Users remains lecturer-gated as today.
- Quarters enrollment ownership unchanged (R8).

### Documentation / Operational Notes

- Operator: run `docs/sql/2026-10-19-drop-user-account-date-of-birth.sql` on Neon and local Postgres before or with the release that removes the JPA column.
- Update `backend/AGENTS.md` / frontend page AGENTS only if they document DoB or Users column contracts.
- `CONCEPTS.md` Term enrollment already describes Users Current Quarter — leave unless wording drifts from KTD3.

---

## Implementation Units

### U1. Drop DoB from schema and write paths

**Goal:** Remove `date_of_birth` from durable schema docs/live DB contract and stop accepting or persisting Date of Birth.

**Requirements:** R1, R2 (write side)

**Dependencies:** none

**Files:**
- `docs/sql/2026-10-19-drop-user-account-date-of-birth.sql` (new)
- `docs/sql/2026-10-13-schema-baseline-reconstructed.sql`
- `backend/src/main/java/model/UserAccount.java`
- `backend/src/main/java/DTO/UserDTO.java` (`CreateUserRequest`)
- `backend/src/main/java/model/GoogleLoginUpsertRequest.java`
- `backend/src/main/java/service/UserService.java`
- `backend/src/main/java/controller/AuthController.java` (call site if signature changes)

**Approach:**
1. Add idempotent operator SQL `DROP COLUMN IF EXISTS date_of_birth` (mirror drop-lab-weight).
2. Remove column from reconstructed baseline `user_account`.
3. Delete entity field/accessors; remove `dateOfBirth` from create/Google request shapes and `UserService` setters / Google upsert parameter.

**Test scenarios:**
- Happy path: create user / Google upsert succeeds with no DoB field.
- Edge case: create request body without `dateOfBirth` still works (already the UI path).

**Verification:** compile + existing `UserServiceTest` adjusted for removed DoB.

---

### U2. List DTO with batched Current Quarter

**Goal:** User list and lecturer users bootstrap expose `currentQuarter` and never serialize DoB.

**Requirements:** R2, R4–R7

**Dependencies:** U1

**Files:**
- `backend/src/main/java/DTO/UserDTO.java` (`UserResponse` or list sibling)
- `backend/src/main/java/service/UserService.java`
- `backend/src/main/java/repository/TermEnrollmentRepository.java`
- `backend/src/main/java/controller/UserController.java`
- `backend/src/main/java/analytics/dto/UsersBootstrapResponse.java`
- `backend/src/main/java/analytics/service/LecturerTabBootstrapService.java`
- `backend/src/main/java/service/TermService.java` (reuse only; no behavior change required)

**Approach:**
1. Add `currentQuarter` to the list response type; factory from `UserAccount` + optional label.
2. Add repository method returning user IDs enrolled where `term.is_current = true` (batch).
3. In one list-mapping method: resolve current label; load enrolled ID set; map each user per KTD3–KTD4.
4. Change `getAllUser` HTTP responses and `UsersBootstrapResponse.content` to that DTO list/page content.

**Test scenarios:**
- Happy path (AE1): active student enrolled in current term → label equals `currentTermLabel()`.
- Edge (AE2): active student not enrolled → null/blank.
- Edge (AE3): lecturer-only → blank.
- Edge: no current term → all blank.
- Edge: inactive user with enrollment row → blank (KTD3).
- Edge: bootstrap and `getAllUser` JSON both include `currentQuarter`, omit `dateOfBirth`.

**Verification:** unit tests in U3; `LecturerTabBootstrapUsersTest` updated if response type changes.

---

### U3. Backend tests for DoB removal and Current Quarter mapping

**Goal:** Lock AE1–AE4 behaviors in backend tests.

**Requirements:** R1, R2, R5, R6

**Dependencies:** U1, U2

**Files:**
- `backend/src/test/java/support/com/eiu/capstone/backend/service/UserServiceTest.java`
- New or extended unit/support test for list Current Quarter mapping
- `backend/src/test/java/unit/com/eiu/capstone/backend/analytics/service/LecturerTabBootstrapUsersTest.java` (if type changes)
- Reference patterns: `StudentTermAccessServiceTest` enrolled-label cases

**Approach:**
1. Remove DoB assertions/setup from `UserServiceTest`.
2. Add list-mapping tests for enrolled / not enrolled / no current term / inactive.
3. Update bootstrap users test expectations for DTO content type.

**Test scenarios:** Same as U2 scenarios; create/Google paths assert no DoB persistence API.

**Verification:** `mvn test` from `backend/`.

---

### U4. Frontend Users column and normalize

**Goal:** Users table shows **Current Quarter** from API; Date of Birth gone from client mapping/sort/display.

**Requirements:** R3, R4

**Dependencies:** U2

**Files:**
- `frontend/src/components/UserTable.jsx`
- `frontend/src/pages/UserManagement.jsx`
- Optional DOX: `frontend/AGENTS.md` / page AGENTS / `docs/USER_GUIDE.md` only if they mention DoB

**Approach:**
1. Replace column `{ key: 'dob', label: 'Date of Birth' }` with `{ key: 'currentQuarter', label: 'Current Quarter' }`.
2. `normalizeUser`: drop `dob`/`dateOfBirth`; map `currentQuarter` from API (blank string when null).
3. Keep client-side sort via existing `sortRows` / accessor; string compare only.
4. Manual: enroll/remove on Quarters, refresh Users (AE5).

**Test scenarios:**
- Happy path: bootstrap users load shows Current Quarter for enrolled active student.
- Edge: truncation fallback `getAllUser` still populates Current Quarter.
- Edge: empty/null `currentQuarter` renders blank cell.

**Verification:** `npm run build` from `frontend/`; manual Users + Quarters check for AE1–AE3, AE5.

---

## Verification Contract

| Gate | Command / action | Proves |
|---|---|---|
| Backend unit/support | `mvn test` from `backend/` | U1–U3; AE1–AE4 logic |
| Frontend build | `npm run build` from `frontend/` | U4 compiles |
| Operator schema | Run `docs/sql/2026-10-19-drop-user-account-date-of-birth.sql` on target DB | R1 live |
| Manual AE | Lecturer Users after Quarters enroll/remove | AE1–AE3, AE5 |

No `release:validate` required for this change. No behavioral skill evaluation.

---

## Definition of Done

**Global**

- [ ] R1–R8 satisfied; AE1–AE5 pass.
- [ ] No `dateOfBirth` / `date_of_birth` / Users `dob` column remains in app code or reconstructed baseline.
- [ ] List + bootstrap JSON expose `currentQuarter`, never DoB.
- [ ] Operator SQL doc exists and has been run (or noted as required) for the deploy target.
- [ ] `mvn test` and `npm run build` green.
- [ ] Abandoned experiment code removed from the diff.
- [ ] DOX pass: update nearest AGENTS only if contracts/docs mention DoB or Users columns; otherwise leave docs unchanged and note why.

**Per unit**

- [ ] U1: entity/requests/SQL/baseline clean of DoB.
- [ ] U2: one mapper; batch enrollment; KTD3 semantics.
- [ ] U3: tests cover enrolled/not/inactive/no-current-term and DoB absence.
- [ ] U4: column + normalize + sort; manual AE5.
