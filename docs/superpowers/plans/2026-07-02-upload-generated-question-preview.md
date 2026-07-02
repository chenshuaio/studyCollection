# Upload Generated Question Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Uploaded Java learning materials generate preview questions before the user submits them for administrator review.

**Architecture:** Keep the existing upload endpoint and import page. Improve extraction/generation on the backend and make the frontend preview workflow explicit and tested.

**Tech Stack:** Spring Boot, Apache POI, PDFBox, Vue 3, Vitest, Vite.

---

### Task 1: Backend Upload Analysis

**Files:**
- Modify: `backend/import-service/src/test/java/com/studycollection/importer/api/KnowledgeGenerateControllerTest.java`
- Modify: `backend/import-service/src/test/java/com/studycollection/importer/generator/KnowledgeQuestionGeneratorTest.java`
- Modify: `backend/import-service/src/main/java/com/studycollection/importer/parser/KnowledgeFileTextExtractor.java`
- Modify: `backend/import-service/src/main/java/com/studycollection/importer/generator/KnowledgeQuestionGenerator.java`

- [x] Add failing tests for XLSX upload analysis and multi-topic material generation.
- [x] Add XLSX text extraction using Apache POI.
- [x] Add local generation rules for JVM, exceptions, interfaces/abstract classes, and collections.
- [x] Run `mvn -pl import-service test`.

### Task 2: Frontend Preview Workflow

**Files:**
- Modify: `frontend/src/pages/ImportPage.test.ts`
- Modify: `frontend/src/pages/ImportPage.vue`

- [x] Add failing test that uploading a file calls `uploadKnowledgeFile`, renders generated questions as preview rows, and submits those preview rows only after clicking submit.
- [x] Update status text to say uploaded questions are previewed and require review submission.
- [x] Add `.xlsx` to the file accept list.
- [x] Run `npm test -- src/pages/ImportPage.test.ts`.

### Task 3: Verification and Delivery

- [x] Run `mvn -pl import-service test`.
- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Restart local services and publish the latest public URL.
- [x] Commit and push changes.
