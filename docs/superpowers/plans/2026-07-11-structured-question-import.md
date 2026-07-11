# 结构化题目批量导入与可编辑预览实施计划

> **执行要求：** 使用测试驱动开发；每项先观察失败，再实现并回归。

**目标：** 用户可以上传 JSON、CSV、XLSX、TXT 或 Markdown 结构化题目文件，批量解析完整题目字段；也可以继续上传 PDF、DOCX 等 Java 学习资料生成题目。两类候选题都必须在可编辑预览中确认后，才提交管理员审核。

**架构：** `import-service` 增加独立的结构化题目文件解析器和 `/imports/questions/upload` 接口，使用 Jackson、Apache Commons CSV 和 Apache POI 解析对应格式；Markdown/TXT 解析器扩展为多题和完整字段。Vue 使用共享的可编辑题目表格组件维护草稿，提交时沿用现有待审核题接口，不绕过管理员审核。

**技术栈：** Java 17、Spring Boot、Jackson、Apache Commons CSV、Apache POI、Vue 3、TypeScript、Vitest。

---

### 任务 1：完整 Markdown/TXT 多题解析

- [x] 写失败测试：一次文本解析两道不同题型，保留选项、答案、解析、知识点和难度。
- [x] 写失败测试：缺少题目、答案、知识点或难度时返回包含题号和字段名的错误。
- [x] 扩展 `ParsedQuestion` 为完整题目字段，增强 `MarkdownQuestionParser` 的分块、题型映射和校验。
- [x] 更新预览控制器测试并确认兼容原有单题格式。

### 任务 2：JSON、CSV 与 XLSX 结构化文件解析接口

- [x] 写失败测试：JSON 数组和 `{ questions: [...] }` 均可批量解析。
- [x] 写失败测试：带引号、逗号和选项列的 CSV 可正确解析多题。
- [x] 写失败测试：XLSX 按表头映射字段，空行跳过，错误行返回明确原因。
- [x] 新增 `StructuredQuestionFileParser`，对扩展名白名单、空文件、字段值和枚举进行校验。
- [x] 新增 `POST /imports/questions/upload`，响应统一为完整题目草稿列表。
- [x] 运行全部 `import-service` 测试。

### 任务 3：可编辑预览与双上传路径

- [x] 写失败 API 测试：结构化题目文件上传使用 `/imports/questions/upload`。
- [x] 写失败页面测试：上传 JSON 后只预览不入库，编辑全部字段后提交审核。
- [x] 写失败页面测试：预览题可新增、删除；学习资料生成题也可编辑。
- [x] 新增共享可编辑题目表格组件，提供题干、题型、难度、知识点、答案、解析和删除操作。
- [x] 在导入页明确分开“结构化题目文件”和“Java 学习资料”上传入口。
- [x] 更新 API 类型、中文说明、README 和本地接口文档。
- [x] 运行全部 Vitest、类型检查和 Vite build。

### 任务 4：真实文件验收与发布

- [x] 用普通账号上传 JSON、CSV 和 XLSX，确认解析数量和字段一致。
- [x] 编辑预览后提交审核，管理员通过后可在题库模糊搜索到题目。
- [x] 在 1280x720 和 390x844 检查编辑表格、上传控件无重叠或页面级溢出。
- [x] 运行全量验证，重启本地服务和公网临时发布，提交并推送分支。
