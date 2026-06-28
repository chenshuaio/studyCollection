# Mistake Mastery Status Design

## Goal

Let users update a mistake record from pending reinforcement to mastered, and change it back when they want to continue practicing it.

## Scope

- Add a backend endpoint that updates a mistake record status by `userId` and `questionId`.
- Add a frontend API client method.
- Add a button on the mistake book page to switch between `PENDING` and `MASTERED`.
- Refresh the mistake list after status updates.

## Behavior

- `POST /mistakes/status` accepts `userId`, `questionId`, and `status`.
- The backend returns the updated `MistakeRecord`.
- If a user marks a pending mistake as mastered, the page shows the localized mastered status.
- If a user marks a mastered mistake as pending, the page shows it as pending reinforcement again.
- The existing "重新练习" link remains available.

## Non-Goals

- No mistake filtering in this slice.
- No dedicated mistake-only practice session in this slice.
- No persistent MySQL mistake table in this slice; this follows the existing in-memory mistake service.

## Testing

- Backend controller test verifies status update after recording a mistake.
- API client test verifies `POST /api/mistakes/status`.
- Mistake page test verifies the status button calls the API and refreshes the list.
