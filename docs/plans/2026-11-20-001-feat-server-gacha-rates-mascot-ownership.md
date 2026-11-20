# Server gacha rates + mascot ownership roster

## Problem

- Gacha odds and `rollSingle` live in `MascotGachaModal.jsx`; students can tamper via DevTools.
- `POST /crystals/grant` trusts the client delta (gacha reports wins).
- Gacha mascot unlocks sit in `localStorage` only — not in `student_mascot_purchase`.
- Pity is `gacha_demo_pity` in `localStorage`.

## Target

1. **Singleton `gacha_rate_config` (PostgreSQL)** — operator-editable rates; students `GET` for odds UI only.
2. **`POST /api/students/streak/gacha-roll`** — server rolls, spends tickets/points, updates pity/crystals/points, inserts mascot rows.
3. **`student_daily_streak`**: `gacha_pity_count`, `crystal_sourced_points` (exchange anti-loop on server).
4. **Retire student `POST /crystals/grant`** (Admin-only sandbox if needed).
5. **`GET /api/lecturer/mascots/ownership`** — roster from `student_mascot_purchase` (+ user IRN/name).

## Out of scope

- Lecturer UI to edit rates in-app (SQL / migrator seed for now).
- Desktop profile.

## Verification

- Unit: `StudentGachaServiceTest` roll distribution smoke + pity + exchange block.
- Auth: student cannot call grant; lecturer can list ownership.
- Manual: 1x/10x roll, shop exchange → roll, lecturer table shows gacha unlocks.