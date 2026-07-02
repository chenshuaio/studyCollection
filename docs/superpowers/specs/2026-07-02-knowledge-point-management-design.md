# 知识点分类管理设计

**目标：** 管理员可以维护 Java 学习知识点目录，用于题库筛选、导入审核、报告分析和后续出题规则配置。

## 范围

- 管理员可查看知识点列表。
- 管理员可新增知识点，字段包括名称、描述、启用状态。
- 管理员可停用知识点，停用后不从目录中删除历史记录。
- 第一版不强制题目 `knowledgePoint` 必须引用目录项，避免破坏现有题库数据。

## 后端设计

在 `question-service` 中新增知识点目录模型：

- `KnowledgePoint`
- `KnowledgePointRepository`
- `InMemoryKnowledgePointRepository`
- `MySqlKnowledgePointRepository`
- `KnowledgePointController`

接口：

- `GET /knowledge-points`：返回全部知识点。
- `POST /knowledge-points`：新增知识点。
- `POST /knowledge-points/{id}/disable`：停用知识点。

MySQL 初始化脚本新增 `knowledge_points` 表，并写入常用默认知识点：Java 基础、集合框架、面向对象、JVM、异常处理、并发编程。

## 前端设计

新增管理员页面 `/knowledge-points`。页面显示知识点表格，支持新增名称和描述，支持停用启用中的知识点。管理员侧边导航增加“知识点管理”入口，普通用户不可访问。

## 验证

- 后端 controller 测试覆盖新增、列表、停用。
- 前端 API 测试覆盖知识点接口。
- 前端页面测试覆盖列表、新增和停用。
- 运行后端 question-service 测试、前端测试、前端构建。
