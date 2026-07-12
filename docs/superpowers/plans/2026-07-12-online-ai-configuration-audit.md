# 在线 AI 配置与调用审计实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 补齐管理员在线模型配置、OpenAI 兼容响应解析、连接测试、调用审计和失败自动回退，让在线/离线报告模式在本地 MySQL 环境中真实可用。

**架构：** `ai-service` 负责非敏感设置、环境密钥、HTTP 客户端、调用审计和管理员 API；MySQL 与内存仓储保持同一接口。`report-service` 只传递当前用户和学习摘要，AI 失败仍返回规则建议。前端新增管理员 AI 设置页，不向浏览器或数据库暴露 API 密钥。

**技术栈：** Java 17、Spring Boot、JdbcTemplate、Java HttpClient、Jackson、MySQL 8、JUnit 5、AssertJ、Vue 3、TypeScript、Vitest、Vite。

---

### 任务 1：在线模型设置与 OpenAI 兼容客户端

**文件：**
- 修改：`backend/ai-service/pom.xml`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiEnvironmentConfig.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiModelSettings.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiModelSettingsRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/InMemoryAiModelSettingsRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/MySqlAiModelSettingsRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiSettingsService.java`
- 修改：`backend/ai-service/src/main/java/com/studycollection/ai/app/OnlineModelClient.java`
- 修改：`backend/ai-service/src/main/java/com/studycollection/ai/app/HttpOnlineModelClient.java`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/app/AiSettingsServiceTest.java`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/app/HttpOnlineModelClientTest.java`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/app/MySqlAiModelSettingsRepositoryTest.java`

- [ ] 写失败测试，固定设置语义：数据库设置优先于环境端点/模型，环境变量只提供 API 密钥，接口只返回 `apiKeyConfigured` 布尔值。
- [ ] 写失败测试，固定校验规则：端点仅允许无用户信息的 `http/https` URI，模型名称不能为空，提供方固定为 `OPENAI_COMPATIBLE`。
- [ ] 写本地 `HttpServer` 失败测试，断言请求包含 Bearer 头和以下标准结构，并从 `choices[0].message.content` 提取文本：

```json
{
  "model": "qwen-plus",
  "messages": [
    {"role": "system", "content": "你是 Java 学习分析助手。"},
    {"role": "user", "content": "JVM 正确率 20%"}
  ],
  "temperature": 0.2
}
```

- [ ] 增加 HTTP 非 2xx、非法 JSON、缺少 `choices`、空内容和超时测试，错误消息只包含状态或归一化原因，不拼接远端完整响应。
- [ ] 运行 `mvn -pl ai-service -am test`，确认测试因设置模型、仓储和标准解析尚未实现而失败。
- [ ] 实现 `AiModelSettings`、环境默认值合并、内存/MySQL 仓储和 `AiSettingsService`；保存时只写端点、模型、修改人和时间。
- [ ] 使用 Jackson 生成请求并解析响应；让 `OnlineModelClient` 保持函数式接口，并通过默认方法提供当前 provider/model 元数据，兼容现有 lambda 测试。
- [ ] 重跑 `mvn -pl ai-service -am test`，确认设置与客户端测试全部通过。
- [ ] 提交：`git commit -m "feat: add openai compatible model settings"`。

### 任务 2：调用审计、失败回退与管理员 API

**文件：**
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiCallAudit.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiCallAuditRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/InMemoryAiCallAuditRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/MySqlAiCallAuditRepository.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiServiceConfiguration.java`
- 修改：`backend/ai-service/src/main/java/com/studycollection/ai/app/AiAnalysisService.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/api/AiAdminController.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/api/AiSettingsResponse.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/api/UpdateAiSettingsRequest.java`
- 新建：`backend/ai-service/src/main/java/com/studycollection/ai/api/AiConnectionTestResponse.java`
- 修改：`backend/report-service/src/main/java/com/studycollection/report/app/LearningReportService.java`
- 删除：`backend/report-service/src/main/java/com/studycollection/report/app/ReportServiceConfiguration.java`
- 修改：`scripts/mysql-init.sql`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/app/AiAnalysisServiceTest.java`
- 测试：`backend/ai-service/src/test/java/com/studycollection/ai/api/AiAdminControllerTest.java`
- 测试：`backend/report-service/src/test/java/com/studycollection/report/api/LearningReportControllerTest.java`

- [ ] 写失败测试：在线成功保存 `SUCCESS` 审计；异常、空响应和缺少配置保存 `FALLBACK` 审计并返回 `RULES`；审计包含用户、用途、provider、model 和非负耗时。
- [ ] 写失败测试：失败原因去掉换行并截断到 240 字符，不包含 API 密钥或响应正文；离线规则模式不写在线调用审计。
- [ ] 写失败接口测试：`GET/PUT /ai/settings`、`POST /ai/settings/test`、`GET /ai/audits?limit=50` 返回统一响应，普通用户由现有 `@AdminOnly` 拦截。
- [ ] 写失败报告测试：`LearningReportService` 调用 AI 时传入当前用户编号与 `LEARNING_REPORT`，成功报告来源为 `ONLINE_MODEL`，失败报告来源为 `RULES` 且仍保存。
- [ ] 在 `scripts/mysql-init.sql` 增加幂等表：

```sql
CREATE TABLE IF NOT EXISTS ai_model_settings (
  id BIGINT PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  endpoint VARCHAR(512) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_call_audits (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  purpose VARCHAR(32) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL,
  failure_reason VARCHAR(240) NULL,
  duration_ms BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_ai_audit_created_at (created_at)
);
```

- [ ] 实现审计仓储和 `AiAnalysisService.generate(mode, userId, purpose, summary)`；保留原双参数重载供既有测试和非审计调用兼容。
- [ ] 实现管理员设置、连接测试与最近审计接口；`limit` 必须在 1 到 100，测试连接使用用途 `CONFIG_TEST`。
- [ ] 修改 Spring 配置为注入设置仓储、HTTP 客户端和审计仓储，避免 `report-service` 再创建不可配置的默认客户端。
- [ ] 运行 `mvn test`，确认 AI、报告和本地应用上下文全部通过。
- [ ] 提交：`git commit -m "feat: audit online ai calls"`。

### 任务 3：管理员 AI 设置与审计页面

**文件：**
- 修改：`frontend/src/api.ts`
- 修改：`frontend/src/api.test.ts`
- 修改：`frontend/src/router.ts`
- 修改：`frontend/src/router.test.ts`
- 新建：`frontend/src/pages/AiSettingsPage.vue`
- 新建：`frontend/src/pages/AiSettingsPage.test.ts`
- 修改：`frontend/src/pages/DashboardPage.vue`
- 修改：`frontend/src/pages/DashboardPage.test.ts`
- 修改：`frontend/src/pages/ReportPage.vue`

- [ ] 写失败 API 测试，固定 `getAiSettings`、`updateAiSettings`、`testAiConnection` 和 `listAiAudits` 的路径、方法、令牌和中文错误解析。
- [ ] 写失败路由测试：管理员可进入 `/ai-settings`，普通用户被重定向到控制台；管理员控制台和报告页显示“AI 设置”入口。
- [ ] 写失败页面测试：加载时展示端点、模型、密钥配置状态和审计；保存后刷新有效设置；连接测试成功/失败均显示后端真实消息；不会出现密钥输入框或密钥值。
- [ ] 实现 TypeScript 类型和 API 函数，审计状态映射为“成功 / 已回退”，用途映射为“学习报告 / 配置测试”。
- [ ] 实现 `AiSettingsPage.vue`：使用紧凑表单、状态提示、保存/测试按钮和可内部滚动的审计表；按钮执行期间禁用以防重复提交。
- [ ] 在管理员控制台与报告页加入导航入口，并保持普通用户页面无管理入口。
- [ ] 运行 `npm test`、`npm run typecheck`、`npm run build`，确认前端测试、类型与生产构建通过。
- [ ] 提交：`git commit -m "feat: add ai settings admin page"`。

### 任务 4：真实 MySQL、响应式页面、文档与发布验收

**文件：**
- 新建：`scripts/e2e-ai-settings.ps1`
- 修改：`README.md`
- 修改：`docs/project-completeness-2026-07-11.md`
- 修改：`docs/superpowers/plans/2026-07-12-online-ai-configuration-audit.md`

- [ ] 使用本地假 OpenAI 服务和 MySQL `root/root` 验证：管理员保存端点/模型、测试连接成功、用户在线报告返回模型建议、审计分别记录 `CONFIG_TEST` 与 `LEARNING_REPORT`。
- [ ] 停止假模型或返回非法响应，验证在线报告自动回退 `RULES`、报告仍保存、审计记录脱敏失败原因；结束后清理临时设置、用户、作答、报告和审计数据。
- [ ] 在 1280x720 与 390x844 检查 AI 设置页，确认表单、状态、按钮、审计表内部滚动和文字可用且无页面级横向溢出。
- [ ] 更新 README，说明 `STUDY_COLLECTION_AI_API_KEY`、可选默认端点/模型、管理员配置页、在线失败回退与审计不保存敏感内容。
- [ ] 更新完整度矩阵，将标准在线模型配置/调用审计标记为已实现，并重新列出剩余数据库并发约束等差距。
- [ ] 运行后端全量 Maven、前端全量 Vitest、类型检查、生产构建、两个真实 MySQL E2E 脚本和 `git diff --check`。
- [ ] 提交代码，运行 `.\scripts\publish-local.ps1 -UseMysql -RestartLocal`，验证本地与新公网入口后推送 `codex/java-learning-platform-implementation` 分支。
