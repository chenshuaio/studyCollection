# StudyCollection 完整度与差距矩阵

对照基准：`docs/superpowers/specs/2026-06-27-java-learning-platform-design.md`。

更新时间：2026-07-11。

## 当前结论

- **本地基础可用性：约 9/10。** 登录、注册、角色隔离、结构化题目导入审核、学习资料出题、公共题库、条件随机多题练习、持久化考试、错题、反馈和可信学习报告可以形成完整基础闭环。
- **主设计功能完整度：约 81%。** 服务端逐题作答、报告历史、近期趋势、题型表现和强化题已补齐；部分实现仍按半项计入。
- **目前不是“全部完结”。** 最大差距集中在控制台近期活动、题目修订影响处理、管理员模拟考试规则、个人题库和标准在线模型配置。

## 需求矩阵

| 设计要求 | 状态 | 当前证据 | 主要差距 |
| --- | --- | --- | --- |
| 注册、登录、普通用户/管理员角色 | 已实现 | `user-service`、`common-lib/security`、登录注册页 | 自有 HMAC 令牌尚非标准 JWT/OIDC |
| 注册用户固定为普通用户，账号/昵称不重复 | 已实现 | `AuthService`、MySQL `users.username` 唯一 | 昵称仅应用层查重，数据库没有唯一索引 |
| 当前账户展示和退出登录 | 已实现 | `CurrentAccount.vue`、`LogoutButton.vue` | 无 |
| 公共题库管理、搜索、模糊查询和软删除 | 已实现 | `QuestionController`、`MySqlQuestionRepository`、`QuestionBankPage.vue` | 选项仍嵌在题干文本中 |
| 普通用户导入，管理员审核后入库 | 已实现 | `PendingQuestionController`、`ImportPage.vue` | 驳回原因尚未独立保存展示 |
| 个人题库管理 | 未实现 | 当前正式题目统一进入公共题库 | 缺少题目可见范围、个人题库页面和查询边界 |
| 知识点分类管理 | 已实现 | `KnowledgePointController`、`KnowledgePointPage.vue` | 无层级知识点和合并能力 |
| 六种题型 | 已实现 | `QuestionType` | 编程题自动判题字段尚未预留到表结构 |
| 客观题评分，多选顺序无关 | 已实现 | `PracticeController`、`ExamSession` | 填空题仍是严格字符串比较 |
| 简答/编程题保存并展示参考答案，不自动评分 | 已实现 | `PracticeResultItem.autoGraded`、考试题快照 | 尚无人工评分入口，第一版不要求 |
| 按知识点、难度、题型、数量随机生成练习 | 已实现 | `PracticeGenerator`、`POST /practice/generate`、`PracticePage.vue` | 当前练习进度仅保存在页面内，刷新后会重新生成 |
| 用户手动组合考试卷 | 已实现 | `CustomExamController`、`ExamPage.vue` | 题目顺序尚不能由用户拖动调整 |
| 考试计时、中断恢复、历史试卷 | 已实现 | `ExamSessionService`、三张考试会话表、`ExamTakingPage.vue` | 多实例并发提交仍需数据库条件更新 |
| 管理员配置模拟考试规则 | 未实现 | 无考试规则领域模型或管理页 | 缺少题型比例、知识点、难度、数量、时限模板 |
| JSON/CSV/XLSX/TXT/MD/PDF/DOCX 导入 | 已实现 | `StructuredQuestionFileParser` 按字段解析 JSON/CSV/XLSX/MD/TXT；`KnowledgeFileTextExtractor` 分析 PDF/DOCX 等学习资料 | PDF/DOCX 走资料分析出题而非严格表格字段映射 |
| 导入候选题可编辑预览 | 已实现 | `EditableQuestionTable.vue` | 候选题提交仍是逐题请求，超大批次尚无服务端事务批量接口 |
| Java 学习资料分析生成题库并预览 | 已实现 | `/imports/knowledge/*`、生成题预览 | 当前以规则生成为主，未接在线模型生成 |
| 错题收集、重练和掌握状态 | 已实现 | `mistake-service`、`MistakePage.vue` | 缺少题型和时间筛选，错题未保存首次/最近错误时间 |
| 题目反馈与管理员采纳/驳回/待复核 | 部分实现 | `QuestionFeedbackService`、审核页 | 缺少重复反馈聚合、用户作答/来源展示、审核备注持久化 |
| 题目修订历史及历史答题快照 | 部分实现 | 修订记录、考试题快照 | 修订记录没有完整修改前/后字段，报告未识别已修订题 |
| 学习控制台真实统计 | 部分实现 | 练习统计、错题、反馈均来自 API；正确率只以可自动评分题目为分母 | 缺少最近练习、最近考试和报告强化推荐摘要 |
| 离线规则报告和在线失败回退 | 已实现 | `LearningReportService`、`AiAnalysisService` | 在线模型失败原因尚未独立审计 |
| 报告历史、正确率趋势、题型表现、考试成绩 | 已实现 | `learning_attempts`、`learning_reports`、报告历史接口与页面 | 尚未按练习/考试活动类型单独拆分趋势 |
| 强化题推荐 | 已实现 | 报告按最薄弱知识点返回最多五道脱敏题目，入口可直接生成定向练习 | 无 |
| 在线模型配置与调用审计 | 部分实现 | 通用 HTTP 客户端和环境变量 | 未兼容 OpenAI 标准响应、无模型配置页、失败审计表 |
| 统一权限和错误响应 | 已实现 | 认证拦截器、管理员注解、异常处理 | 无网关级追踪编号，当前是本地聚合进程 |
| 桌面端与手机端可用 | 已实现 | 响应式 CSS，390px/1280px 浏览器验证 | 管理表格在手机上采用横向滚动 |
| 自动化测试与本地发布 | 已实现 | Maven、`vue-tsc`、Vitest、Vite build、启动/发布脚本 | 尚未加入完整浏览器 E2E 套件 |

## 差距关闭顺序

1. 控制台最近练习/考试、趋势摘要和个性化强化入口。
2. 错题时间/题型筛选、反馈聚合与完整修订历史。
3. 管理员考试规则和模拟考试。
4. 个人题库、标准在线模型配置与数据库并发约束。
5. 在线部署前再拆分网关/独立服务并迁移标准 JWT/OIDC；这不是当前本地学习版的阻塞项。
