# Mistake Filter Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add mistake-book filtering and summary counts.

**Architecture:** Keep filtering in the Vue page using computed state derived from the existing `listMistakes(userId)` response. No backend changes are needed because the current mistake dataset is small and already fetched for the user.

**Tech Stack:** Vue 3, TypeScript, Vitest.

---

### Task 1: Frontend Filter and Summary

**Files:**
- Modify: `frontend/src/pages/MistakePage.test.ts`
- Modify: `frontend/src/pages/MistakePage.vue`

- [x] Write a failing test that loads three mistakes and expects summary counts for total, pending, and mastered.
- [x] Write a failing test that filters by knowledge point `JVM` and status `PENDING`.
- [x] Run `npm test -- MistakePage.test.ts` and verify failure is caused by missing filter controls.
- [x] Add computed `knowledgePoints`, `filteredMistakes`, and `summaryCounts`.
- [x] Render filter controls and use `filteredMistakes` in the table.
- [x] Re-run targeted frontend test and verify it passes.

### Task 2: Verification and Delivery

**Files:**
- No additional files.

- [x] Run `npm test` from `frontend`.
- [x] Run `npm run build` from `frontend`.
- [x] Run `mvn test` from `backend`.
- [ ] Restart local services with `scripts/start-local.ps1 -UseMysql`.
- [ ] Commit and push the changes to `codex/java-learning-platform-implementation`.
