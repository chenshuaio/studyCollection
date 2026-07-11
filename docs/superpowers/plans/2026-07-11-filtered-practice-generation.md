# 条件随机多题练习实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 用户可以按知识点、难度、题型和数量从已审核题库随机生成多题练习，逐题作答，并得到口径正确的客观题正确率。

**架构：** `exam-service` 新增练习生成服务，通过 `QuestionRepository` 使用结构化筛选并在服务端随机抽取，不向普通用户返回答案。Vue 练习页维护一次生成结果的当前题号，逐题调用现有服务端评分；统计增加“已自动评分题数”，正确率只使用客观题分母。

**技术栈：** Java 17、Spring Boot、Spring JDBC、MySQL、Vue 3、TypeScript、Vitest。

---

### 任务 1：练习生成领域服务与 API

**文件：**
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeGenerateRequest.java`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/api/GeneratedPractice.java`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/api/GeneratedPracticeQuestion.java`
- 创建：`backend/exam-service/src/main/java/com/studycollection/exam/app/PracticeGenerator.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeController.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/PracticeGeneratorTest.java`
- 修改测试：`backend/exam-service/src/test/java/com/studycollection/exam/api/PracticeControllerTest.java`

- [x] 写失败测试：知识点、难度和题型组合筛选后最多返回请求数量，题目不重复且答案/解析为空。
- [x] 写失败测试：数量必须为 1-100，没有匹配题目时返回明确校验错误。
- [x] 运行 `mvn -pl exam-service -am "-Dtest=PracticeGeneratorTest,PracticeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，确认因生成类型和接口不存在而失败。
- [x] 实现 `PracticeGenerator.generate(knowledgePoint, difficulty, type, count)`，使用可注入 `Random` 的 Fisher-Yates 打乱后截取。
- [x] 实现 `POST /practice/generate`，返回 `requestedCount`、`actualCount` 和脱敏题目列表。
- [x] 重跑聚焦测试并确认通过。

### 任务 2：修正练习统计正确率口径

**文件：**
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeStats.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/PracticeStatsRepository.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/InMemoryPracticeStatsRepository.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/MySqlPracticeStatsRepository.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeController.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/ExamSessionService.java`
- 修改：`scripts/mysql-init.sql`
- 修改测试：`backend/exam-service/src/test/java/com/studycollection/exam/api/PracticeControllerTest.java`
- 修改测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/ExamSessionServiceTest.java`

- [x] 写失败测试：回答一道简答题后 `answeredQuestionCount=1`、`gradedQuestionCount=0`、正确率分母不增加。
- [x] 写失败测试：考试中的客观题和主观题分别累计已作答与已评分数量，重复提交不重复累计。
- [x] 给 `practice_stats` 增加幂等迁移列 `graded_question_count`，历史行按 `answered_question_count` 初始化以保持旧数据兼容。
- [x] 将仓库 `add` 改为同时累计 answered、graded、correct，并更新所有调用方。
- [x] 运行全部 `exam-service` 测试。

### 任务 3：Vue 条件生成与多题练习流程

**文件：**
- 修改：`frontend/src/api.ts`
- 修改：`frontend/src/api.test.ts`
- 修改：`frontend/src/pages/PracticePage.vue`
- 修改：`frontend/src/pages/PracticePage.test.ts`
- 修改：`frontend/src/pages/DashboardPage.vue`
- 修改：`frontend/src/pages/DashboardPage.test.ts`
- 修改：`frontend/src/styles/theme.css`

- [x] 写失败 API 测试，验证 `/practice/generate` 的筛选请求体。
- [x] 写失败页面测试，验证知识点、难度、题型、数量控件和生成按钮。
- [x] 写失败页面测试，生成三题后显示 `1/3`，提交第一题后可进入下一题，最后显示练习完成摘要。
- [x] 实现筛选表单，知识点来自 `/knowledge-points`，默认数量为 10。
- [x] 用服务端生成结果替代“题库第一题”，保留错题本单题重练入口。
- [x] 更新控制台正确率为 `correctQuestionCount / gradedQuestionCount`。
- [x] 运行聚焦测试、全部 Vitest 和 Vite build。

### 任务 4：真实流程验证与发布

- [x] 应用 MySQL 迁移并以 `root/root` 重启。
- [x] 通过普通账号生成筛选练习，确认题目数量、无答案泄漏、逐题提交和统计变化。
- [x] 在 1280x720 与 390x844 检查页面无重叠和页面级横向溢出。
- [x] 运行 `scripts/verify-local.ps1`，重启 Cloudflare 临时发布，提交并推送分支。
