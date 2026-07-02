# Knowledge Point Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add administrator knowledge point category management.

**Architecture:** Add a small catalog to question-service and a Vue admin page. Keep existing question records as string knowledge points for compatibility.

**Tech Stack:** Spring Boot, JdbcTemplate, Vue 3, Vitest, Vite, MySQL.

---

### Task 1: Backend Knowledge Point Catalog

**Files:**
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/KnowledgePoint.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/KnowledgePointRepository.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/InMemoryKnowledgePointRepository.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/MySqlKnowledgePointRepository.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/api/KnowledgePointController.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/api/CreateKnowledgePointRequest.java`
- Create: `backend/question-service/src/test/java/com/studycollection/question/api/KnowledgePointControllerTest.java`
- Modify: `scripts/mysql-init.sql`

- [x] Write failing backend test for listing, creating, and disabling knowledge points.
- [x] Implement domain, repository, controller, and MySQL schema.
- [x] Run `mvn -pl question-service test`.

### Task 2: Frontend Admin Page

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/api.test.ts`
- Modify: `frontend/src/router.ts`
- Create: `frontend/src/pages/KnowledgePointPage.vue`
- Create: `frontend/src/pages/KnowledgePointPage.test.ts`
- Modify protected page navigation where admin links are shown.

- [x] Write failing frontend API and page tests.
- [x] Add knowledge point API helpers and admin route.
- [x] Add knowledge point management page and admin navigation links.
- [x] Run `npm test -- src/api.test.ts src/pages/KnowledgePointPage.test.ts src/router.test.ts`.

### Task 3: Verification and Delivery

- [x] Run `mvn -pl question-service test`.
- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Restart local services and publish the latest public URL.
- [x] Commit and push changes.
