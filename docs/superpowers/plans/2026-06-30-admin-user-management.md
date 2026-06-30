# 管理员用户管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local admin user management page backed by a real user list API.

---

### Task 1: Backend User Listing

**Files:**
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/UserRepository.java`
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/InMemoryUserRepository.java`
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/MyBatisUserRepository.java`
- Modify: `backend/user-service/src/main/java/com/studycollection/user/auth/UserMapper.java`
- Add: `backend/user-service/src/main/java/com/studycollection/user/auth/UserSummary.java`
- Add: `backend/user-service/src/main/java/com/studycollection/user/auth/UserController.java`
- Modify tests under `backend/user-service/src/test/java/com/studycollection/user/auth`

- [x] Write failing backend tests for listing users without password hashes.
- [x] Add repository `findAll()` support.
- [x] Add `GET /users` controller.
- [x] Fix user-service Chinese validation messages.
- [x] Run `mvn -pl user-service test`.

### Task 2: Frontend User Management

**Files:**
- Modify: `frontend/src/api.ts`
- Add: `frontend/src/pages/UserManagementPage.vue`
- Add: `frontend/src/pages/UserManagementPage.test.ts`
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/router.test.ts`
- Modify: `frontend/src/pages/DashboardPage.vue`
- Modify: `frontend/src/pages/DashboardPage.test.ts`

- [x] Write failing frontend tests for `listUsers()`, route access, and the user management page.
- [x] Add API type and function.
- [x] Add admin-only route `/users`.
- [x] Add user management page and admin navigation.
- [x] Fix dashboard Chinese copy.
- [x] Run targeted frontend tests.

### Task 3: Verification and Delivery

- [x] Run `npm test`.
- [x] Run `npm run build`.
- [x] Run `mvn test`.
- [x] Restart local services with MySQL.
- [x] Commit and push changes.
