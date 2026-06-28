# Admin Delete Question Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add administrator deletion for formal question-bank questions.

**Architecture:** Extend the existing question repository boundary with `deleteById(Long id)`, expose it through `QuestionController`, and call it from the Vue question-bank management page. Keep the implementation hard-delete only, matching the approved option A.

**Tech Stack:** Java 21, Spring Boot, JdbcTemplate, Vue 3, Vitest.

---

### Task 1: Backend Delete Endpoint

**Files:**
- Modify: `backend/question-service/src/test/java/com/studycollection/question/api/QuestionControllerTest.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/api/QuestionController.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/QuestionRepository.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/InMemoryQuestionRepository.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/MySqlQuestionRepository.java`

- [x] Write a failing controller test that creates two questions, deletes one, and verifies only the other remains.
- [x] Run `mvn -pl question-service test -Dtest=QuestionControllerTest` and verify the failure is caused by missing delete support.
- [x] Add `deleteById(Long id)` to the repository interface and implement it in both repositories.
- [x] Add `@DeleteMapping("/{id}")` to `QuestionController`.
- [x] Re-run the targeted backend test and verify it passes.

### Task 2: Frontend API and Page

**Files:**
- Modify: `frontend/src/api.test.ts`
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/pages/QuestionBankPage.test.ts`
- Modify: `frontend/src/pages/QuestionBankPage.vue`

- [x] Write a failing API test for `deleteQuestion(9)` sending `DELETE /api/questions/9`.
- [x] Write a failing page test where an admin confirms deletion and the page refreshes the list.
- [x] Run `npm test -- api.test.ts QuestionBankPage.test.ts` and verify the new tests fail for missing delete support.
- [x] Implement `deleteQuestion(id)` in the API client.
- [x] Add a delete button to the question-bank table and call the API after `window.confirm`.
- [x] Re-run the targeted frontend tests and verify they pass.

### Task 3: Verification and Delivery

**Files:**
- No additional files.

- [x] Run `mvn test` from `backend`.
- [x] Run `npm test` from `frontend`.
- [x] Run `npm run build` from `frontend`.
- [x] Restart local services with `scripts/start-local.ps1 -UseMysql`.
- [ ] Commit and push the changes to `codex/java-learning-platform-implementation`.
