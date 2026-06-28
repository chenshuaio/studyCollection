# Admin Delete Question Design

## Goal

Allow administrators to remove questions from the formal question bank in the local Java learning platform.

## Scope

- Add a backend delete endpoint for formal questions.
- Support deletion in both in-memory and MySQL question repositories.
- Add a frontend API client method.
- Show a delete action in question-bank management for administrators.
- Refresh the current question list after deletion.

## Behavior

- `DELETE /questions/{id}` removes the question with that id.
- The endpoint returns the deleted id in the standard API response.
- The question-bank page asks for browser confirmation before deleting.
- After deletion succeeds, the page reloads the current filtered question list.
- If deletion fails, the page shows the backend error message or a localized fallback.

## Non-Goals

- No soft delete or restore flow.
- No schema migration.
- No audit trail in this slice.

## Testing

- Backend controller test verifies a deleted question no longer appears in search.
- Frontend API test verifies the `DELETE /api/questions/{id}` request.
- Frontend page test verifies the admin delete action calls the API and refreshes the list.
