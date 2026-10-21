# Ranking

## Purpose

Shared live weekly ranking table for student and lecturer Ranking screens.

## Ownership

| File | Role |
|---|---|
| `RankingBoard.jsx` | Leaderboard layout: Ranking, Fullname (initials avatar), IRN, Total, Gift chip; top 20 only, 10 per page; signed-in student row below the table |

## Local Contracts

- Data is passed in (`weekStart`, `weekEnd`, `rows`); parents fetch
- Table shows ranks 1–20 only; pagination is 10 rows per page
- Column labels: Ranking, Fullname, IRN, Total, Gift
- Gift `0` displays as a muted `—` chip; positive gifts as `N pts` (example: `100 pts`)
- Totals use `formatNumber` (floor)
- Client-side header sort applies to the top 20; default Ranking ascending; sort resets to page 1
- The ranking card fills remaining viewport height down to the footer. Extra space is inside the card, below the table; pagination and **Your ranking** stay at the bottom of the card. Lecturer omits `viewerUserId` and the footer
- No previous-week movement arrows (v1 has no history)

## Work Guidance

- Keep presentational; do not fetch here

## Verification

- Manual: student Ranking highlights the signed-in row; lecturer Ranking lists the same live board

## Child DOX Index

No child docs.
