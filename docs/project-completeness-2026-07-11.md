# StudyCollection 完整度与差距矩阵

对照基准：`docs/superpowers/specs/2026-06-27-java-learning-platform-design.md`。

更新时间：2026-07-12。

## 当前结论

- **本地基础可用性：约 9.7/10。** 登录、注册、角色隔离、结构化题目导入审核、学习资料出题、公共/个人题库、条件随机多题练习、管理员模拟考试、个人组卷、持久化答题、错题、反馈和可信学习报告已形成完整基础闭环。
- **主设计功能完整度：约 96%。** 个人题目归属、公共/个人审核目标、全学习链路可见性边界、管理员公共题模拟考试和响应式个人题库页面已补齐。
- **目前不是“全部完结”。** 主要差距集中在标准在线模型配置/调用审计和数据库并发约束。

## 需求矩阵

| 设计要求 | 状态 | 当前证据 | 主要差距 |
| --- | --- | --- | --- |
| 注册、登录、普通用户/管理员角色 | 已实现 | `user-service`、`common-lib/security`、登录注册页 | 自有 HMAC 令牌尚非标准 JWT/OIDC |
| 注册用户固定为普通用户，账号/昵称不重复 | 已实现 | `AuthService`、MySQL `users.username` 唯一 | 昵称仅应用层查重，数据库没有唯一索引 |
| 当前账户展示和退出登录 | 已实现 | `CurrentAccount.vue`、`LogoutButton.vue` | 无 |
| 公共题库管理、搜索、模糊查询和软删除 | 已实现 | `QuestionController`、`MySqlQuestionRepository`、`QuestionBankPage.vue` | 选项仍嵌在题干文本中 |
| 普通用户导入，管理员审核后入库 | 已实现 | `PendingQuestionController`、`ImportPage.vue` | 驳回原因尚未独立保存展示 |
| 个人题库管理 | 已实现 | `QuestionBankScope`、题目所有者、审核目标范围、`QuestionBankPage.vue`；普通用户可使用公共题和本人个人题 | 无 |
| 知识点分类管理 | 已实现 | `KnowledgePointController`、`KnowledgePointPage.vue` | 无层级知识点和合并能力 |
| 六种题型 | 已实现 | `QuestionType` | 编程题自动判题字段尚未预留到表结构 |
| 客观题评分，多选顺序无关 | 已实现 | `PracticeController`、`ExamSession` | 填空题仍是严格字符串比较 |
| 简答/编程题保存并展示参考答案，不自动评分 | 已实现 | `PracticeResultItem.autoGraded`、考试题快照 | 尚无人工评分入口，第一版不要求 |
| 按知识点、难度、题型、数量随机生成练习 | 已实现 | `PracticeGenerator`、`POST /practice/generate`、`PracticePage.vue` | 当前练习进度仅保存在页面内，刷新后会重新生成 |
| 用户手动组合考试卷 | 已实现 | `CustomExamController`、`ExamPage.vue` | 题目顺序尚不能由用户拖动调整 |
| 考试计时、中断恢复、历史试卷 | 已实现 | `ExamSessionService`、三张考试会话表、`ExamTakingPage.vue` | 多实例并发提交仍需数据库条件更新 |
| 管理员配置模拟考试规则 | 已实现 | `ExamRuleService`、`RuleBasedExamGenerator`、`exam_rules`、`ExamRuleManagementPage.vue`；支持草稿、发布、停用、删除和精确联合配额组卷，且只使用公共题 | 无 |
| JSON/CSV/XLSX/TXT/MD/PDF/DOCX 导入 | 已实现 | `StructuredQuestionFileParser` 按字段解析 JSON/CSV/XLSX/MD/TXT；`KnowledgeFileTextExtractor` 分析 PDF/DOCX 等学习资料 | PDF/DOCX 走资料分析出题而非严格表格字段映射 |
| 导入候选题可编辑预览 | 已实现 | `EditableQuestionTable.vue` | 候选题提交仍是逐题请求，超大批次尚无服务端事务批量接口 |
| Java 学习资料分析生成题库并预览 | 已实现 | `/imports/knowledge/*`、生成题预览 | 当前以规则生成为主，未接在线模型生成 |
| 错题收集、重练和掌握状态 | 已实现 | 错题题型、首次/最近错误时间、错误次数、组合筛选和服务端题目快照 | 无 |
| 题目反馈与管理员采纳/驳回/待复核 | 已实现 | 练习、考试结果和错题本反馈入口；按题目/类型聚合；用户答案/来源与审核审计 | 普通用户暂无独立题目详情页 |
| 题目修订历史及历史答题快照 | 已实现 | 完整修订前后快照、关联反馈、修改人/时间；历史作答快照不改写 | 无 |
| 学习控制台真实统计 | 已实现 | 累计统计、最近练习批次、最近考试、报告趋势和薄弱点强化入口均来自当前令牌用户 API | 无 |
| 离线规则报告和在线失败回退 | 已实现 | `LearningReportService`、`AiAnalysisService` | 在线模型失败原因尚未独立审计 |
| 报告历史、正确率趋势、题型表现、考试成绩 | 已实现 | `learning_attempts`、`learning_reports`、报告历史接口与页面；支持排除或按当前答案重算已修订题 | 尚未按练习/考试活动类型单独拆分趋势 |
| 强化题推荐 | 已实现 | 报告按最薄弱知识点返回最多五道脱敏题目，入口可直接生成定向练习 | 无 |
| 在线模型配置与调用审计 | 部分实现 | 通用 HTTP 客户端和环境变量 | 未兼容 OpenAI 标准响应、无模型配置页、失败审计表 |
| 统一权限和错误响应 | 已实现 | 认证拦截器、管理员注解、异常处理 | 无网关级追踪编号，当前是本地聚合进程 |
| 桌面端与手机端可用 | 已实现 | 响应式 CSS，390px/1280px 浏览器验证 | 管理表格在手机上采用横向滚动 |
| 自动化测试与本地发布 | 已实现 | Maven、`vue-tsc`、Vitest、Vite build、启动/发布脚本、个人题库真实 MySQL 端到端脚本 | 尚未加入覆盖全部页面的浏览器 E2E 套件 |

## 差距关闭顺序

1. 标准在线模型配置与调用审计。
2. 考试提交等关键写入的数据库并发约束。
3. 在线部署前再拆分网关/独立服务并迁移标准 JWT/OIDC；这不是当前本地学习版的阻塞项。
