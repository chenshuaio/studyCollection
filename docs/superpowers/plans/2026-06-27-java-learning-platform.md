# Java 学习题库平台实施计划

> **给 agentic 执行者：** 必须使用子技能：推荐使用 `superpowers:subagent-driven-development`，也可以使用 `superpowers:executing-plans`，按任务逐项执行本计划。所有步骤使用复选框 (`- [ ]`) 跟踪进度。

**目标：** 构建一个本地优先的 Java 学习题库平台，使用 Vue、Spring Boot 微服务预备版后端和 MySQL，支持登录、题目导入、练习、考试、用户自定义组卷、错题、报告，以及可选择的 AI/规则分析。

**架构：** 使用 monorepo，一个 Vue 前端应用配多个 Spring Boot 后端服务。每个后端服务负责一个清晰的领域包并暴露 REST API；第一版保持服务边界清楚，同时允许通过 Maven 和 npm 在本地启动与验证。

**技术栈：** Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Maven、Vue 3、Vite、TypeScript、Pinia、Vue Router、Vitest。

---

## 文件结构

- 创建 `backend/pom.xml`：所有 Spring Boot 服务和共享库的 Maven 父项目。
- 创建 `backend/common-lib`：共享 API 响应、错误码、JWT 辅助能力、分页 DTO 和测试夹具。
- 创建 `backend/gateway-service`：网关入口、鉴权过滤器和路由配置。
- 创建 `backend/user-service`：用户、角色、登录、注册、JWT 签发和权限检查。
- 创建 `backend/question-service`：题库、题目、选项、答案、知识点、搜索 API、题目问题反馈和修订历史。
- 创建 `backend/import-service`：文件上传、解析策略、预览记录和确认导入流程。
- 创建 `backend/exam-service`：练习生成、管理员考试模板、用户自定义考试、答题会话和客观题评分。
- 创建 `backend/mistake-service`：错题记录、掌握状态和错题重练 API。
- 创建 `backend/report-service`：统计、薄弱点分析、规则报告和 AI 回退编排。
- 创建 `backend/ai-service`：可选在线 AI 适配器和离线规则响应适配器。
- 创建 `frontend`：Vue 3 应用，包含用户端和管理员端布局。
- 创建 `docs/api`：服务实现期间维护的 REST API 说明。
- 创建 `scripts`：本地启动、检查和数据库辅助脚本。

## 实施任务

### 任务 1：仓库脚手架

**文件：**
- 创建：`backend/pom.xml`
- 创建：`backend/common-lib/pom.xml`
- 创建：`frontend/package.json`
- 创建：`frontend/vite.config.ts`
- 创建：`frontend/src/main.ts`
- 创建：`scripts/dev-check.ps1`
- 修改：`.gitignore`

- [ ] **步骤 1：创建会失败的仓库健康检查**

创建 `scripts/dev-check.ps1`:

```powershell
$ErrorActionPreference = "Stop"

if (!(Test-Path "backend/pom.xml")) { throw "Missing backend/pom.xml" }
if (!(Test-Path "backend/common-lib/pom.xml")) { throw "Missing backend/common-lib/pom.xml" }
if (!(Test-Path "frontend/package.json")) { throw "Missing frontend/package.json" }
if (!(Test-Path "frontend/src/main.ts")) { throw "Missing frontend/src/main.ts" }

Write-Host "Repository scaffold is present."
```

- [ ] **步骤 2：运行健康检查并确认失败**

运行：`powershell -ExecutionPolicy Bypass -File scripts/dev-check.ps1`

预期：失败，并显示 `Missing backend/pom.xml`。

- [ ] **步骤 3：创建后端 Maven 父项目文件**

创建 `backend/pom.xml`:

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

- [ ] **步骤 4：创建共享模块 Maven 文件**

创建 `backend/common-lib/pom.xml`:

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

- [ ] **步骤 5：创建 Vue 应用外壳文件**

创建 `frontend/package.json`:

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

创建 `frontend/vite.config.ts`:

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

创建 `frontend/src/main.ts`:

```ts
import { createApp } from 'vue'

const Root = {
  template: '<main>StudyCollection</main>'
}

createApp(Root).mount('#app')
```

- [ ] **步骤 6：更新忽略规则**

修改 `.gitignore`:

```gitignore
.superpowers/
node_modules/
dist/
target/
.env
.env.local
uploads/
```

- [ ] **步骤 7：运行健康检查并提交**

运行：`powershell -ExecutionPolicy Bypass -File scripts/dev-check.ps1`

预期：通过，并显示 `Repository scaffold is present.`

提交：

```bash
git add .gitignore backend frontend scripts/dev-check.ps1
git commit -m "chore: scaffold java vue monorepo"
```

### 任务 2：后端共享契约

**文件：**
- 创建：`backend/common-lib/src/main/java/com/studycollection/common/api/ApiResponse.java`
- 创建：`backend/common-lib/src/main/java/com/studycollection/common/api/ErrorCode.java`
- 创建：`backend/common-lib/src/main/java/com/studycollection/common/security/Role.java`
- 测试：`backend/common-lib/src/test/java/com/studycollection/common/api/ApiResponseTest.java`

- [ ] **步骤 1：编写会失败的响应契约测试**

创建 `backend/common-lib/src/test/java/com/studycollection/common/api/ApiResponseTest.java`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd backend && mvn -pl common-lib test`

预期：失败，因为 `ApiResponse` 尚不存在。

- [ ] **步骤 3：添加响应对象和枚举契约**

创建 `backend/common-lib/src/main/java/com/studycollection/common/api/ErrorCode.java`:

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

创建 `backend/common-lib/src/main/java/com/studycollection/common/api/ApiResponse.java`:

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

创建 `backend/common-lib/src/main/java/com/studycollection/common/security/Role.java`:

```java
package com.studycollection.common.security;

public enum Role {
    USER,
    ADMIN
}
```

- [ ] **步骤 4：运行测试并提交**

运行：`cd backend && mvn -pl common-lib test`

预期：通过。

提交：

```bash
git add backend/common-lib
git commit -m "feat: add shared backend contracts"
```

### 任务 3：用户服务认证

**文件：**
- 创建：`backend/user-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/user-service/src/main/java/com/studycollection/user/UserServiceApplication.java`
- 创建：`backend/user-service/src/main/java/com/studycollection/user/auth/AuthController.java`
- 创建：`backend/user-service/src/main/java/com/studycollection/user/auth/AuthService.java`
- 创建：`backend/user-service/src/main/java/com/studycollection/user/auth/LoginRequest.java`
- 创建：`backend/user-service/src/main/java/com/studycollection/user/auth/LoginResponse.java`
- 测试：`backend/user-service/src/test/java/com/studycollection/user/auth/AuthServiceTest.java`

- [ ] **步骤 1：编写会失败的登录服务测试**

创建 `backend/user-service/src/test/java/com/studycollection/user/auth/AuthServiceTest.java`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd backend && mvn -pl user-service test`

预期：失败，因为 `user-service` 尚未注册。

- [ ] **步骤 3：注册用户服务 Maven 模块**

修改 `backend/pom.xml` modules:

```xml
<modules>
  <module>common-lib</module>
  <module>user-service</module>
</modules>
```

创建 `backend/user-service/pom.xml`:

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

- [ ] **步骤 4：添加最小认证实现**

创建 `backend/user-service/src/main/java/com/studycollection/user/auth/LoginRequest.java`:

```java
package com.studycollection.user.auth;

public record LoginRequest(String username, String password) {
}
```

创建 `backend/user-service/src/main/java/com/studycollection/user/auth/LoginResponse.java`:

```java
package com.studycollection.user.auth;

public record LoginResponse(String token, String role, String displayName) {
}
```

创建 `backend/user-service/src/main/java/com/studycollection/user/auth/AuthService.java`:

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

- [ ] **步骤 5：添加 Spring 启动入口和控制器**

创建 `backend/user-service/src/main/java/com/studycollection/user/UserServiceApplication.java`:

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

创建 `backend/user-service/src/main/java/com/studycollection/user/auth/AuthController.java`:

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

- [ ] **步骤 6：运行测试并提交**

运行：`cd backend && mvn -pl user-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/user-service
git commit -m "feat: add user authentication service"
```

### 任务 4：前端登录页

**文件：**
- 创建：`frontend/index.html`
- 创建：`frontend/src/App.vue`
- 创建：`frontend/src/pages/LoginPage.vue`
- 创建：`frontend/src/router.ts`
- 创建：`frontend/src/styles/theme.css`
- 测试：`frontend/src/pages/LoginPage.test.ts`

- [ ] **步骤 1：编写会失败的登录页测试**

创建 `frontend/src/pages/LoginPage.test.ts`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd frontend && npm test -- LoginPage.test.ts`

预期：失败，因为 Vue 测试工具和页面尚不存在。

- [ ] **步骤 3：添加测试依赖**

修改 `frontend/package.json` 的开发依赖：

```json
"@vue/test-utils": "^2.4.6",
"jsdom": "^24.1.1"
```

修改 `frontend/vite.config.ts`：

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

- [ ] **步骤 4：实现精美登录页**

创建 `frontend/index.html`:

```html
<div id="app"></div>
<script type="module" src="/src/main.ts"></script>
```

创建 `frontend/src/App.vue`:

```vue
<template>
  <router-view />
</template>
```

创建 `frontend/src/router.ts`:

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

修改 `frontend/src/main.ts`：

```ts
import { createApp } from 'vue'
import { router } from './router'
import App from './App.vue'
import './styles/theme.css'

createApp(App).use(router).mount('#app')
```

创建 `frontend/src/pages/LoginPage.vue`:

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

创建 `frontend/src/styles/theme.css`:

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

- [ ] **步骤 5：运行测试、构建并提交**

运行：

```bash
cd frontend
npm install
npm test -- LoginPage.test.ts
npm run build
```

预期：测试通过，构建以退出码 0 结束。

提交：

```bash
git add frontend
git commit -m "feat: add polished login page"
```

### 任务 5：题目领域模型、搜索与反馈审核

**文件：**
- 创建：`backend/question-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/Question.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/QuestionType.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/Difficulty.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/QuestionFeedback.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/FeedbackStatus.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/FeedbackType.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/domain/QuestionRevision.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/app/QuestionSearchService.java`
- 创建：`backend/question-service/src/main/java/com/studycollection/question/app/QuestionFeedbackService.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/app/QuestionSearchServiceTest.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/app/QuestionFeedbackServiceTest.java`

- [ ] **步骤 1：编写会失败的搜索测试**

创建 `backend/question-service/src/test/java/com/studycollection/question/app/QuestionSearchServiceTest.java`:

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

- [ ] **步骤 2：编写会失败的题目反馈审核测试**

创建 `backend/question-service/src/test/java/com/studycollection/question/app/QuestionFeedbackServiceTest.java`:

```java
package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionFeedbackServiceTest {
    @Test
    void acceptedFeedbackCreatesRevisionHistory() {
        QuestionFeedbackService service = new QuestionFeedbackService();
        QuestionFeedback feedback = service.submit(
                7L,
                101L,
                FeedbackType.ANSWER_ERROR,
                "标准答案应为 B，当前答案 A 不正确"
        );

        QuestionRevision revision = service.accept(
                feedback.id(),
                1L,
                "答案从 A 修改为 B",
                "用户反馈属实"
        );

        assertThat(service.find(feedback.id()).status()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(revision.questionId()).isEqualTo(101L);
        assertThat(revision.changeSummary()).contains("答案从 A 修改为 B");
    }
}
```

- [ ] **步骤 3：运行测试并确认失败**

运行：`cd backend && mvn -pl question-service test`

预期：失败，因为 `question-service` 尚未注册。

- [ ] **步骤 4：注册模块并实现领域模型**

将 `question-service` 添加到 `backend/pom.xml` 的 modules。

创建 `backend/question-service/pom.xml`，使用与 `user-service` 相同的父项目结构，并依赖 `common-lib`。

创建 `QuestionType.java`:

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

创建 `Difficulty.java`:

```java
package com.studycollection.question.domain;

public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
```

创建 `Question.java`:

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

创建 `FeedbackType.java`:

```java
package com.studycollection.question.domain;

public enum FeedbackType {
    ANSWER_ERROR,
    EXPLANATION_ERROR,
    STEM_ERROR,
    OPTION_ERROR,
    KNOWLEDGE_POINT_ERROR,
    DIFFICULTY_ERROR,
    OTHER
}
```

创建 `FeedbackStatus.java`:

```java
package com.studycollection.question.domain;

public enum FeedbackStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    NEEDS_REVIEW
}
```

创建 `QuestionFeedback.java`:

```java
package com.studycollection.question.domain;

public record QuestionFeedback(
        Long id,
        Long userId,
        Long questionId,
        FeedbackType type,
        String content,
        FeedbackStatus status
) {
}
```

创建 `QuestionRevision.java`:

```java
package com.studycollection.question.domain;

public record QuestionRevision(
        Long id,
        Long questionId,
        Long feedbackId,
        Long adminUserId,
        String changeSummary,
        String reviewNote
) {
}
```

创建 `QuestionSearchService.java`:

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

创建 `QuestionFeedbackService.java`:

```java
package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class QuestionFeedbackService {
    private final AtomicLong feedbackIds = new AtomicLong(1);
    private final AtomicLong revisionIds = new AtomicLong(1);
    private final Map<Long, QuestionFeedback> feedbacks = new HashMap<>();

    public QuestionFeedback submit(Long userId, Long questionId, FeedbackType type, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        Long id = feedbackIds.getAndIncrement();
        QuestionFeedback feedback = new QuestionFeedback(id, userId, questionId, type, content, FeedbackStatus.PENDING);
        feedbacks.put(id, feedback);
        return feedback;
    }

    public QuestionFeedback find(Long feedbackId) {
        QuestionFeedback feedback = feedbacks.get(feedbackId);
        if (feedback == null) {
            throw new IllegalArgumentException("反馈不存在");
        }
        return feedback;
    }

    public QuestionRevision accept(Long feedbackId, Long adminUserId, String changeSummary, String reviewNote) {
        QuestionFeedback feedback = find(feedbackId);
        QuestionFeedback accepted = new QuestionFeedback(
                feedback.id(),
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                FeedbackStatus.ACCEPTED
        );
        feedbacks.put(feedbackId, accepted);
        return new QuestionRevision(
                revisionIds.getAndIncrement(),
                feedback.questionId(),
                feedback.id(),
                adminUserId,
                changeSummary,
                reviewNote
        );
    }
}
```

- [ ] **步骤 5：运行测试并提交**

运行：`cd backend && mvn -pl question-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/question-service
git commit -m "feat: add question feedback review flow"
```

### 任务 6：导入预览流程

**文件：**
- 创建：`backend/import-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/import-service/src/main/java/com/studycollection/importer/parser/ParsedQuestion.java`
- 创建：`backend/import-service/src/main/java/com/studycollection/importer/parser/MarkdownQuestionParser.java`
- 创建：`backend/import-service/src/main/java/com/studycollection/importer/parser/ImportPreview.java`
- 测试：`backend/import-service/src/test/java/com/studycollection/importer/parser/MarkdownQuestionParserTest.java`

- [ ] **步骤 1：编写会失败的 Markdown 解析测试**

创建 `MarkdownQuestionParserTest.java`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd backend && mvn -pl import-service test`

预期：失败，因为 `import-service` 尚未注册。

- [ ] **步骤 3：注册模块并实现解析器**

添加 `import-service` to `backend/pom.xml` modules.

创建 `backend/import-service/pom.xml` with dependencies on `common-lib` and `question-service`.

创建 `ParsedQuestion.java`:

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

创建 `ImportPreview.java`:

```java
package com.studycollection.importer.parser;

import java.util.List;

public record ImportPreview(List<ParsedQuestion> questions) {
}
```

创建 `MarkdownQuestionParser.java`:

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

- [ ] **步骤 4：运行测试并提交**

运行：`cd backend && mvn -pl import-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/import-service
git commit -m "feat: add import preview parser"
```

### 任务 7：练习、考试与用户自定义组卷

**文件：**
- 创建：`backend/exam-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamPaper.java`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/app/CustomExamComposer.java`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/app/ScoringService.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/CustomExamComposerTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/ScoringServiceTest.java`

- [ ] **步骤 1：编写会失败的自定义组卷测试**

创建 `CustomExamComposerTest.java`:

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

- [ ] **步骤 2：编写会失败的评分测试**

创建 `ScoringServiceTest.java`:

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

- [ ] **步骤 3：运行测试并确认失败**

运行：`cd backend && mvn -pl exam-service test`

预期：失败，因为 `exam-service` 尚未注册。

- [ ] **步骤 4：注册模块并实现考试服务**

添加 `exam-service` to `backend/pom.xml` modules.

创建 `backend/exam-service/pom.xml` with dependencies on `common-lib` and `question-service`.

创建 `ExamPaper.java`:

```java
package com.studycollection.exam.domain;

import java.util.List;

public record ExamPaper(String name, int durationMinutes, List<Long> questionIds) {
}
```

创建 `CustomExamComposer.java`:

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

创建 `ScoringService.java`:

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

- [ ] **步骤 5：运行测试并提交**

运行：`cd backend && mvn -pl exam-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/exam-service
git commit -m "feat: add custom exam composition"
```

### 任务 8：错题与报告分析

**文件：**
- 创建：`backend/mistake-service/pom.xml`
- 创建：`backend/report-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/mistake-service/src/main/java/com/studycollection/mistake/domain/MistakeRecord.java`
- 创建：`backend/report-service/src/main/java/com/studycollection/report/app/WeakPointAnalyzer.java`
- 创建：`backend/report-service/src/main/java/com/studycollection/report/app/LearningReport.java`
- 测试：`backend/report-service/src/test/java/com/studycollection/report/app/WeakPointAnalyzerTest.java`

- [ ] **步骤 1：编写会失败的薄弱点测试**

创建 `WeakPointAnalyzerTest.java`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd backend && mvn -pl report-service test`

预期：失败，因为 `report-service` 尚未注册。

- [ ] **步骤 3：注册模块并实现分析器**

添加 `mistake-service` and `report-service` to `backend/pom.xml`.

创建两个模块的 `pom.xml` 文件，并让它们依赖 `common-lib`。

创建 `backend/mistake-service/src/main/java/com/studycollection/mistake/domain/MistakeRecord.java`:

```java
package com.studycollection.mistake.domain;

public record MistakeRecord(Long userId, Long questionId, String knowledgePoint, String status) {
}
```

创建 `LearningReport.java`:

```java
package com.studycollection.report.app;

public record LearningReport(String weakestKnowledgePoint, String recommendation) {
}
```

创建 `WeakPointAnalyzer.java`:

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

- [ ] **步骤 4：运行测试并提交**

运行：`cd backend && mvn -pl report-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/mistake-service backend/report-service
git commit -m "feat: add weak point report analysis"
```

### 任务 9：AI 分析模式开关

**文件：**
- 创建：`backend/ai-service/pom.xml`
- 修改：`backend/pom.xml`
- 创建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AnalysisMode.java`
- 创建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiAnalysisService.java`
- 创建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AnalysisAdvice.java`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/app/AiAnalysisServiceTest.java`

- [ ] **步骤 1：编写会失败的回退测试**

创建 `AiAnalysisServiceTest.java`:

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

- [ ] **步骤 2：运行测试并确认失败**

运行：`cd backend && mvn -pl ai-service test`

预期：失败，因为 `ai-service` 尚未注册。

- [ ] **步骤 3：注册模块并实现模式开关**

添加 `ai-service` to `backend/pom.xml`.

创建 `backend/ai-service/pom.xml` with dependency on `common-lib`.

创建 `AnalysisMode.java`:

```java
package com.studycollection.ai.app;

public enum AnalysisMode {
    ONLINE_MODEL,
    OFFLINE_RULES
}
```

创建 `AnalysisAdvice.java`:

```java
package com.studycollection.ai.app;

public record AnalysisAdvice(String source, String content) {
}
```

创建 `AiAnalysisService.java`:

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

- [ ] **步骤 4：运行测试并提交**

运行：`cd backend && mvn -pl ai-service test`

预期：通过。

提交：

```bash
git add backend/pom.xml backend/ai-service
git commit -m "feat: add ai analysis mode switch"
```

### 任务 10：本地端到端验证

**文件：**
- 创建：`scripts/verify-local.ps1`
- 创建：`docs/api/local-flow.md`

- [ ] **步骤 1：创建验证脚本**

创建 `scripts/verify-local.ps1`:

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

- [ ] **步骤 2：创建本地流程文档**

创建 `docs/api/local-flow.md`:

```markdown
# Local Flow

1. 用户通过登录页登录。
2. 用户上传题目文件并进入解析预览。
3. 用户确认题目进入个人题库。
4. 用户选择知识点、难度、题型生成练习。
5. 用户手动勾选题目组成个人考试卷。
6. 用户提交答案。
7. 系统记录错题。
8. 用户发现题目答案或解析错误时提交题目反馈。
9. 管理员审核反馈并修订题库。
10. 系统生成规则报告或在线 AI 建议。
```

- [ ] **步骤 3：运行完整验证**

运行：`powershell -ExecutionPolicy Bypass -File scripts/verify-local.ps1`

预期：后端测试通过，前端测试通过，前端构建以退出码 0 结束。

- [ ] **步骤 4：提交并推送**

提交：

```bash
git add scripts/verify-local.ps1 docs/api/local-flow.md
git commit -m "chore: add local verification flow"
git push
```

## 自审记录

- 规格覆盖：计划覆盖登录、角色、题目领域模型、题目问题反馈、管理员修订历史、导入预览、练习/考试、用户自定义组卷、错题、报告、AI/规则模式开关和验证。
- 范围控制：真正的 Java 代码沙箱判题、班级管理、支付和生产级容器编排不纳入第一版实现范围。
- 执行顺序：每个任务都产出可运行的小切片，并保持频繁提交。

