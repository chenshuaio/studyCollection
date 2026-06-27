# Java Learning Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local-first Java learning question platform with Vue, Spring Boot microservice-prepared backend, MySQL, login, question import, practice, exams, custom exam composition, mistakes, reports, and selectable AI/rule analysis.

**Architecture:** Use a monorepo with one Vue app and multiple Spring Boot services. Each backend service owns a focused domain package and exposes REST APIs; the first implementation keeps service boundaries explicit while allowing local startup through Maven and npm commands.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, Maven, Vue 3, Vite, TypeScript, Pinia, Vue Router, Vitest.

---

## File Structure

- Create `backend/pom.xml`: parent Maven project for all Spring Boot services and shared library.
- Create `backend/common-lib`: shared API response, error codes, JWT helpers, pagination DTOs, and test fixtures.
- Create `backend/gateway-service`: gateway entry point, auth filter, and route configuration.
- Create `backend/user-service`: users, roles, login, registration, JWT issuing, and permission checks.
- Create `backend/question-service`: question banks, questions, options, answers, knowledge points, and search APIs.
- Create `backend/import-service`: file upload, parser strategies, preview records, and confirmed import flow.
- Create `backend/exam-service`: practice generation, admin exam templates, custom user exams, answer sessions, and objective scoring.
- Create `backend/mistake-service`: wrong-answer records, mastery status, and repeat-practice APIs.
- Create `backend/report-service`: statistics, weak-point analysis, rule-based reports, and AI fallback orchestration.
- Create `backend/ai-service`: selectable online AI adapter and offline rule response adapter.
- Create `frontend`: Vue 3 app with user and admin layouts.
- Create `docs/api`: REST API notes used while services are implemented.
- Create `scripts`: local startup and database helper scripts.

## Implementation Tasks

### Task 1: Repository Scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/common-lib/pom.xml`
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/src/main.ts`
- Create: `scripts/dev-check.ps1`
- Modify: `.gitignore`

- [ ] **Step 1: Create failing repository health check**

Create `scripts/dev-check.ps1`:

```powershell
$ErrorActionPreference = "Stop"

if (!(Test-Path "backend/pom.xml")) { throw "Missing backend/pom.xml" }
if (!(Test-Path "backend/common-lib/pom.xml")) { throw "Missing backend/common-lib/pom.xml" }
if (!(Test-Path "frontend/package.json")) { throw "Missing frontend/package.json" }
if (!(Test-Path "frontend/src/main.ts")) { throw "Missing frontend/src/main.ts" }

Write-Host "Repository scaffold is present."
```

- [ ] **Step 2: Run health check and verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File scripts/dev-check.ps1`

Expected: FAIL with `Missing backend/pom.xml`.

- [ ] **Step 3: Create backend parent Maven file**

Create `backend/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.studycollection</groupId>
  <artifactId>studycollection-backend</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.2</spring-boot.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
  </properties>

  <modules>
    <module>common-lib</module>
  </modules>
</project>
```

- [ ] **Step 4: Create shared module Maven file**

Create `backend/common-lib/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.studycollection</groupId>
    <artifactId>studycollection-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>common-lib</artifactId>
  <packaging>jar</packaging>
</project>
```

- [ ] **Step 5: Create Vue app shell files**

Create `frontend/package.json`:

```json
{
  "name": "studycollection-web",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 127.0.0.1",
    "build": "vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@vitejs/plugin-vue": "^5.1.2",
    "vite": "^5.4.0",
    "vue": "^3.4.38",
    "vue-router": "^4.4.3",
    "pinia": "^2.2.2",
    "axios": "^1.7.4"
  },
  "devDependencies": {
    "typescript": "^5.5.4",
    "vitest": "^2.0.5"
  }
}
```

Create `frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true
  }
})
```

Create `frontend/src/main.ts`:

```ts
import { createApp } from 'vue'

const Root = {
  template: '<main>StudyCollection</main>'
}

createApp(Root).mount('#app')
```

- [ ] **Step 6: Update ignore rules**

Modify `.gitignore`:

```gitignore
.superpowers/
node_modules/
dist/
target/
.env
.env.local
uploads/
```

- [ ] **Step 7: Run health check and commit**

Run: `powershell -ExecutionPolicy Bypass -File scripts/dev-check.ps1`

Expected: PASS with `Repository scaffold is present.`

Commit:

```bash
git add .gitignore backend frontend scripts/dev-check.ps1
git commit -m "chore: scaffold java vue monorepo"
```

### Task 2: Shared Backend Contracts

**Files:**
- Create: `backend/common-lib/src/main/java/com/studycollection/common/api/ApiResponse.java`
- Create: `backend/common-lib/src/main/java/com/studycollection/common/api/ErrorCode.java`
- Create: `backend/common-lib/src/main/java/com/studycollection/common/security/Role.java`
- Test: `backend/common-lib/src/test/java/com/studycollection/common/api/ApiResponseTest.java`

- [ ] **Step 1: Write failing response contract test**

Create `backend/common-lib/src/test/java/com/studycollection/common/api/ApiResponseTest.java`:

```java
package com.studycollection.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successWrapsDataWithOkCode() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ready");
    }

    @Test
    void failureUsesErrorCodeMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.UNAUTHORIZED);

        assertThat(response.code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).isEqualTo("请先登录");
        assertThat(response.data()).isNull();
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl common-lib test`

Expected: FAIL because `ApiResponse` does not exist.

- [ ] **Step 3: Add response and enum contracts**

Create `backend/common-lib/src/main/java/com/studycollection/common/api/ErrorCode.java`:

```java
package com.studycollection.common.api;

public enum ErrorCode {
    UNAUTHORIZED("请先登录"),
    FORBIDDEN("没有权限"),
    VALIDATION_FAILED("请求参数不正确"),
    SERVICE_UNAVAILABLE("服务暂时不可用");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
```

Create `backend/common-lib/src/main/java/com/studycollection/common/api/ApiResponse.java`:

```java
package com.studycollection.common.api;

public record ApiResponse<T>(String code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.name(), errorCode.message(), null);
    }
}
```

Create `backend/common-lib/src/main/java/com/studycollection/common/security/Role.java`:

```java
package com.studycollection.common.security;

public enum Role {
    USER,
    ADMIN
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn -pl common-lib test`

Expected: PASS.

Commit:

```bash
git add backend/common-lib
git commit -m "feat: add shared backend contracts"
```

### Task 3: User Service Authentication

**Files:**
- Create: `backend/user-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/user-service/src/main/java/com/studycollection/user/UserServiceApplication.java`
- Create: `backend/user-service/src/main/java/com/studycollection/user/auth/AuthController.java`
- Create: `backend/user-service/src/main/java/com/studycollection/user/auth/AuthService.java`
- Create: `backend/user-service/src/main/java/com/studycollection/user/auth/LoginRequest.java`
- Create: `backend/user-service/src/main/java/com/studycollection/user/auth/LoginResponse.java`
- Test: `backend/user-service/src/test/java/com/studycollection/user/auth/AuthServiceTest.java`

- [ ] **Step 1: Write failing login service test**

Create `backend/user-service/src/test/java/com/studycollection/user/auth/AuthServiceTest.java`:

```java
package com.studycollection.user.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {
    @Test
    void demoAdminCanLogin() {
        AuthService service = new AuthService();

        LoginResponse response = service.login(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).startsWith("demo-token-");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.displayName()).isEqualTo("系统管理员");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl user-service test`

Expected: FAIL because `user-service` is not registered.

- [ ] **Step 3: Register user service Maven module**

Modify `backend/pom.xml` modules:

```xml
<modules>
  <module>common-lib</module>
  <module>user-service</module>
</modules>
```

Create `backend/user-service/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.studycollection</groupId>
    <artifactId>studycollection-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>user-service</artifactId>
  <dependencies>
    <dependency>
      <groupId>com.studycollection</groupId>
      <artifactId>common-lib</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Step 4: Add minimal authentication implementation**

Create `backend/user-service/src/main/java/com/studycollection/user/auth/LoginRequest.java`:

```java
package com.studycollection.user.auth;

public record LoginRequest(String username, String password) {
}
```

Create `backend/user-service/src/main/java/com/studycollection/user/auth/LoginResponse.java`:

```java
package com.studycollection.user.auth;

public record LoginResponse(String token, String role, String displayName) {
}
```

Create `backend/user-service/src/main/java/com/studycollection/user/auth/AuthService.java`:

```java
package com.studycollection.user.auth;

public class AuthService {
    public LoginResponse login(LoginRequest request) {
        if ("admin".equals(request.username()) && "admin123".equals(request.password())) {
            return new LoginResponse("demo-token-admin", "ADMIN", "系统管理员");
        }
        if ("user".equals(request.username()) && "user123".equals(request.password())) {
            return new LoginResponse("demo-token-user", "USER", "学习用户");
        }
        throw new IllegalArgumentException("用户名或密码错误");
    }
}
```

- [ ] **Step 5: Add Spring entry and controller**

Create `backend/user-service/src/main/java/com/studycollection/user/UserServiceApplication.java`:

```java
package com.studycollection.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

Create `backend/user-service/src/main/java/com/studycollection/user/auth/AuthController.java`:

```java
package com.studycollection.user.auth;

import com.studycollection.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService = new AuthService();

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
```

- [ ] **Step 6: Run tests and commit**

Run: `cd backend && mvn -pl user-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/user-service
git commit -m "feat: add user authentication service"
```

### Task 4: Frontend Login Page

**Files:**
- Create: `frontend/index.html`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/pages/LoginPage.vue`
- Create: `frontend/src/router.ts`
- Create: `frontend/src/styles/theme.css`
- Test: `frontend/src/pages/LoginPage.test.ts`

- [ ] **Step 1: Write failing login page test**

Create `frontend/src/pages/LoginPage.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginPage from './LoginPage.vue'

describe('LoginPage', () => {
  it('renders professional Java learning platform copy', () => {
    const wrapper = mount(LoginPage)

    expect(wrapper.text()).toContain('Java 学习题库平台')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('错题报告')
    expect(wrapper.find('button[type="submit"]').text()).toBe('登录')
  })
})
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd frontend && npm test -- LoginPage.test.ts`

Expected: FAIL because Vue test utilities and the page are absent.

- [ ] **Step 3: Add test dependency**

Modify `frontend/package.json` dev dependencies:

```json
"@vue/test-utils": "^2.4.6",
"jsdom": "^24.1.1"
```

Modify `frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true
  },
  test: {
    environment: 'jsdom'
  }
})
```

- [ ] **Step 4: Implement polished login page**

Create `frontend/index.html`:

```html
<div id="app"></div>
<script type="module" src="/src/main.ts"></script>
```

Create `frontend/src/App.vue`:

```vue
<template>
  <router-view />
</template>
```

Create `frontend/src/router.ts`:

```ts
import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: LoginPage }
  ]
})
```

Modify `frontend/src/main.ts`:

```ts
import { createApp } from 'vue'
import { router } from './router'
import App from './App.vue'
import './styles/theme.css'

createApp(App).use(router).mount('#app')
```

Create `frontend/src/pages/LoginPage.vue`:

```vue
<template>
  <main class="login-shell">
    <section class="login-hero">
      <p class="eyebrow">StudyCollection</p>
      <h1>Java 学习题库平台</h1>
      <p class="hero-copy">从入门到精通，汇总题库、智能组卷、错题报告和薄弱点强化训练。</p>
      <div class="feature-grid">
        <span>题库导入</span>
        <span>自动出题</span>
        <span>错题报告</span>
      </div>
    </section>
    <section class="login-card" aria-label="登录表单">
      <h2>欢迎回来</h2>
      <p>登录后继续你的 Java 训练计划</p>
      <form>
        <label>
          账号
          <input autocomplete="username" placeholder="admin 或 user" />
        </label>
        <label>
          密码
          <input autocomplete="current-password" type="password" placeholder="请输入密码" />
        </label>
        <button type="submit">登录</button>
      </form>
      <a href="/register">创建学习账号</a>
    </section>
  </main>
</template>
```

Create `frontend/src/styles/theme.css`:

```css
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: Inter, "Microsoft YaHei", system-ui, sans-serif;
  color: #172033;
  background: #f5f7fb;
}

.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 48px;
  align-items: center;
  padding: 56px;
}

.login-hero h1 {
  margin: 0 0 18px;
  font-size: 52px;
  line-height: 1.08;
}

.eyebrow {
  color: #1f6feb;
  font-weight: 700;
}

.hero-copy {
  max-width: 640px;
  color: #5c6678;
  font-size: 19px;
  line-height: 1.7;
}

.feature-grid {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.feature-grid span {
  padding: 10px 14px;
  border: 1px solid #d8e1ef;
  border-radius: 8px;
  background: white;
  color: #344054;
}

.login-card {
  background: white;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  padding: 34px;
  box-shadow: 0 18px 48px rgba(31, 45, 61, 0.12);
}

.login-card h2 {
  margin: 0 0 8px;
}

.login-card p {
  margin: 0 0 24px;
  color: #667085;
}

.login-card form {
  display: grid;
  gap: 16px;
}

.login-card label {
  display: grid;
  gap: 8px;
  color: #344054;
}

.login-card input {
  height: 44px;
  border: 1px solid #cfd8e6;
  border-radius: 7px;
  padding: 0 12px;
  font: inherit;
}

.login-card button {
  height: 46px;
  border: 0;
  border-radius: 7px;
  background: #1f6feb;
  color: white;
  font-weight: 700;
  cursor: pointer;
}

.login-card a {
  display: inline-block;
  margin-top: 18px;
  color: #1f6feb;
  text-decoration: none;
}
```

- [ ] **Step 5: Run test, build, and commit**

Run:

```bash
cd frontend
npm install
npm test -- LoginPage.test.ts
npm run build
```

Expected: tests PASS and build exits 0.

Commit:

```bash
git add frontend
git commit -m "feat: add polished login page"
```

### Task 5: Question Domain and Search

**Files:**
- Create: `backend/question-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/Question.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/QuestionType.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/domain/Difficulty.java`
- Create: `backend/question-service/src/main/java/com/studycollection/question/app/QuestionSearchService.java`
- Test: `backend/question-service/src/test/java/com/studycollection/question/app/QuestionSearchServiceTest.java`

- [ ] **Step 1: Write failing search test**

Create `backend/question-service/src/test/java/com/studycollection/question/app/QuestionSearchServiceTest.java`:

```java
package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSearchServiceTest {
    @Test
    void filtersByKnowledgePointDifficultyAndType() {
        QuestionSearchService service = new QuestionSearchService(List.of(
                new Question(1L, "ArrayList 扩容机制", QuestionType.SINGLE_CHOICE, Difficulty.BEGINNER, "集合框架"),
                new Question(2L, "JVM GC Roots", QuestionType.SHORT_ANSWER, Difficulty.ADVANCED, "JVM")
        ));

        List<Question> result = service.search("集合框架", Difficulty.BEGINNER, QuestionType.SINGLE_CHOICE);

        assertThat(result).extracting(Question::id).containsExactly(1L);
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl question-service test`

Expected: FAIL because `question-service` is not registered.

- [ ] **Step 3: Register module and implement domain**

Add `question-service` to `backend/pom.xml` modules.

Create `backend/question-service/pom.xml` using the same parent pattern as `user-service`, with dependency on `common-lib`.

Create `QuestionType.java`:

```java
package com.studycollection.question.domain;

public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    FILL_BLANK,
    SHORT_ANSWER,
    PROGRAMMING
}
```

Create `Difficulty.java`:

```java
package com.studycollection.question.domain;

public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
```

Create `Question.java`:

```java
package com.studycollection.question.domain;

public record Question(
        Long id,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint
) {
}
```

Create `QuestionSearchService.java`:

```java
package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;

import java.util.List;

public class QuestionSearchService {
    private final List<Question> questions;

    public QuestionSearchService(List<Question> questions) {
        this.questions = questions;
    }

    public List<Question> search(String knowledgePoint, Difficulty difficulty, QuestionType type) {
        return questions.stream()
                .filter(question -> question.knowledgePoint().equals(knowledgePoint))
                .filter(question -> question.difficulty() == difficulty)
                .filter(question -> question.type() == type)
                .toList();
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn -pl question-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/question-service
git commit -m "feat: add question search domain"
```

### Task 6: Import Preview Flow

**Files:**
- Create: `backend/import-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/import-service/src/main/java/com/studycollection/importer/parser/ParsedQuestion.java`
- Create: `backend/import-service/src/main/java/com/studycollection/importer/parser/MarkdownQuestionParser.java`
- Create: `backend/import-service/src/main/java/com/studycollection/importer/parser/ImportPreview.java`
- Test: `backend/import-service/src/test/java/com/studycollection/importer/parser/MarkdownQuestionParserTest.java`

- [ ] **Step 1: Write failing Markdown parser test**

Create `MarkdownQuestionParserTest.java`:

```java
package com.studycollection.importer.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownQuestionParserTest {
    @Test
    void parsesSimpleQuestionBlocksIntoPreview() {
        String markdown = """
                ## 单选题
                题目: Java 中 int 默认值是多少？
                A. 0
                B. null
                答案: A
                知识点: Java 基础
                难度: BEGINNER
                """;

        ImportPreview preview = new MarkdownQuestionParser().parse(markdown);

        assertThat(preview.questions()).hasSize(1);
        assertThat(preview.questions().get(0).title()).isEqualTo("Java 中 int 默认值是多少？");
        assertThat(preview.questions().get(0).answer()).isEqualTo("A");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl import-service test`

Expected: FAIL because `import-service` is not registered.

- [ ] **Step 3: Register module and implement parser**

Add `import-service` to `backend/pom.xml` modules.

Create `backend/import-service/pom.xml` with dependencies on `common-lib` and `question-service`.

Create `ParsedQuestion.java`:

```java
package com.studycollection.importer.parser;

public record ParsedQuestion(
        String title,
        String answer,
        String knowledgePoint,
        String difficulty
) {
}
```

Create `ImportPreview.java`:

```java
package com.studycollection.importer.parser;

import java.util.List;

public record ImportPreview(List<ParsedQuestion> questions) {
}
```

Create `MarkdownQuestionParser.java`:

```java
package com.studycollection.importer.parser;

import java.util.ArrayList;
import java.util.List;

public class MarkdownQuestionParser {
    public ImportPreview parse(String markdown) {
        String[] lines = markdown.split("\\R");
        String title = "";
        String answer = "";
        String knowledgePoint = "";
        String difficulty = "";

        for (String line : lines) {
            if (line.startsWith("题目:")) {
                title = line.substring("题目:".length()).trim();
            } else if (line.startsWith("答案:")) {
                answer = line.substring("答案:".length()).trim();
            } else if (line.startsWith("知识点:")) {
                knowledgePoint = line.substring("知识点:".length()).trim();
            } else if (line.startsWith("难度:")) {
                difficulty = line.substring("难度:".length()).trim();
            }
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        if (!title.isBlank()) {
            questions.add(new ParsedQuestion(title, answer, knowledgePoint, difficulty));
        }
        return new ImportPreview(questions);
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn -pl import-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/import-service
git commit -m "feat: add import preview parser"
```

### Task 7: Practice, Exam, and Custom Composition

**Files:**
- Create: `backend/exam-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamPaper.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/CustomExamComposer.java`
- Create: `backend/exam-service/src/main/java/com/studycollection/exam/app/ScoringService.java`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/app/CustomExamComposerTest.java`
- Test: `backend/exam-service/src/test/java/com/studycollection/exam/app/ScoringServiceTest.java`

- [ ] **Step 1: Write failing custom composition test**

Create `CustomExamComposerTest.java`:

```java
package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamPaper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExamComposerTest {
    @Test
    void createsUserPaperFromSelectedQuestionIds() {
        CustomExamComposer composer = new CustomExamComposer();

        ExamPaper paper = composer.compose("集合专项测试", 45, List.of(11L, 12L, 13L));

        assertThat(paper.name()).isEqualTo("集合专项测试");
        assertThat(paper.durationMinutes()).isEqualTo(45);
        assertThat(paper.questionIds()).containsExactly(11L, 12L, 13L);
    }
}
```

- [ ] **Step 2: Write failing scoring test**

Create `ScoringServiceTest.java`:

```java
package com.studycollection.exam.app;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {
    @Test
    void scoresObjectiveAnswers() {
        ScoringService service = new ScoringService();

        int score = service.score(
                Map.of(1L, "A", 2L, "true"),
                Map.of(1L, "A", 2L, "false"),
                5
        );

        assertThat(score).isEqualTo(5);
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run: `cd backend && mvn -pl exam-service test`

Expected: FAIL because `exam-service` is not registered.

- [ ] **Step 4: Register module and implement exam services**

Add `exam-service` to `backend/pom.xml` modules.

Create `backend/exam-service/pom.xml` with dependencies on `common-lib` and `question-service`.

Create `ExamPaper.java`:

```java
package com.studycollection.exam.domain;

import java.util.List;

public record ExamPaper(String name, int durationMinutes, List<Long> questionIds) {
}
```

Create `CustomExamComposer.java`:

```java
package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamPaper;

import java.util.List;

public class CustomExamComposer {
    public ExamPaper compose(String name, int durationMinutes, List<Long> selectedQuestionIds) {
        if (selectedQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一道题目");
        }
        return new ExamPaper(name, durationMinutes, List.copyOf(selectedQuestionIds));
    }
}
```

Create `ScoringService.java`:

```java
package com.studycollection.exam.app;

import java.util.Map;

public class ScoringService {
    public int score(Map<Long, String> correctAnswers, Map<Long, String> submittedAnswers, int pointsPerQuestion) {
        return correctAnswers.entrySet().stream()
                .mapToInt(entry -> entry.getValue().equals(submittedAnswers.get(entry.getKey())) ? pointsPerQuestion : 0)
                .sum();
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run: `cd backend && mvn -pl exam-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/exam-service
git commit -m "feat: add custom exam composition"
```

### Task 8: Mistake and Report Analysis

**Files:**
- Create: `backend/mistake-service/pom.xml`
- Create: `backend/report-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/mistake-service/src/main/java/com/studycollection/mistake/domain/MistakeRecord.java`
- Create: `backend/report-service/src/main/java/com/studycollection/report/app/WeakPointAnalyzer.java`
- Create: `backend/report-service/src/main/java/com/studycollection/report/app/LearningReport.java`
- Test: `backend/report-service/src/test/java/com/studycollection/report/app/WeakPointAnalyzerTest.java`

- [ ] **Step 1: Write failing weak-point test**

Create `WeakPointAnalyzerTest.java`:

```java
package com.studycollection.report.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeakPointAnalyzerTest {
    @Test
    void identifiesLowestAccuracyKnowledgePoint() {
        WeakPointAnalyzer analyzer = new WeakPointAnalyzer();

        LearningReport report = analyzer.analyze(List.of(
                new WeakPointAnalyzer.Result("集合框架", true),
                new WeakPointAnalyzer.Result("集合框架", false),
                new WeakPointAnalyzer.Result("JVM", false),
                new WeakPointAnalyzer.Result("JVM", false)
        ));

        assertThat(report.weakestKnowledgePoint()).isEqualTo("JVM");
        assertThat(report.recommendation()).contains("JVM");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl report-service test`

Expected: FAIL because `report-service` is not registered.

- [ ] **Step 3: Register modules and implement analyzer**

Add `mistake-service` and `report-service` to `backend/pom.xml`.

Create both module `pom.xml` files with dependency on `common-lib`.

Create `backend/mistake-service/src/main/java/com/studycollection/mistake/domain/MistakeRecord.java`:

```java
package com.studycollection.mistake.domain;

public record MistakeRecord(Long userId, Long questionId, String knowledgePoint, String status) {
}
```

Create `LearningReport.java`:

```java
package com.studycollection.report.app;

public record LearningReport(String weakestKnowledgePoint, String recommendation) {
}
```

Create `WeakPointAnalyzer.java`:

```java
package com.studycollection.report.app;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeakPointAnalyzer {
    public record Result(String knowledgePoint, boolean correct) {
    }

    public LearningReport analyze(List<Result> results) {
        String weakest = results.stream()
                .collect(Collectors.groupingBy(Result::knowledgePoint))
                .entrySet()
                .stream()
                .min(Comparator.comparingDouble(entry -> accuracy(entry.getValue())))
                .map(Map.Entry::getKey)
                .orElse("暂无数据");

        return new LearningReport(weakest, "建议优先强化 " + weakest + "，并通过错题重练巩固。");
    }

    private double accuracy(List<Result> results) {
        long correct = results.stream().filter(Result::correct).count();
        return results.isEmpty() ? 1.0 : (double) correct / results.size();
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn -pl report-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/mistake-service backend/report-service
git commit -m "feat: add weak point report analysis"
```

### Task 9: AI Analysis Switch

**Files:**
- Create: `backend/ai-service/pom.xml`
- Modify: `backend/pom.xml`
- Create: `backend/ai-service/src/main/java/com/studycollection/ai/app/AnalysisMode.java`
- Create: `backend/ai-service/src/main/java/com/studycollection/ai/app/AiAnalysisService.java`
- Create: `backend/ai-service/src/main/java/com/studycollection/ai/app/AnalysisAdvice.java`
- Test: `backend/ai-service/src/test/java/com/studycollection/ai/app/AiAnalysisServiceTest.java`

- [ ] **Step 1: Write failing fallback test**

Create `AiAnalysisServiceTest.java`:

```java
package com.studycollection.ai.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceTest {
    @Test
    void offlineModeUsesRuleAdvice() {
        AiAnalysisService service = new AiAnalysisService();

        AnalysisAdvice advice = service.generate(AnalysisMode.OFFLINE_RULES, "集合框架正确率 50%");

        assertThat(advice.source()).isEqualTo("RULES");
        assertThat(advice.content()).contains("集合框架");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `cd backend && mvn -pl ai-service test`

Expected: FAIL because `ai-service` is not registered.

- [ ] **Step 3: Register module and implement switch**

Add `ai-service` to `backend/pom.xml`.

Create `backend/ai-service/pom.xml` with dependency on `common-lib`.

Create `AnalysisMode.java`:

```java
package com.studycollection.ai.app;

public enum AnalysisMode {
    ONLINE_MODEL,
    OFFLINE_RULES
}
```

Create `AnalysisAdvice.java`:

```java
package com.studycollection.ai.app;

public record AnalysisAdvice(String source, String content) {
}
```

Create `AiAnalysisService.java`:

```java
package com.studycollection.ai.app;

public class AiAnalysisService {
    public AnalysisAdvice generate(AnalysisMode mode, String summary) {
        if (mode == AnalysisMode.ONLINE_MODEL) {
            return new AnalysisAdvice("ONLINE_MODEL", "在线模型建议：" + summary);
        }
        return new AnalysisAdvice("RULES", "规则分析建议：请针对薄弱项继续练习。" + summary);
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn -pl ai-service test`

Expected: PASS.

Commit:

```bash
git add backend/pom.xml backend/ai-service
git commit -m "feat: add ai analysis mode switch"
```

### Task 10: End-to-End Local Verification

**Files:**
- Create: `scripts/verify-local.ps1`
- Create: `docs/api/local-flow.md`

- [ ] **Step 1: Create verification script**

Create `scripts/verify-local.ps1`:

```powershell
$ErrorActionPreference = "Stop"

Push-Location backend
mvn test
Pop-Location

Push-Location frontend
npm test
npm run build
Pop-Location

Write-Host "Local verification passed."
```

- [ ] **Step 2: Create local flow documentation**

Create `docs/api/local-flow.md`:

```markdown
# Local Flow

1. 用户通过登录页登录。
2. 用户上传题目文件并进入解析预览。
3. 用户确认题目进入个人题库。
4. 用户选择知识点、难度、题型生成练习。
5. 用户手动勾选题目组成个人考试卷。
6. 用户提交答案。
7. 系统记录错题。
8. 系统生成规则报告或在线 AI 建议。
```

- [ ] **Step 3: Run full verification**

Run: `powershell -ExecutionPolicy Bypass -File scripts/verify-local.ps1`

Expected: backend tests PASS, frontend tests PASS, frontend build exits 0.

- [ ] **Step 4: Commit and push**

Commit:

```bash
git add scripts/verify-local.ps1 docs/api/local-flow.md
git commit -m "chore: add local verification flow"
git push
```

## Self-Review Notes

- Spec coverage: plan covers login, roles, question domain, import preview, practice/exam, user custom exam composition, mistakes, reports, AI/rule switch, and verification.
- Scope control: true Java code sandbox judging, class management, payments, and production container orchestration remain outside the first implementation scope.
- Execution order: tasks create runnable slices and commit frequently.
