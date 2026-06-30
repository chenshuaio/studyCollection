# 错题重练实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make mistake-book retry actions open the selected mistake in practice mode.

---

### Task 1: Frontend Retry Target

**Files:**
- Modify: `frontend/src/pages/MistakePage.test.ts`
- Modify: `frontend/src/pages/MistakePage.vue`
- Modify: `frontend/src/pages/PracticePage.test.ts`
- Modify: `frontend/src/pages/PracticePage.vue`

- [x] Write failing test that clicking retry stores selected mistake target.
- [x] Write failing test that practice prioritizes the stored mistake question.
- [x] Implement retry target storage in mistake page.
- [x] Implement retry target consumption in practice page.
- [x] Run targeted frontend tests.

### Task 2: Verification and Delivery

- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Run `mvn test`.
- [x] Restart local services with MySQL.
- [x] Commit and push changes.
