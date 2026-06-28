# Question Bank Driven Practice And Exam Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make practice and custom exams use formal approved question bank data instead of hardcoded frontend sample questions.

**Architecture:** Reuse existing `/questions` search API for question selection. Extend practice submission answers with optional snapshot fields so the backend can score real question-bank records without coupling `exam-service` directly to `question-service`.

**Tech Stack:** Vue 3, Vitest, Java Spring Boot, JUnit.

---

### Task 1: Backend Snapshot Scoring

**Files:**
- Modify: `backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeAnswer.java`
- Modify: `backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeController.java`
- Modify: `backend/exam-service/src/test/java/com/studycollection/exam/api/PracticeControllerTest.java`

- [ ] Write failing test where a submitted answer includes `correctAnswer` and `analysis` for an arbitrary `questionId`.
- [ ] Implement snapshot scoring fallback when sample question bank has no matching question.
- [ ] Keep existing sample-question tests passing.

### Task 2: Frontend API Types

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/api.test.ts`

- [ ] Add optional `correctAnswer` and `analysis` fields to `PracticeAnswer`.
- [ ] Add tests that `submitUserPractice` sends snapshot answers unchanged.

### Task 3: Exam Center Uses Question Bank

**Files:**
- Modify: `frontend/src/pages/ExamPage.vue`
- Modify: `frontend/src/pages/ExamPage.test.ts`

- [ ] Write failing test that `ExamPage` loads `searchQuestions()` and saves selected real question data into the exam session.
- [ ] Replace hardcoded sample question list with searched formal questions.
- [ ] Keep custom exam composition behavior.

### Task 4: Exam Taking Supports Text Answers

**Files:**
- Modify: `frontend/src/pages/ExamTakingPage.vue`
- Modify: `frontend/src/pages/ExamTakingPage.test.ts`

- [ ] Write failing test for a question without options using text input.
- [ ] Render radio options when options exist, otherwise render an answer input.
- [ ] Submit snapshot answer fields for scoring.

### Task 5: Practice Center Uses Question Bank

**Files:**
- Modify: `frontend/src/pages/PracticePage.vue`
- Modify: `frontend/src/pages/PracticePage.test.ts`

- [ ] Write failing test that practice loads a formal question from `searchQuestions()`.
- [ ] Render question-bank question and answer input.
- [ ] Submit snapshot answer fields and record mistakes as before.

### Task 6: Verification And Documentation Check

**Files:**
- Existing tests and docs.

- [ ] Run targeted backend and frontend tests.
- [ ] Run `.\scripts\verify-local.ps1`.
- [ ] Restart local services.
- [ ] Compare with `docs/superpowers/specs/2026-06-27-java-learning-platform-design.md`.
