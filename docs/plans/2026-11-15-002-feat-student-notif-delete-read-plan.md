---
title: Student Notification Delete Read - Plan
type: feat
date: 2026-11-15
topic: student-notif-delete-read
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged — planning adds HOW sections; deferred Outstanding Questions resolved into KTDs
---

# Student Notification Delete Read - Plan

## Goal Capsule

- **Objective:** Let web students permanently remove read notifications from the bell (per-item and Clear all read) so those keys never reappear for that student.
- **Product authority:** This Product Contract.
- **Open blockers:** None.
- **Stop conditions:** Do not materialize a notification inbox. Do not change feedback-reply consume-on-leave. Do not mount the bell on desktop. Do not change notification key builders. Do not auto-hide read items without an explicit delete.
- **Execution:** Code. `ce-work` owns the shipping tail.
- **Product Contract preservation:** Unchanged — planning adds HOW sections; deferred Outstanding Questions resolved into KTDs.

<!-- ce-section: work-relationships -->
## How This Work Fits Together

This plan owns **permanent delete of read student notifications**.
It extends durable read receipts from `docs/plans/2026-11-15-001-feat-student-notif-read-receipts-plan.md` and does not reopen that plan’s mark-read contract.

- Student notification read receipts (`docs/plans/2026-11-15-001-feat-student-notif-read-receipts-plan.md`)
  - **Depends on:** Mark-read and durable read keys already exist; delete applies only after an item is read.
  - **Shares:** Same computed notification keys and web-only bell surface.

## Product Contract

### Summary

Add durable **dismissed** keys for the web student notification bell.
Students can delete individual read items or clear all currently listed read items; dismissed keys stay hidden forever even when the underlying lab or reply condition still applies.
Unread items stay click-to-mark-read; notifications remain client-computed.

### Problem Frame

Read receipts keep dimmed rows in the list after mark-read.
Students need a way to remove those read items so the bell is not cluttered by notifications they have already acknowledged.

### Key Decisions

- **Dismissed forever, not snooze** — a deleted key never returns for that student while that key would still be computed. (session-settled: user-directed — chosen over re-show-while-condition-holds: permanent delete means gone.) Governs R3, R4.
- **Per-item + Clear all read** — both a row delete control and a header Clear all read action. (session-settled: user-directed — chosen over per-item-only or clear-all-only.) Governs R5, R6.
- **Delete only when read** — unread rows cannot be deleted; mark-read remains the first step. Governs R2, R5.
- **Dismissed overlay, not inbox rows** — persist only dismiss keys (and keep existing read receipts); do not materialize notification bodies. Governs R1, R7.
- **Approach A over auto-hide-all-read** — keep dimmed read rows until an explicit delete. (session-settled: user-approved — chosen over Approach B auto-hide every read item: preserves current read UX.) Governs R2.

### Actors

- **A1. Student (web)** — opens the bell, marks items read, deletes read items or clears all read.
- **A2. System** — stores dismiss keys for that student and returns them so the client can hide matching computed items.

### Key Flows

- F1. Hide dismissed items on load
  - **Trigger:** Student opens the web dashboard (bell mounts).
  - **Actors:** A1, A2
  - **Steps:** System loads that student’s dismiss keys; client builds current notifications; listed items exclude any key in the dismiss set; unread styling still uses read receipts among remaining items.
  - **Covered by:** R1, R3

- F2. Delete one read item
  - **Trigger:** Student uses the delete control on a listed read row.
  - **Actors:** A1, A2
  - **Steps:** Client removes that item from the list immediately; system records the key as dismissed for that student; later sessions never show that key again.
  - **Covered by:** R2, R3, R5

- F3. Clear all read
  - **Trigger:** Student chooses Clear all read while the bell lists one or more read items.
  - **Actors:** A1, A2
  - **Steps:** Client removes every currently listed read item; system records each of those keys as dismissed; unread listed items remain.
  - **Covered by:** R3, R6

### Requirements

**Dismiss behavior**

- R1. The system persists dismissed notification keys per student without storing notification title/body/severity as inbox rows.
- R2. Only notifications that are already read (and currently listed) may be deleted; unread items have no delete path.
- R3. Once a key is dismissed for a student, that key never appears again in that student’s bell, even if the underlying computed condition still holds.
- R4. Different stable keys for the same lab (for example countdown vs expired) remain independent: dismissing one does not dismiss the other.

**Controls**

- R5. Each listed read row offers a delete control that dismisses that one key.
- R6. The bell offers Clear all read, which dismisses every currently listed read key and leaves listed unread items untouched.

**Unchanged product surface**

- R7. Notifications stay client-computed from labs and feedback replies; durable state remains keys only (read receipts plus dismissals).
- R8. Opening the bell alone does not mark items read or dismiss them.
- R9. Desktop continues to omit the notification bell.
- R10. Feedback-reply consume-on-leave (hard-delete shown replies on pagehide/logout) remains unchanged.

### Acceptance Examples

- AE1. Forever hide after delete
  - **Covers:** R3, R5
  - **Given:** Student marked a lab countdown notification read, then deleted it
  - **When:** They reopen the bell later while that lab is still in the countdown window
  - **Then:** That countdown key is absent from the list

- AE2. Clear all read leaves unread
  - **Covers:** R6
  - **Given:** The bell lists two read items and one unread item
  - **When:** Student chooses Clear all read
  - **Then:** The two read items disappear and stay gone; the unread item remains listed and unread

- AE3. Sibling key can still appear
  - **Covers:** R4
  - **Given:** Student dismissed `deadline-countdown-{labId}`
  - **When:** That lab later produces `deadline-expired-{labId}`
  - **Then:** The expired key can appear as a new unread item until marked read / dismissed

### Scope Boundaries

- In scope: durable dismiss for web student bell; per-item delete on read rows; Clear all read for currently listed read keys; forever-hide semantics.
- Out of scope: desktop bell; push/email; deleting unread items; auto-hiding all read without an explicit delete; materializing a notification inbox; changing notification generation rules; changing feedback-reply consume-on-leave; pruning orphan dismiss keys (v1).

### Dependencies / Assumptions

- DA1. Durable read receipts from `docs/plans/2026-11-15-001-feat-student-notif-read-receipts-plan.md` are already in place (or land first).
- DA2. Existing client notification ids remain the dismiss keys (same stable shapes as read receipts).
- DA3. Clear all read targets only keys present in the current listed set that are read—not every historical read receipt in storage.

## Planning Contract

### Key Technical Decisions

- **KTD1. Separate dismiss table** — `student_notification_dismiss (user_id, notif_key)` with PK `(user_id, notif_key)`, `ON DELETE CASCADE` to `user_account`, and `dismissed_at TIMESTAMPTZ NOT NULL`. Clone `student_notification_read` / embeddable-key pattern; do not add a status column on the read table. (session-settled: user-approved at plan scoping — chosen over status-on-read-table: keeps read vs dismiss orthogonal and matches existing migrator style.) Governs R1, R3, R7.
- **KTD2. API under `/api/students/notification-reads`** — reuse the same web-only controller (`@Profile("!desktop & !desktop-lecturer")`). Add `GET /dismissed/list`, `POST /dismiss` (one key), `POST /dismiss/batch` (keys array). Stay under `/api/students/**` so SecurityConfig already gates `STUDENT|ADMIN`. (session-settled: user-approved at plan scoping — batch Clear all over N single posts.) Governs R1, R5, R6, R9.
- **KTD3. Server requires an existing read receipt before dismiss** — reject dismiss of a key that is not already in `student_notification_read` for that user (400). Client still hides delete controls on unread rows (R2). Reuse the same key normalize/prefix rules as mark-read (`StudentNotificationReadService.normalizeAndValidate` or a shared helper). Governs R2.
- **KTD4. Optimistic UI, no Clear-all confirm** — per-row delete and Clear all update local dismissed set immediately, then POST; on failure roll back the local set and toast via `readFriendlyApiError` / `toFriendlyError`. No confirmation dialog. (session-settled: user-approved at plan scoping — chosen over confirm-step: matches mark-read optimism.) Governs F2, F3, R5, R6.
- **KTD5. Keep client key builders unchanged** — reuse ids from `frontend/src/utils/studentNotifications.js` as dismiss keys. Governs R4, DA2.
- **KTD6. No pruning in v1** — orphan dismiss keys stay; storage stays tiny.
- **KTD7. Cap key length / batch size** — `notif_key VARCHAR(191)`; blank/oversized/disallowed keys → 400; batch body rejects empty list and caps at 50.

### High-Level Technical Design

```text
[StudentNotificationBell]
  mount → GET .../notification-reads/list → Set(readIds)
       → GET .../notification-reads/dismissed/list → Set(dismissedIds)
  visible = computed.filter(n => !dismissedIds.has(n.id))
  unread among visible = !readIds.has
  row trash (read only) → local dismissed add → POST /dismiss { key }
  Clear all read → local add all listed-read keys → POST /dismiss/batch { keys }
  (no confirm dialog)

[StudentNotificationReadController] (!desktop)
  existing GET /list, POST /mark
  + GET /dismissed/list, POST /dismiss, POST /dismiss/batch
  → StudentNotificationDismissService (or extend ReadService carefully)
  → student_notification_dismiss

Feedback GET/DELETE /replies + feedbackReplyConsume.js — untouched (R10)
```

### Assumptions

- Read-receipt entities, migrator, and `/mark` already ship on this branch (DA1).
- `/api/students/**` matcher already covers the controller; no SecurityConfig change.
- Desktop never mounts the bell. Match the read-receipt clone: `@Profile("!desktop & !desktop-lecturer")` on migrator/service/controller only; leave JPA entities unprofiled (desktop ddl-auto may create an unused H2 table the same way as read).
- Clear-all sends only currently listed read keys (DA3), not the full historical read-key set from the server.

### Implementation Constraints

- Controllers thin; logic in `service/`.
- Startup migrator + operator SQL mirror under `docs/sql/`; register FQCN in `SchemaMigratorCatalogTest`.
- DOX closeout: `backend/AGENTS.md`, `frontend/src/components/student/AGENTS.md`, `docs/sql/README.md`, root Child DOX index for the new SQL file; `CONCEPTS.md` already defines Notification dismiss.
- Prefer `JwtAuthHelper.requireActiveUser`; own-data only (principal’s `user_id`).
- Prefer lucide trash icon consistent with other compact controls; keep severity strip layout (no nested cards).

### Sequencing

1. U1 Backend dismiss persistence + API + tests
2. U2 Frontend bell delete + Clear all
3. U3 Docs / DOX closeout

### Sources & Research

- Clone: `StudentNotificationRead*` model/key/repo/service/controller/migrator + `docs/sql/2026-11-15-student-notification-read.sql`
- Key validation: `StudentNotificationReadService.normalizeAndValidate` / `ALLOWED_PREFIXES`
- Frontend: `frontend/src/components/student/StudentNotificationBell.jsx` (optimistic mark-read + toast)
- Prior plan: `docs/plans/2026-11-15-001-feat-student-notif-read-receipts-plan.md`
- Vocabulary: `CONCEPTS.md` — Student notification (computed), Notification read receipt, Notification dismiss

## Implementation Units

### U1. Backend dismiss store and API

- **Goal:** Persist and expose per-student dismiss keys; enforce read-before-dismiss.
- **Requirements:** R1, R2, R3, R5, R6, R9
- **Files:**
  - `backend/src/main/java/com/eiu/capstone/backend/model/StudentNotificationDismiss.java`
  - `backend/src/main/java/com/eiu/capstone/backend/model/StudentNotificationDismissKey.java`
  - `backend/src/main/java/com/eiu/capstone/backend/repository/StudentNotificationDismissRepository.java`
  - `backend/src/main/java/com/eiu/capstone/backend/service/StudentNotificationDismissService.java` (or shared helper with ReadService for validation)
  - `backend/src/main/java/com/eiu/capstone/backend/controller/StudentNotificationReadController.java` (add dismiss routes)
  - `backend/src/main/java/com/eiu/capstone/backend/DTO/` (dismissed keys DTO; dismiss / dismiss-batch request records)
  - `backend/src/main/java/com/eiu/capstone/backend/config/StudentNotificationDismissSchemaMigrator.java`
  - `docs/sql/2026-11-15-student-notification-dismiss.sql`
  - `backend/src/test/java/unit/.../service/StudentNotificationDismissServiceTest.java`
  - `backend/src/test/java/unit/.../config/SchemaMigratorCatalogTest.java` (add FQCN)
  - `backend/src/test/java/authorization/.../controller/StudentNotificationReadAuthorizationTest.java` (extend for dismiss routes)
- **Approach:** Follow KTD1–KTD3, KTD6–KTD7. Migrator `CREATE TABLE IF NOT EXISTS` with `@Profile("!desktop & !desktop-lecturer")`. Service: list dismiss keys; dismiss one (require read receipt exists, upsert dismiss idempotent); batch dismiss (validate each key, skip already dismissed, reject if any key fails validation / missing read — prefer fail-fast 400 for bad keys). Authorization: student 200, lecturer 403, anonymous 401.
- **Test scenarios:**
  - List dismissed returns empty when none
  - Dismiss after mark-read inserts; second dismiss is idempotent
  - Dismiss without prior read receipt → 400
  - Batch dismisses multiple listed-read keys omitted from the request stay untouched; any key lacking a read receipt → 400 (fail-fast)
  - Blank, oversized, or disallowed-prefix key → 400; empty batch → 400
  - List returns only that user’s dismiss keys
  - Student JWT OK; lecturer 403; anonymous 401 on new routes
  - SchemaMigratorCatalogTest includes the new migrator
- **Verification:** `mvn test` from `backend/` covering the new unit + authorization cases
- **Dependencies:** None (assumes DA1 read-receipt code present)

### U2. Frontend delete and Clear all

- **Goal:** Hide dismissed keys forever; expose per-row delete on read items and Clear all read.
- **Requirements:** R2, R3, R4, R5, R6, R8, R10
- **Files:**
  - `frontend/src/components/student/StudentNotificationBell.jsx`
  - Leave untouched unless a tiny filter helper is warranted: `frontend/src/utils/studentNotifications.js` (do not change key formats)
  - Leave untouched: `frontend/src/utils/feedbackReplyConsume.js`
- **Approach:** On mount (web), also `GET .../dismissed/list` → `dismissedIds`. Filter notifications before render. Show trash only when `readIds.has(id) && !dismissed`. Clear all read visible when any listed item is read; on click apply KTD4 with batch POST of those keys. Stop row click from firing when pressing trash (separate control). Keep mark-read and feedback consume-on-leave.
- **Test scenarios:**
  - Manual AE1: delete read countdown → reopen → absent
  - Manual AE2: Clear all read removes only read rows; unread remains
  - Manual AE3: dismissed countdown does not block expired sibling key
  - Persist failure: dismissed rows reappear + error toast
  - Unread rows have no trash; Clear all hidden when no listed read items
  - `npm run build` succeeds
- **Verification:** `npm run build` from `frontend/`; manual web bell checks for AE1–AE3
- **Dependencies:** U1

### U3. DOX and operator SQL catalog

- **Goal:** Document dismiss persistence and bell controls.
- **Requirements:** R7, R9, R10 (documentation of unchanged/new boundaries)
- **Files:**
  - `backend/AGENTS.md` (API table + persistence bullet for dismiss)
  - `frontend/src/components/student/AGENTS.md` (bell: dismiss + Clear all; durable dismissed keys)
  - `frontend/src/pages/AGENTS.md` (API mapping rows for dismiss endpoints if that table lists reads)
  - `docs/sql/README.md` (inventory row)
  - `AGENTS.md` Child DOX Index (new SQL path)
  - Optional: `docs/USER_GUIDE.md` if notification section needs delete wording
- **Approach:** DOX pass only; `CONCEPTS.md` Notification dismiss already present from brainstorm.
- **Test scenarios:** Docs match shipped endpoints and bell behavior; no claim that read items cannot be removed.
- **Verification:** Manual DOX review of touched AGENTS paths
- **Dependencies:** U1, U2

## Verification Contract

- Backend: `mvn test` from `backend/` (at least U1 unit + authorization + SchemaMigratorCatalogTest)
- Frontend: `npm run build` from `frontend/`
- Manual web: AE1, AE2, AE3 on student dashboard bell
- Desktop: confirm bell still omitted (no regression expected)

## Definition of Done

- Global: R1–R10 satisfied; U1–U3 complete; abandoned experimental code removed from the diff.
- U1: dismiss table/migrator/API live; read-before-dismiss enforced; tests green; idempotent dismiss + batch.
- U2: durable dismissed load; optimistic per-row delete + Clear all (no confirm); feedback consume unchanged; build green.
- U3: AGENTS / sql README / Child DOX index updated; USER_GUIDE aligned if needed.
