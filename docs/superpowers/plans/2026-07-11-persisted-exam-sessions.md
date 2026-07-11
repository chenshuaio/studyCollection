# Persisted Exam Sessions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist custom exam papers, answers, timer state, submissions, and history so an authenticated user can resume an interrupted Java exam and review completed results.

**Architecture:** `exam-service` owns an immutable question snapshot for each exam session and stores current answers separately from the public question bank. The API always derives ownership from the authenticated token, hides correct answers until submission, automatically finalizes expired sessions, and only auto-grades objective question types. Vue routes use the server session id instead of `sessionStorage`, save answers incrementally, display a real countdown, and list active/completed exam history.

**Tech Stack:** Java 21, Spring Boot 3, Spring JDBC, MySQL 8, Vue 3, TypeScript, Vitest.

---

### Task 1: Define and test the exam session aggregate

**Files:**
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamSession.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamQuestionSnapshot.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamAnswer.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamStatus.java`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/domain/ExamSessionTest.java`

- [x] Write a failing test proving answer replacement, objective scoring, subjective non-grading, and completion state.
- [x] Run `mvn -pl exam-service -am -Dtest=ExamSessionTest -Dsurefire.failIfNoSpecifiedTests=false test` and confirm the test fails for missing types.
- [x] Implement the smallest immutable aggregate that passes the test.
- [x] Re-run the focused test and confirm it passes.

### Task 2: Persist sessions in memory and MySQL

**Files:**
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/ExamSessionRepository.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/InMemoryExamSessionRepository.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/MySqlExamSessionRepository.java`
- Modify: `scripts/mysql-init.sql`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/app/InMemoryExamSessionRepositoryTest.java`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/app/MySqlExamSessionRepositoryTest.java`

- [x] Write failing repository contract tests for create, ownership-safe lookup, answer persistence, completion, ordering, and snapshot retention.
- [x] Add `exam_sessions`, `exam_session_questions`, and `exam_session_answers` idempotent schema definitions.
- [x] Implement profile-specific repositories using the established `!local-mysql` and `local-mysql` pattern.
- [x] Run the repository tests and confirm they pass.

### Task 3: Add authenticated exam lifecycle APIs

**Files:**
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/ExamSessionService.java`
- Modify: `backend/exam-service/src/main/java/com/studycollection/exam/api/CustomExamController.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/api/ExamSessionResponse.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/api/ExamSummaryResponse.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/api/SaveExamAnswerRequest.java`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/app/ExamSessionServiceTest.java`
- Modify: `backend/exam-service/src/test/java/com/studycollection/exam/api/CustomExamControllerTest.java`

- [x] Write failing tests for create/list/load/save/submit, token-derived ownership, hidden pre-submit answers, expiry finalization, and invalid input.
- [x] Implement `POST /exams/custom`, `GET /exams`, `GET /exams/{id}`, `PUT /exams/{id}/answers/{questionId}`, and `POST /exams/{id}/submit`.
- [x] Snapshot every selected question at creation, reject missing/duplicate questions, and limit duration to 1-480 minutes.
- [x] Auto-grade only `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`, and `FILL_BLANK`; return reference answers for subjective items only after submission.
- [x] Run all `exam-service` tests.

### Task 4: Use server sessions in the Vue exam workflow

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/pages/ExamPage.vue`
- Modify: `frontend/src/pages/ExamTakingPage.vue`
- Modify: `frontend/src/pages/ExamPage.test.ts`
- Modify: `frontend/src/pages/ExamTakingPage.test.ts`
- Modify: `frontend/src/api.test.ts`
- Modify: `frontend/src/router.test.ts`

- [x] Write failing component/API/router tests for server-created exam ids, history, restore, saved answers, countdown, automatic submission, and subjective result copy.
- [x] Replace `sessionStorage` transfer with `/exams/{id}` loading and a route parameter.
- [x] Save each changed answer to the backend, show save state, and prevent edits after submission.
- [x] Add active/completed exam history and a real `HH:MM:SS` countdown.
- [x] Run focused Vitest files and then the full frontend suite.

### Task 5: Verify the real MySQL flow and publish

**Files:**
- Modify: `docs/project-audit-2026-07-10.md`
- Modify: `docs/api/local-flow.md`
- Modify: `README.md`

- [x] Apply `scripts/mysql-init.sql` to the local `root/root` database.
- [x] Run the full `scripts/verify-local.ps1` verification.
- [x] Restart backend/frontend, create an exam through the authenticated API, save an answer, reload it, submit it, and verify history in MySQL mode.
- [x] Check desktop/mobile browser behavior and console output.
- [ ] Restart the public tunnel, probe public and local URLs, update the audit status, commit, and push the branch.
