# Report From Mistakes Design

## Goal

Make the learning report page generate analysis from the signed-in user's real mistake-book data instead of hardcoded sample results.

## Scope

- Load current user's mistake records on the report page.
- Derive report input from mistake status:
  - `PENDING` means the knowledge point is still weak, so `correct: false`.
  - `MASTERED` means the user has reinforced it, so `correct: true`.
- Show lightweight report inputs summary: mistake count and pending weak-point count.
- Prevent report generation when there is no mistake data.

## Non-Goals

- No backend aggregation endpoint in this slice.
- No time-series trend chart in this slice.
- No export/download report in this slice.

## Testing

- Report page test verifies it fetches current user's mistakes and sends derived results to `POST /api/reports/learning`.
- Report page test verifies empty mistake data shows a no-data prompt and does not call report generation.
