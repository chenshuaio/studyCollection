# Report From Mistakes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hardcoded report sample data with current user's mistake-book-derived analysis input.

**Architecture:** Keep report generation API unchanged. The frontend report page loads `listMistakes(currentUserId)`, converts mistake statuses into `LearningReportPayload.results`, and calls `generateLearningReport`.

**Tech Stack:** Vue 3, TypeScript, Vitest.

---

### Task 1: Report Page Data Source

**Files:**
- Modify: `frontend/src/pages/ReportPage.test.ts`
- Modify: `frontend/src/pages/ReportPage.vue`

- [x] Write a failing test that report generation first fetches `/api/mistakes?userId=7` and then posts derived report results.
- [x] Write a failing test that no mistake data disables report generation with a prompt.
- [x] Run `npm test -- ReportPage.test.ts` and verify failures are caused by hardcoded sample data.
- [x] Import `listMistakes` and `getCurrentUser`.
- [x] Load mistakes on mount and build report results from mistake status.
- [x] Render mistake count and pending count.
- [x] Re-run targeted frontend test and verify it passes.

### Task 2: Verification and Delivery

**Files:**
- No additional files.

- [x] Run `npm test` from `frontend`.
- [x] Run `npm run build` from `frontend`.
- [x] Run `mvn test` from `backend`.
- [x] Restart local services with `scripts/start-local.ps1 -UseMysql`.
- [x] Commit and push the changes to `codex/java-learning-platform-implementation`.
