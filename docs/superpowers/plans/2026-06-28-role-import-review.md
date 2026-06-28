# Role Permission And Import Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate ordinary-user and administrator permissions, show the current account, and make imported Java questions enter an administrator review queue before becoming usable题库内容.

**Architecture:** Keep the current local monolith-style Spring Boot composition and Vue pages. Add a small pending-question review model inside `question-service`, expose review APIs next to existing question APIs, and gate admin-only frontend routes through existing route metadata.

**Tech Stack:** Java 17/Spring Boot, Vue 3, Vitest, JUnit, in-memory repositories for local tests with MySQL repository left compatible for approved questions.

---

### Task 1: Account Identity And Registration Rules

**Files:**
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/UserRepository.java`
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/InMemoryUserRepository.java`
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/AuthService.java`
- Modify: `backend/user-service/src/test/java/com/studycollection/user/auth/AuthServiceTest.java`
- Create: `frontend/src/components/CurrentAccount.vue`
- Modify: login-protected Vue pages to display `CurrentAccount`.

- [ ] Write a failing backend test proving duplicate `displayName` is rejected while registered role stays `USER`.
- [ ] Implement `findByDisplayName` in user repositories and validate it in `AuthService.register`.
- [ ] Add `CurrentAccount.vue` using `getCurrentUser()` to display display name, username when available, and localized role.
- [ ] Replace header logout-only UI with current account plus logout on protected pages.

### Task 2: Route And Navigation Permissions

**Files:**
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/router.test.ts`
- Modify: `frontend/src/pages/DashboardPage.vue`
- Modify: protected page sidebars.

- [ ] Write failing route tests that ordinary users cannot access `/questions` and administrators can.
- [ ] Mark `/questions` as `requiredRole: 'ADMIN'`.
- [ ] Hide admin-only navigation items for ordinary users.
- [ ] Keep import, practice, exams, mistakes, and reports visible to ordinary users.

### Task 3: Pending Import Review Queue

**Files:**
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/PendingQuestion.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/PendingQuestionStatus.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/PendingQuestionRepository.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/InMemoryPendingQuestionRepository.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/api/SubmitPendingQuestionRequest.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/api/PendingQuestionController.java`
- Create/modify tests for pending question submission and review.
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/pages/ImportPage.vue`
- Modify: `frontend/src/pages/ImportPage.test.ts`

- [ ] Write failing backend tests: submitting imports creates `PENDING` records; approved questions are copied to formal question bank; rejected ones are not.
- [ ] Implement pending question repository/controller with `POST /questions/pending`, `GET /questions/pending`, `POST /questions/pending/{id}/approve`, and `POST /questions/pending/{id}/reject`.
- [ ] Update import page to call pending submission instead of direct `createQuestion`.
- [ ] Update import page status copy to say “已提交管理员审核”.

### Task 4: Administrator Review UI And Verification

**Files:**
- Modify: `frontend/src/pages/QuestionBankPage.vue`
- Modify: `frontend/src/pages/QuestionBankPage.test.ts`
- Modify: `frontend/src/api.test.ts`
- Modify: `backend/local-app/src/test/java/com/studycollection/local/LocalStudyCollectionApplicationTest.java`

- [ ] Write failing frontend tests showing the admin question bank loads pending imports and can approve one.
- [ ] Add pending review panel to the question bank page.
- [ ] Refresh formal questions after approval.
- [ ] Update local integration smoke test to submit a pending question, approve it, and then find it in `/questions`.
- [ ] Run `.\scripts\verify-local.ps1`, restart local services, and compare implemented behavior against `docs/superpowers/specs/2026-06-27-java-learning-platform-design.md`.
