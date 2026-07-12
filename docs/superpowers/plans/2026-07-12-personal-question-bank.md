# 个人题库与题目可见性实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 普通用户可以导入并使用自己的个人题库，也可以申请将题目公开；任何导入题都必须经过管理员审核，用户只能查看和使用公共题及本人个人题，管理员模拟考试只使用公共题。

**架构：** 启用数据库已经预留的 `questions.owner_user_id`：空值代表公共题，用户编号代表个人题；新增 `QuestionBankScope` 统一表达 `ALL`、`PUBLIC`、`PERSONAL` 查询范围。待审核题保存目标范围，审核通过时写入公共或提交人的个人题库；仓储提供可见性查询和所有权删除方法，练习、考试、反馈、错题和报告推荐均从服务端执行访问校验，避免只靠前端隐藏。

**技术栈：** Java 17、Spring Boot、JdbcTemplate、MySQL 8、JUnit 5、AssertJ、Vue 3、TypeScript、Vitest、Vite。

---

### 任务 1：个人题库领域模型、审核目标与持久化

**文件：**
- 新建：`backend/question-service/src/main/java/com/studycollection/question/domain/QuestionBankScope.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/domain/Question.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/domain/PendingQuestion.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/api/SubmitPendingQuestionRequest.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/api/PendingQuestionController.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/QuestionRepository.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/InMemoryQuestionRepository.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/MySqlQuestionRepository.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/InMemoryPendingQuestionRepository.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/MySqlPendingQuestionRepository.java`
- 修改：`scripts/mysql-init.sql`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/app/InMemoryQuestionRepositoryTest.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/app/MySqlQuestionRepositoryTest.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/api/PendingQuestionControllerTest.java`

- [ ] 写失败测试：公共题对所有用户可见，个人题只对所有者可见，`PUBLIC`、`PERSONAL`、`ALL` 三种范围返回正确集合，其他用户无法删除不属于自己的个人题。
- [ ] 运行 `mvn -pl question-service -am test`，确认测试因所有者模型和可见范围尚未实现而失败。
- [ ] 为 `Question` 增加 `ownerUserId` 并保留七参数公共题兼容构造；实现 `QuestionBankScope`，其中待审核目标只允许 `PUBLIC` 或 `PERSONAL`。
- [ ] 为仓储增加 `searchAccessible`、`findAccessibleById` 和 `deleteOwnedById`；内存与 MySQL 实现相同行为，未授权统一返回“题目不存在或无权访问”。
- [ ] 写失败测试：用户提交个人题和公开申请后均进入待审核；审核个人题时所有者为提交用户，审核公开题时所有者为空；旧客户端未传目标范围时保持 `PUBLIC` 兼容行为。
- [ ] 在 `pending_questions` 增加 `target_scope VARCHAR(16) NOT NULL DEFAULT 'PUBLIC'` 的幂等迁移，并为 `questions.owner_user_id` 增加筛选索引；更新待审核内存/MySQL 仓储和审核入库映射。
- [ ] 重跑题库模块测试，确认领域、审核和两种仓储全部通过。

### 任务 2：查询、练习、考试与反馈的服务端权限边界

**文件：**
- 修改：`backend/question-service/src/main/java/com/studycollection/question/api/QuestionController.java`
- 修改：`backend/question-service/src/main/java/com/studycollection/question/app/QuestionFeedbackService.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeGenerateRequest.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/api/PracticeController.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/PracticeGenerator.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/ExamSessionService.java`
- 修改：`backend/exam-service/src/main/java/com/studycollection/exam/app/RuleBasedExamGenerator.java`
- 修改：`backend/mistake-service/src/main/java/com/studycollection/mistake/app/MistakeService.java`
- 修改：`backend/report-service/src/main/java/com/studycollection/report/app/LearningReportService.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/api/QuestionControllerTest.java`
- 测试：`backend/question-service/src/test/java/com/studycollection/question/app/QuestionFeedbackServiceTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/api/PracticeControllerTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/PracticeGeneratorTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/ExamSessionServiceTest.java`
- 测试：`backend/exam-service/src/test/java/com/studycollection/exam/app/RuleBasedExamGeneratorTest.java`
- 测试：`backend/mistake-service/src/test/java/com/studycollection/mistake/api/MistakeControllerTest.java`
- 测试：`backend/report-service/src/test/java/com/studycollection/report/api/LearningReportControllerTest.java`

- [ ] 写失败接口测试：普通用户查询只能得到公共题和本人个人题，答案与解析仍不提前泄露；普通用户只能删除本人个人题，管理员保留公共题管理能力。
- [ ] 写失败学习链路测试：练习生成支持题库范围，提交答案、个人组卷、错题记录和题目反馈拒绝访问其他用户的个人题。
- [ ] 写失败规则与报告测试：管理员模拟考试只从公共题库组卷；薄弱点强化推荐包含公共题和当前用户个人题，不包含其他用户个人题。
- [ ] 运行相关模块定向测试，逐项确认因访问边界缺失而失败。
- [ ] 改造题库查询和删除控制器；管理员直接创建仍只创建公共题，普通用户不能绕过审核直接创建正式题。
- [ ] 让 `PracticeGenerator` 接收当前用户和范围；练习提交、个人组卷、错题和反馈在读取题目时使用 `findAccessibleById`，并保留历史考试快照不受后续可见性变化影响。
- [ ] 让 `RuleBasedExamGenerator` 固定查询 `PUBLIC`，让报告推荐按当前用户查询 `ALL`；题目反馈修订必须保留原题 `ownerUserId`，不能把个人题意外转为公共题。
- [ ] 重跑后端全量 `mvn test`，确认权限边界与既有学习闭环无回归。

### 任务 3：前端个人题库、导入范围和组卷范围

**文件：**
- 修改：`frontend/src/api.ts`
- 修改：`frontend/src/api.test.ts`
- 修改：`frontend/src/router.ts`
- 修改：`frontend/src/router.test.ts`
- 修改：`frontend/src/pages/QuestionBankPage.vue`
- 修改：`frontend/src/pages/QuestionBankPage.test.ts`
- 修改：`frontend/src/pages/ImportPage.vue`
- 修改：`frontend/src/pages/ImportPage.test.ts`
- 修改：`frontend/src/pages/PracticePage.vue`
- 修改：`frontend/src/pages/PracticePage.test.ts`
- 修改：`frontend/src/pages/ExamPage.vue`
- 修改：`frontend/src/pages/ExamPage.test.ts`
- 修改：`frontend/src/pages/DashboardPage.vue`
- 修改：`frontend/src/pages/DashboardPage.test.ts`
- 修改：`frontend/src/pages/ExamTakingPage.vue`
- 修改：`frontend/src/pages/MistakePage.vue`
- 修改：`frontend/src/pages/ReportPage.vue`
- 修改：`frontend/src/pages/FeedbackReviewPage.vue`
- 修改：`frontend/src/pages/UserManagementPage.vue`
- 修改：`frontend/src/pages/KnowledgePointPage.vue`
- 修改：`frontend/src/pages/ExamRuleManagementPage.vue`

- [ ] 写失败 API 测试：题库搜索和练习生成发送 `scope`，待审核提交发送 `targetScope`，统一令牌与错误解析保持不变。
- [ ] 写失败路由与题库页面测试：普通用户可进入 `/questions`，按“全部可用、公共题库、我的题库”筛选，只能删除自己的个人题；管理员仍看到新增、审核、答案和公共题管理操作。
- [ ] 写失败导入页面测试：结构化题和学习资料生成题共用“个人题库 / 申请公开”分段选择，默认个人题库，提交后明确提示正在等待管理员审核。
- [ ] 写失败练习与考试测试：练习中心和个人组卷均可选择题库范围；普通用户侧边栏出现“我的题库”，管理员显示“题库管理”。
- [ ] 实现 TypeScript 类型、查询参数、路由权限与角色化题库页面；用户列表不展示标准答案列，管理员审核列表展示目标范围。
- [ ] 实现导入目标范围、练习范围和个人组卷范围控件，并保持桌面与手机端稳定尺寸、无页面级横向滚动。
- [ ] 运行 `npm test`、`npm run typecheck` 和 `npm run build`，确认前端行为与生产构建通过。

### 任务 4：真实 MySQL、响应式页面、文档与发布验收

**文件：**
- 新建：`scripts/e2e-personal-question-bank.ps1`
- 修改：`docs/project-completeness-2026-07-11.md`
- 修改：`README.md`
- 修改：`docs/superpowers/plans/2026-07-12-personal-question-bank.md`

- [ ] 使用 MySQL `root/root` 验证：用户 A 提交个人题和公开申请，管理员审核后，A 可见两题、用户 B 只能看到公开题，管理员模拟考试不抽取个人题。
- [ ] 验证用户 A 可用个人题生成练习和个人考试，用户 B 通过搜索、交卷、反馈、错题和自定义组卷均无法访问 A 的个人题；结束后清理临时用户、题目、记录和审核数据。
- [ ] 在 1280x720 与 390x844 检查个人题库、导入、练习和考试页面，确认筛选控件、表格内部滚动、按钮和文字均可操作且控制台无错误。
- [ ] 更新完整度矩阵与 README，将个人题库和题目可见范围标记为已实现，并记录公共/个人题都先审核的规则。
- [ ] 运行后端全量 Maven、前端全量 Vitest、类型检查、生产构建和 `git diff --check`。
- [ ] 提交代码，运行 `.\scripts\publish-local.ps1 -UseMysql -RestartLocal`，验证本地与新公网入口后推送 `codex/java-learning-platform-implementation` 分支。
