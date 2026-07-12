# 管理员考试规则与模拟考试实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 管理员可以配置、发布和停用模拟考试规则，普通用户可以从已发布规则启动一场满足题型、难度、知识点、题量和时限约束的模拟考试。

**架构：** `exam-service` 新增独立的考试规则领域模型、内存/MySQL 仓储和规则组卷器；规则启动后只把选中的题目复制为现有 `ExamSession` 快照，继续复用计时、自动保存、评分、错题和报告链路。前端新增管理员规则页，并在考试中心展示可参加的模拟考试，同时保留个人手动组卷。

**技术栈：** Java 17、Spring Boot、JdbcTemplate、MySQL 8、JUnit 5、AssertJ、Vue 3、TypeScript、Vitest、Vite。

---

### 任务 1：考试规则模型、校验与精确配额组卷

**文件：**
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamRule.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/domain/ExamRuleStatus.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/app/RuleBasedExamGenerator.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/RuleBasedExamGeneratorTest.java`

- [x] 写失败测试：规则名称、题量和时限合法，题型配额与难度配额各自之和必须等于总题量，空知识点表示全部知识点。
- [x] 运行 `mvn -pl exam-service -am -Dtest=RuleBasedExamGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test`，确认因模型和生成器尚不存在而失败。
- [x] 实现不可变规则模型；总题量限制为 1 至 200，时限限制为 1 至 480 分钟，配额不得为负数或包含空键。
- [x] 写失败测试：生成器只使用指定知识点，并同时精确满足题型和难度两个维度的配额；题库容量无法满足联合分布时返回明确中文错误。
- [x] 实现按“题型 × 难度”候选单元分配数量的回溯求解，再对每个单元随机抽题，避免简单贪心导致存在可行解却组卷失败。
- [x] 重跑定向测试，确认全部通过。

### 任务 2：规则持久化、管理员生命周期与模拟考试启动

**文件：**
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/app/ExamRuleRepository.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/app/InMemoryExamRuleRepository.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/app/MySqlExamRuleRepository.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/app/ExamRuleService.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/api/ExamRuleRequest.java`
- 新建：`backend/exam-service/src/main/java/com/studycollection/exam/api/ExamRuleController.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/ExamSessionService.java`
- 修改：`scripts/mysql-init.sql`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/ExamRuleServiceTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/api/ExamRuleControllerTest.java`

- [x] 写失败测试：管理员可创建草稿、修改、发布、停用和删除规则；修改已发布规则后自动回到草稿，普通列表只返回已发布规则。
- [x] 写失败测试：发布时校验当前公共题库存在可行组卷，启动时再次校验并生成属于当前用户的考试会话。
- [x] 运行考试模块测试，确认因仓储、服务和控制器尚不存在而失败。
- [x] 实现内存仓储与服务；`GET /exam-rules` 和 `POST /exam-rules/{id}/start` 面向已登录用户，`/exam-rules/admin` 及创建、修改、发布、停用、删除接口使用 `@AdminOnly`。
- [x] 实现 MySQL `exam_rules` 表与仓储，列表、知识点和配额使用 JSON 文本持久化，保存创建人、创建时间和更新时间。
- [x] 让规则启动调用现有会话快照创建逻辑，确保后续规则或原题修改不改变已开始考试。
- [x] 重跑 `mvn -pl exam-service -am test`，确认规则与既有考试测试全部通过。

### 任务 3：前端考试规则管理与模拟考试入口

**文件：**
- 新建：`frontend/src/pages/ExamRuleManagementPage.vue`
- 新建：`frontend/src/pages/ExamRuleManagementPage.test.ts`
- 修改：`frontend/src/api.ts`
- 修改：`frontend/src/api.test.ts`
- 修改：`frontend/src/router.ts`
- 修改：`frontend/src/router.test.ts`
- 修改：`frontend/src/pages/ExamPage.vue`
- 修改：`frontend/src/pages/ExamPage.test.ts`
- 修改：`frontend/src/pages/DashboardPage.vue`
- 修改：`frontend/src/pages/QuestionBankPage.vue`
- 修改：`frontend/src/pages/ImportPage.vue`
- 修改：`frontend/src/pages/PracticePage.vue`
- 修改：`frontend/src/pages/ExamTakingPage.vue`
- 修改：`frontend/src/pages/MistakePage.vue`
- 修改：`frontend/src/pages/ReportPage.vue`
- 修改：`frontend/src/pages/FeedbackReviewPage.vue`
- 修改：`frontend/src/pages/UserManagementPage.vue`
- 修改：`frontend/src/pages/KnowledgePointPage.vue`

- [x] 写失败 API 测试：规则管理、发布/停用/删除、公开列表和启动模拟考试均携带登录令牌并解析统一响应。
- [x] 写失败页面测试：管理员可编辑所有六种题型和三种难度配额，看到配额合计校验、规则状态及操作按钮；普通用户考试中心只显示已发布规则。
- [x] 写失败路由测试：`/exam-rules/manage` 仅管理员可访问。
- [x] 实现 TypeScript 类型和请求函数、管理员规则管理页、路由守卫与管理员侧边栏入口。
- [x] 改造考试中心：顶部展示模拟考试名称、说明、题量、时限、知识点和配额，点击后创建会话并进入现有答题页；个人手动组卷保持可用。
- [x] 运行 `npm test -- --run` 与 `npm run typecheck`，确认前端测试和类型检查通过。

### 任务 4：真实数据库、响应式页面与发布验收

**文件：**
- 修改：`docs/project-completeness-2026-07-11.md`
- 修改：`docs/superpowers/plans/2026-07-12-admin-exam-rules.md`

- [x] 运行后端全量 Maven 测试、前端全量 Vitest、`vue-tsc --noEmit` 和 Vite 生产构建。
- [x] 使用 MySQL `root/root` 完成管理员创建并发布规则、普通用户查询并启动、精确配额校验、答题提交和历史记录端到端验证，结束后清理临时数据。
- [x] 在 1280x720 与 390x844 检查规则管理页、考试中心和答题跳转，无页面级横向溢出、文字遮挡或不可操作控件，并检查浏览器控制台无错误。
- [x] 更新完整度矩阵，将管理员考试规则与模拟考试标记为已实现。
- [x] 运行 `.\scripts\publish-local.ps1 -UseMysql -RestartLocal`，验证本地前后端和新的公网入口。
- [ ] 提交并推送 `codex/java-learning-platform-implementation` 分支。
