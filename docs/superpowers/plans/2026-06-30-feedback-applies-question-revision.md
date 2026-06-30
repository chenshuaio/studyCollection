# 反馈采纳修订题库实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let accepted question feedback update the formal question bank, not only record review status.

**Architecture:** Extend the existing feedback accept request with optional revision fields. Inject `QuestionRepository` into `QuestionFeedbackController`/`QuestionFeedbackService`, update the existing `Question` through repository update support, and refresh the admin feedback review page payload.

---

### Task 1: Backend Revision Application

**Files:**
- Modify: `backend/question-service/src/test/java/com/studycollection/question/app/QuestionFeedbackServiceTest.java`
- Modify: `backend/question-service/src/test/java/com/studycollection/question/api/QuestionFeedbackControllerTest.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/api/AcceptFeedbackRequest.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/QuestionRepository.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/InMemoryQuestionRepository.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/MySqlQuestionRepository.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/app/QuestionFeedbackService.java`
- Modify: `backend/question-service/src/main/java/com/studycollection/question/api/QuestionFeedbackController.java`

- [x] Write failing backend tests that accepted feedback updates the related question's answer and analysis.
- [x] Add repository lookup/update methods.
- [x] Apply corrected answer/analysis during feedback acceptance.
- [x] Keep existing accept/reject/needs-review behavior passing.
- [x] Re-run `mvn -pl question-service test`.

### Task 2: Frontend Admin Review

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/pages/FeedbackReviewPage.test.ts`
- Modify: `frontend/src/pages/FeedbackReviewPage.vue`

- [x] Write failing frontend test that accept payload includes corrected answer and analysis.
- [x] Add payload type fields.
- [x] Add clean Chinese admin fields for corrected answer and analysis.
- [x] Re-run `npm test -- FeedbackReviewPage.test.ts`.

### Task 3: Verification and Delivery

- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Run `mvn test`.
- [x] Restart local services with MySQL.
- [x] Commit and push changes.
