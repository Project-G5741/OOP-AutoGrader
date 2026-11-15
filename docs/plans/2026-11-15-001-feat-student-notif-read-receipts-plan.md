---
title: Student Notification Read Receipts - Plan
type: feat
date: 2026-11-15
topic: student-notif-read-receipts
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections; deferred Outstanding Questions resolved into KTDs
---

# Student Notification Read Receipts - Plan

## Goal Capsule

- **Objective:** Persist student notification “marked as read” state across tab close and re-login by storing durable read-receipt keys server-side, without materializing a notification inbox.
- **Product authority:** This Product Contract.
- **Open blockers:** None.
- **Stop conditions:** Do not materialize a notification inbox. Do not change feedback-reply consume-on-leave. Do not mount the bell on desktop. Do not change notification key builders in a way that breaks R3 without a migration story.
- **Execution:** Code. `ce-work` owns the shipping tail.
- **Product Contract preservation:** Unchanged — planning adds HOW sections; deferred Outstanding Questions resolved into KTDs.

## Product Contract

### Summary

Replace session-only read tracking on the web student notification bell with durable **read receipts**: when a student marks an item read, the system stores that item’s stable key for that student forever. Notifications remain client-computed from labs and feedback replies; only marked-read keys are persisted.

### Problem Frame

Today the bell stores read ids in `sessionStorage`. Closing the tab clears them, so the red dot returns on the next login even though the student already dismissed those items.

### Key Decisions

- **Read-receipt keys, not a full inbox** — persist only `(student, notification key)` when marked read; keep notification bodies computed. (session-settled: user-approved — chosen over JSON bag on user / materialized notification rows: DB-efficient and matches computed notifs.) Governs R1, R2, R5.
- **Stable lab keys stay read across countdown day changes** — one dismiss of a lab’s countdown covers the whole countdown window even as the title changes day to day. (session-settled: user-directed — chosen over day-scoped re-alert.) Governs R3.
- **Forever durability** — a marked-read key remains read across sessions and devices for that student until product scope changes. Governs R1, R4.

### Actors

- **A1. Student (web)** — opens the bell, marks items read, expects unread state to survive logout/tab close.
- **A2. System** — loads that student’s read keys and records new keys on mark-read; does not invent notification content.

### Key Flows

- F1. Load bell with durable unread state
  - **Trigger:** Student opens the web dashboard (bell mounts).
  - **Actors:** A1, A2
  - **Steps:** System loads the student’s stored read keys; client builds current notifications; unread = present items whose keys are not in the receipt set; red dot reflects unread.
  - **Covered by:** R1, R2, R4

- F2. Mark one item read permanently
  - **Trigger:** Student clicks a notification row in the bell.
  - **Actors:** A1, A2
  - **Steps:** Client marks that id read locally; system upserts the receipt key for that student; later sessions still treat that key as read.
  - **Covered by:** R1, R3, R4, R5

### Requirements

**Durability**

- R1. When a student marks a notification read, that notification’s stable key is stored server-side for that student and remains read after tab close, logout, and re-login.
- R2. The system does not persist notification title/body/severity as inbox rows; only read-receipt keys are stored.
- R3. Lab countdown keys use the existing stable per-lab identity so a single dismiss stays read for that lab’s countdown for the whole countdown window (including day-to-day title changes).
- R4. Unread indicators (bell and row red dots) are derived from current notifications minus the student’s stored read keys.

**Unchanged product surface**

- R5. Opening the bell alone does not mark items read; click-to-mark-one behavior remains.
- R6. Desktop continues to omit the notification bell.
- R7. Feedback-reply consume-on-leave (hard-delete shown replies on pagehide/logout) remains unchanged.

### Acceptance Examples

- AE1. Persist across re-login
  - **Covers:** R1, R4
  - **Given:** Student marks a lab countdown notification read and closes the tab
  - **When:** They log in again later with that lab still in the countdown window
  - **Then:** That countdown item is listed without unread styling and the bell has no red dot for it

- AE2. Countdown day change stays read
  - **Covers:** R3
  - **Given:** Student marked `deadline-countdown-{labId}` read when it said “3 days”
  - **When:** The next calendar day the same lab still shows a countdown (e.g. “2 days”)
  - **Then:** The item remains read (no red dot for that key)

- AE3. New logical notification can be unread
  - **Covers:** R4
  - **Given:** Student has read receipts only for keys they dismissed
  - **When:** A different stable key appears (e.g. a new lab’s countdown, or deadline-expired after countdown ends)
  - **Then:** That new key shows as unread until clicked

### Scope Boundaries

- In scope: durable read receipts for web student bell; lab-derived and feedback-reply keys as computed today.
- Out of scope: desktop bell; push/email; changing notification generation rules; changing feedback reply consume-on-leave; mark-all-on-open; materializing a notification inbox; pruning orphan receipt keys (v1).

### Dependencies / Assumptions

- DA1. Existing client notification ids (`deadline-countdown-{labId}`, `deadline-expired-{labId}`, `no-submit-{labId}`, `feedback-reply-{id}-{responseAt}`) remain the receipt keys.
- DA2. Feedback replies may still vanish on leave per R7; durable receipts for those keys matter mainly while the reply is still listed.

## Planning Contract

### Key Technical Decisions

- **KTD1. Composite receipt table** — `student_notification_read (user_id, notif_key)` with PK `(user_id, notif_key)`, `ON DELETE CASCADE` to `user_account`, and `read_at TIMESTAMPTZ NOT NULL`. Clone the `student_mascot_purchase` embeddable-key pattern. (session-settled product approach A; HOW: table over JSON bag.) Governs R1, R2.
- **KTD2. Student API under `/api/students/notification-reads`** — `@Profile("!desktop & !desktop-lecturer")`. `GET /list` returns the caller’s keys; `POST /mark` upserts one key (idempotent). Stay under `/api/students/**` so SecurityConfig already gates `STUDENT|ADMIN` (do not hang on `/api/feedback/**`). Governs R1, R4, R6.
- **KTD3. Keep client key builders unchanged** — reuse ids from `frontend/src/utils/studentNotifications.js` as `notif_key` values (especially `deadline-countdown-{labId}` for R3). Governs R3, DA1.
- **KTD4. Optimistic mark-read** — on row click, update local `readIds` immediately, then `POST /mark`; on failure remove that id from the local set and toast via `readFriendlyApiError` / `toFriendlyError` (MascotShop pattern). (session-settled: user-approved at plan scoping.) Governs F2, R1.
- **KTD5. No pruning in v1** — orphan keys (labs gone / replies consumed) stay; storage stays tiny. Deferred follow-up only if needed.
- **KTD6. Cap key length** — `notif_key VARCHAR(191)` (or similar ≤255) is enough for current id shapes; reject blank / oversized keys with 400.

### High-Level Technical Design

```text
[StudentNotificationBell]
  mount → GET /api/students/notification-reads/list → Set(readIds)
  click → local Set add → POST /mark { key }
  build notifs ← studentNotifications.js (unchanged ids)
  unread = notifs.filter(n => !readIds.has(n.id))

[StudentNotificationReadController] (!desktop)
  → StudentNotificationReadService
  → StudentNotificationReadRepository (JPA)
  → student_notification_read

Feedback GET/DELETE /replies + feedbackReplyConsume.js — untouched (R7)
```

### Assumptions

- `/api/students/**` matcher already covers the new controller; no SecurityConfig change unless path leaves that prefix.
- Desktop never mounts the bell, so no H2 migrator is required for this table.
- Theme `localStorage` is unrelated; receipts are server-side for cross-device durability.

### Implementation Constraints

- Controllers thin; logic in `service/`.
- Startup migrator + operator SQL mirror under `docs/sql/`; register FQCN in `SchemaMigratorCatalogTest`.
- DOX closeout: `backend/AGENTS.md`, `frontend/src/components/student/AGENTS.md`, `docs/sql/README.md`, root Child DOX index for the new SQL file.
- Prefer `JwtAuthHelper.requireActiveUser`; own-data only (principal’s `user_id`).

### Sequencing

1. U1 Backend persistence + API + tests
2. U2 Frontend bell wiring
3. U3 Docs / DOX closeout

### Sources & Research

- Pattern clone: `StudentMascotPurchase` / `StudentMascotPurchaseKey` / `StudentStreakSchemaMigrator` (`student_mascot_purchase`)
- API style: `FeedbackController`, `StudentStreakController` (`!desktop & !desktop-lecturer`, `JwtAuthHelper`)
- Security: `SecurityConfig` — `/api/students/**` → STUDENT|ADMIN
- Frontend: `StudentNotificationBell.jsx` sessionStorage today; toast via `MascotShop.jsx` + `apiError.js`
- Learnings: no `docs/solutions/` on notification read-state; follow JWT matcher + idempotent migrator patterns in `docs/solutions/architecture-patterns/spring-security-default-deny-matcher-table.md`

## Implementation Units

### U1. Backend receipt store and API

- **Goal:** Persist and expose per-student read keys.
- **Requirements:** R1, R2, R6
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/model/StudentNotificationRead.java`
  - `backend/src/main/java/com/eiu/capstone/backend/model/StudentNotificationReadKey.java`
  - `backend/src/main/java/com/eiu/capstone/backend/repository/StudentNotificationReadRepository.java`
  - `backend/src/main/java/com/eiu/capstone/backend/service/StudentNotificationReadService.java`
  - `backend/src/main/java/com/eiu/capstone/backend/controller/StudentNotificationReadController.java`
  - `backend/src/main/java/com/eiu/capstone/backend/DTO/` (list + mark request/response records)
  - `backend/src/main/java/com/eiu/capstone/backend/config/StudentNotificationReadSchemaMigrator.java`
  - `docs/sql/2026-11-15-student-notification-read.sql`
  - `backend/src/test/java/unit/.../service/StudentNotificationReadServiceTest.java`
  - `backend/src/test/java/unit/.../config/SchemaMigratorCatalogTest.java` (add FQCN)
  - `backend/src/test/java/authorization/.../controller/StudentNotificationReadAuthorizationTest.java`
- **Approach:** Follow KTD1–KTD2, KTD5–KTD6. Migrator `CREATE TABLE IF NOT EXISTS` with `@Profile("!desktop & !desktop-lecturer")`. Controller: `GET /list`, `POST /mark`. Service: list by `user_id`; upsert if missing (idempotent). Authorization test: student 200, lecturer 403, anonymous 401.
- **Test scenarios:**
  - List returns empty for a user with no rows
  - Mark inserts a key; second mark of same key is idempotent
  - List returns only that user’s keys
  - Blank or oversized key → 400
  - Student JWT OK; lecturer JWT 403; anonymous 401
  - SchemaMigratorCatalogTest includes the new migrator
- **Verification:** `mvn test` from `backend/` covering the new unit + authorization tests
- **Dependencies:** None

### U2. Frontend durable read wiring

- **Goal:** Replace `sessionStorage` with API-backed read state and optimistic mark-read.
- **Requirements:** R1, R3, R4, R5, R7
- **Files:**
  - `frontend/src/components/student/StudentNotificationBell.jsx`
  - `frontend/src/utils/studentNotifications.js` (read-only unless a tiny helper export is needed; do not change key formats)
  - Leave untouched: `frontend/src/utils/feedbackReplyConsume.js`, Feedback reply fetch/consume paths
- **Approach:** On mount (web only), `GET .../notification-reads/list` seeds `readIds`. `markOneRead` applies KTD4. Remove `READ_STORAGE_KEY` / sessionStorage helpers. Keep click-to-mark-one and feedback consume-on-leave.
- **Test scenarios:**
  - Manual AE1: mark countdown → close tab → re-login → still read
  - Manual AE2: next calendar day same lab countdown stays read
  - Manual AE3: new lab key shows unread
  - Persist failure: row returns to unread + error toast
  - `npm run build` succeeds
- **Verification:** `npm run build` from `frontend/`; manual web bell checks for AE1–AE3
- **Dependencies:** U1

### U3. DOX and operator SQL catalog

- **Goal:** Document the new persistence surface and contracts.
- **Requirements:** R2, R6, R7 (documentation of unchanged/new boundaries)
- **Files:**
  - `backend/AGENTS.md` (API table + persistence bullet)
  - `frontend/src/components/student/AGENTS.md` (bell row: server receipts, not sessionStorage)
  - `docs/sql/README.md` (inventory row)
  - `AGENTS.md` Child DOX Index (new SQL path)
  - Optional: `docs/USER_GUIDE.md` §3.4 if it still claims open-bell marks all / session-only (align with R5 + durable reads)
- **Approach:** DOX pass only; no behavior change.
- **Test scenarios:** Docs match shipped endpoints and bell behavior; no contradictory sessionStorage claim left for read state.
- **Verification:** Manual DOX review of touched AGENTS paths
- **Dependencies:** U1, U2

## Verification Contract

- Backend: `mvn test` from `backend/` (at least U1 unit + authorization + SchemaMigratorCatalogTest)
- Frontend: `npm run build` from `frontend/`
- Manual web: AE1, AE2, AE3 on student dashboard bell
- Desktop: confirm bell still omitted (no regression expected)

## Definition of Done

- Global: R1–R7 satisfied; sessionStorage read store removed from the bell; U1–U3 complete; abandoned experimental code removed from the diff.
- U1: table/migrator/API live; tests green; idempotent mark.
- U2: durable load + optimistic mark; feedback consume unchanged; build green.
- U3: AGENTS / sql README / Child DOX index updated; USER_GUIDE aligned if needed.
