# Mistake Filter Summary Design

## Goal

Improve the mistake book so users can quickly focus on weak knowledge points and mastery status.

## Scope

- Add client-side filtering by knowledge point.
- Add client-side filtering by mastery status.
- Show summary counts for all mistakes, pending reinforcement, and mastered mistakes.
- Keep the existing re-practice link and status toggle action.

## Behavior

- The mistake page computes unique knowledge points from the loaded mistake list.
- Selecting a knowledge point filters the visible mistake table.
- Selecting `PENDING` or `MASTERED` filters by mastery status.
- The summary counts always reflect the full loaded mistake list, not the currently filtered subset.
- If no filtered mistakes remain, the table shows an empty-state row.

## Non-Goals

- No backend filtering in this slice.
- No mistake-only practice session in this slice.
- No date or question-type filtering until the mistake data model stores those fields.

## Testing

- Frontend page test verifies summary counts.
- Frontend page test verifies combined knowledge point and status filtering.
