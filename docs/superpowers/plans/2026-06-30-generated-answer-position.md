# 生成题答案位置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generated single-choice questions should not always use `A` as the correct answer.

---

### Task 1: Generator Behavior

**Files:**
- Modify: `backend/import-service/src/test/java/com/studycollection/importer/generator/KnowledgeQuestionGeneratorTest.java`
- Modify: `backend/import-service/src/main/java/com/studycollection/importer/generator/KnowledgeQuestionGenerator.java`

- [x] Write failing generator tests for varied correct answer letters and visible A-D options.
- [x] Add deterministic answer position mixing for generated single-choice questions.
- [x] Keep existing generated question fields compatible.
- [x] Run `mvn -pl import-service test`.

### Task 2: Verification and Delivery

- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Run `mvn test`.
- [x] Restart local services with MySQL.
- [x] Commit and push changes.
