# Mistake Mastery Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a usable mistake-book mastery status update flow.

**Architecture:** Extend the existing in-memory `MistakeService` with status updates keyed by `userId + questionId`, expose it through `MistakeController`, and wire the Vue mistake page through the existing API client.

**Tech Stack:** Java 21, Spring Boot, Vue 3, TypeScript, Vitest.

---

### Task 1: Backend Status Update

**Files:**
- Modify: `backend/mistake-service/src/test/java/com/studycollection/mistake/api/MistakeControllerTest.java`
- Create: `backend/mistake-service/src/main/java/com/studycollection/mistake/api/UpdateMistakeStatusRequest.java`
- Modify: `backend/mistake-service/src/main/java/com/studycollection/mistake/app/MistakeService.java`
- Modify: `backend/mistake-service/src/main/java/com/studycollection/mistake/api/MistakeController.java`

- [x] Write a failing test that records a mistake, updates it to `MASTERED`, and verifies the listed record has `MASTERED`.
- [x] Run `mvn -pl mistake-service test -Dtest=MistakeControllerTest` and verify the failure is caused by missing status update support.
- [x] Add `UpdateMistakeStatusRequest`.
- [x] Add `MistakeService.updateStatus(userId, questionId, status)` and `MistakeController.updateStatus(...)`.
- [x] Re-run the targeted backend test and verify it passes.

### Task 2: Frontend API and Page

**Files:**
- Modify: `frontend/src/api.test.ts`
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/pages/MistakePage.test.ts`
- Modify: `frontend/src/pages/MistakePage.vue`

- [x] Write a failing API test for `updateMistakeStatus({ userId: 7, questionId: 1, status: 'MASTERED' })`.
- [x] Write a failing page test where clicking "标记已掌握" calls the API and refreshes the list.
- [x] Run `npm test -- api.test.ts MistakePage.test.ts` and verify the failures are caused by missing update support.
- [x] Implement `updateMistakeStatus` in the API client.
- [x] Add status action buttons to `MistakePage.vue`.
- [x] Re-run targeted frontend tests and verify they pass.

### Task 3: Verification and Delivery

**Files:**
- No additional files.

- [x] Run `mvn test` from `backend`.
- [x] Run `npm test` from `frontend`.
- [x] Run `npm run build` from `frontend`.
- [x] Restart local services with `scripts/start-local.ps1 -UseMysql`.
- [ ] Commit and push the changes to `codex/java-learning-platform-implementation`.
