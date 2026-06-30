# Responsive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Vue frontend usable on both desktop and mobile viewports.

**Architecture:** Keep the current desktop layout as the default. Add responsive CSS breakpoints in the shared theme so every existing page benefits without adding new navigation state or route-specific mobile components.

**Tech Stack:** Vue 3, Vue Router, Vitest, Vite, CSS media queries.

---

### Task 1: Responsive Structure Test

**Files:**
- Modify: `frontend/src/pages/DashboardPage.test.ts`

- [x] Add a test that renders the dashboard and verifies the page uses the shared shell, sidebar navigation, header actions, metric grid, and workspace grid classes.
- [x] Run `npm test -- src/pages/DashboardPage.test.ts` and verify the new assertion fails before the CSS/structure work if any expected responsive hook is missing.

### Task 2: Shared Responsive CSS

**Files:**
- Modify: `frontend/src/styles/theme.css`

- [x] Preserve desktop defaults for `.dashboard-shell`, `.dashboard-sidebar`, `.metric-grid`, `.workspace-grid`, `.question-layout`, `.import-layout`, `.practice-layout`, and `.exam-taking-layout`.
- [x] Add or refine `max-width: 860px` rules so layouts become single-column, the sidebar becomes a top navigation band, and header actions can wrap.
- [x] Add `max-width: 560px` rules for phone density: smaller page padding, smaller headings, full-width action buttons, scrollable tables, compact cards, and file upload stacking.
- [x] Ensure text can wrap inside table cells, buttons, option labels, and panels.

### Task 3: Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-06-30-responsive-layout.md`

- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Start or reuse the local frontend service.
- [x] Check desktop viewport and mobile viewport in the browser.
- [x] Commit and push changes.
