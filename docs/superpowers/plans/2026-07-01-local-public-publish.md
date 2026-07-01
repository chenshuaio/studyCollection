# Local Public Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one-command public publishing for the local Java learning platform after each update/restart.

**Architecture:** Keep the existing local frontend/backend startup scripts. Add a Cloudflare quick tunnel wrapper that exposes the Vite frontend and records the generated public URL.

**Tech Stack:** PowerShell, Vite, Cloudflare Tunnel (`cloudflared`), Spring Boot local app, Vue dev server.

---

### Task 1: Script Contract Test

**Files:**
- Create: `scripts/test-publish-scripts.ps1`

- [x] Add a PowerShell test script that checks `publish-local.ps1`, `stop-publish.ps1`, Vite `allowedHosts`, and the official Cloudflare latest download URL.
- [x] Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-publish-scripts.ps1` and verify it fails before implementation because the publish scripts are missing.

### Task 2: Publish and Stop Scripts

**Files:**
- Create: `scripts/publish-local.ps1`
- Create: `scripts/stop-publish.ps1`
- Modify: `frontend/vite.config.ts`

- [x] Implement `publish-local.ps1` to start local services when needed, resolve or download `cloudflared`, start a tunnel to `http://127.0.0.1:5173`, write PID/log/url files under `.local`, and print the public URL.
- [x] Implement `stop-publish.ps1` to stop the public tunnel and local services.
- [x] Add `allowedHosts: true` to the Vite dev server config.
- [x] Run the script contract test and confirm it passes.

### Task 3: Runtime Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-07-01-local-public-publish.md`

- [x] Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-publish-scripts.ps1`.
- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Run `scripts/publish-local.ps1 -UseMysql`.
- [x] Confirm `.local/public-url.txt` contains an HTTPS URL.
- [x] Access the public URL and confirm the login page loads.
- [x] Commit and push changes.
