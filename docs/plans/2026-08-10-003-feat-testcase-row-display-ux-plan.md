---
title: Testcase Row Display UX - Plan
type: feat
date: 2026-08-10
topic: testcase-row-display-ux
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Testcase Row Display UX - Plan

## Goal Capsule

- **Objective:** Simplify the student Testcase tab so pass/fail is scannable at a glance, with detail expansion only on failed checks.
- **Product authority:** This plan owns the Testcase tab row layout and interaction in the student challenge results panel (`StudentUI.jsx`). Class tab, MMD tab, lecturer views, and history pages are not active scope.
- **Open blockers:** None — ready for implementation.

## Product Contract

### Summary

Each structural testcase row shows the check name, a full-row pass/fail background tint, and green **PASS** or red **FAIL** text at the trailing edge. Failed rows alone are clickable to expand feedback details. Check/X icons and SUCCESS/ERROR badges are removed from testcase rows.

**Product Contract preservation:** Unchanged — planning adds HOW sections only.

### Problem Frame

The current Testcase tab repeats status four times per row — icon, SUCCESS/ERROR badge, PASS/FAIL label, and expand affordance on every row. Students scanning a long rubric must parse redundant signals before finding what to fix. Passing rows do not need detail, yet they invite clicks and show chevrons like failing rows.

### Key Decisions

- **Remove redundant status chrome on testcase rows** — drop the leading ✓/✗ icon (`Tick`) and the SUCCESS/ERROR `StatusBadge`; keep only trailing **PASS** / **FAIL** text. Governs R1, R2.
- **Expand details only on failed rows** — passing rows are non-interactive with no chevron; failing rows toggle an inline detail panel. Governs R3, R4.
- **Full-row background tint by outcome** — each row gets a subtle green tint when passed and red tint when failed, reusing the app's existing pass/fail surface colors (light and dark mode). Governs R5.
- **Testcase tab only** — Class and MMD tabs keep their current icon/badge patterns until separately requested. Governs scope boundary.

### Actors

- A1. **Student** — reviews structural testcase results after upload or when revisiting a graded challenge on the student dashboard.

### Requirements

- R1. On the Testcase tab, each visible testcase row omits the leading pass/fail icon.
- R2. On the Testcase tab, each visible testcase row omits the SUCCESS and ERROR status badges.
- R3. On the Testcase tab, a passing row shows green **PASS** text at the row's trailing edge and is not clickable — no chevron, no hover affordance implying expansion.
- R4. On the Testcase tab, a failing row shows red **FAIL** text at the trailing edge, a chevron indicating expand/collapse, and toggles a detail section on click showing the existing feedback content (error message / student output fields already wired from grading).
- R5. Each testcase row applies a full-width background tint: subtle green for pass, subtle red for fail, with readable contrast for row text in both light and dark themes.
- R6. The Testcase Score header and score pill at the top of the tab are unchanged.
- R7. Hidden testcase rows (if any remain in the data model) keep their current non-interactive treatment and are not restyled by this change.

### Key Flows

- F1. **Student scans passing checks**
  - **Trigger:** Student opens the Testcase tab on a challenge with mostly passing structural checks.
  - **Actors:** A1
  - **Steps:** Student sees a list of green-tinted rows with check names and trailing **PASS**; no icons, badges, or expand controls appear on those rows.
  - **Covered by:** R1–R3, R5

- F2. **Student investigates a failure**
  - **Trigger:** Student sees one or more red-tinted rows with **FAIL**.
  - **Actors:** A1
  - **Steps:** Student clicks a failing row; detail panel expands with grading feedback; student clicks again to collapse.
  - **Covered by:** R4, R5

### Acceptance Examples

- AE1. Given a passing structural check "Field speed is private int", the row shows the name, green background tint, and trailing green **PASS** — no icon, no SUCCESS badge, no chevron, and clicking does nothing.
- AE2. Given a failing check "Constructor Car(int, String) exists", the row shows the name, red background tint, trailing red **FAIL**, and a chevron; clicking reveals the feedback text from grading.
- AE3. Given a mix of pass and fail rows, a student can visually separate outcomes by row color without reading every label.

### Scope Boundaries

- **In scope:** Testcase tab row layout and interaction in `frontend/src/components/student/StudentUI.jsx`.
- **Out of scope:** Class tab and MMD tab row styling; lecturer submission drawer; `StudentHistoryPage` testcase display; backend grading or testcase payload shape changes.

### Success Signals

- Students can identify failing checks by row color and **FAIL** label without redundant icons or badges.
- Passing rows no longer suggest expandable detail.
- No regression to testcase score calculation or the score pill display.

---

## Planning Contract

### Key Technical Decisions

- **KTD1. Split row wrapper by pass/fail** — passing rows render as a static `<div>`; failing rows render as a `<button type="button">` with `onClick` toggling `expandedTC`. Governs U1, R3, R4.
- **KTD2. Reuse existing Tailwind pass/fail surface tokens** — row tints mirror the expanded-panel output blocks already in `StudentUI.jsx`: pass `bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800/40`; fail `bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/40`. Governs U1, R5.
- **KTD3. Scope `Tick` / `StatusBadge` removal to testcase tab only** — shared helpers stay; Class and MMD tab call sites unchanged. Governs U1, R1, R2.
- **KTD4. Keep existing three-column expanded detail panel for failures** — Input / Expected Output / Your Output markup preserved; only the expand trigger and row chrome change. Governs U1, R4.

### Technical Design

Current testcase row (lines ~630–670 in `StudentUI.jsx`) wraps every `tc.isExample` row in a `<button>` with `Tick`, `StatusBadge`, PASS/FAIL label, and chevron for all rows.

Target structure per row:

```text
Pass row:  [div, tinted green]  check name ················· PASS
Fail row:  [button, tinted red] check name ········· FAIL  ⌄
           [expanded panel when expandedTC === tc.id]
```

`expandedTC` state already exists; gate expansion with `!tc.passed && expandedTC === tc.id`. On pass rows, skip `setExpandedTC` entirely and use `cursor-default` without hover background change.

### Assumptions

- Structural testcase feedback remains in `tc.studentOutput` / `tc.feedback` from `mapStructuralTestcases` in `StudentDashboard.jsx` — no payload changes needed.
- Hidden (`!tc.isExample`) rows stay on the existing lock/opacity treatment.

### Sequencing

U1 only — single frontend unit.

---

## Implementation Units

### U1. Restyle testcase rows for scan-first pass/fail UX

**Covers:** R1–R7, F1–F2, AE1–AE3

**Files:**
- `frontend/src/components/student/StudentUI.jsx`
- `frontend/src/components/student/AGENTS.md` (note testcase row interaction rule)

**Work:**
- In the `activeTab === 'testcase'` map loop (~line 630), branch each `tc.isExample` row:
  - **Pass:** render outer wrapper as `<div>` with green tint classes (KTD2); show check name + green **PASS** only; omit `Tick`, `StatusBadge`, chevron, and `onClick`.
  - **Fail:** render outer wrapper as `<button type="button">` with red tint classes; show check name + red **FAIL** + chevron; `onClick` toggles `expandedTC`.
- Render expanded detail panel only when `tc.isExample && !tc.passed && expandedTC === tc.id`.
- Leave hidden rows (`!tc.isExample`) unchanged.
- Leave Testcase Score header, `ScorePill`, and `testScore` computation unchanged.

**Test scenarios (manual — no frontend automated test suite today):**
- Challenge with all-pass testcases: every row green-tinted, **PASS** at end, no chevrons, rows not focusable/clickable.
- Challenge with at least one fail: fail rows red-tinted, **FAIL** + chevron; click expands feedback panel; second click collapses.
- Mixed list: pass and fail rows visually distinct by background without reading labels.
- Class and MMD tabs: still show ✓/✗ icons and existing expand behavior — no regression.
- Dark mode: row text readable on tinted backgrounds.

---

## Verification Contract

| Check | Command / action |
|---|---|
| Frontend build | `npm run build` from `frontend/` |
| Manual — passing rows | Open Testcase tab on a mostly-passing challenge; confirm no icons, badges, or chevrons on green rows |
| Manual — failing rows | Confirm red rows expand on click and show feedback; collapse on second click |
| Manual — score header | Confirm Testcase Score pill unchanged |
| Manual — sibling tabs | Spot-check Class and MMD tabs still use icon/badge pattern |
| Manual — dark mode | Toggle theme; confirm row tints and text contrast |

---

## Definition of Done

- [ ] Testcase pass rows: green tint, **PASS** label only, non-interactive (R1–R3, R5)
- [ ] Testcase fail rows: red tint, **FAIL** label, chevron, expand/collapse feedback (R4, R5)
- [ ] No SUCCESS/ERROR badges or leading icons on testcase rows (R1, R2)
- [ ] Score header and pill unchanged (R6)
- [ ] Hidden testcase rows unchanged (R7)
- [ ] Class/MMD tabs unaffected
- [ ] `npm run build` passes
- [ ] `frontend/src/components/student/AGENTS.md` documents testcase expand-on-fail-only rule
