# Local Flow

1. 用户通过登录页登录。
2. 用户上传或粘贴结构化题目内容，并进入解析预览。
3. 用户也可以上传或粘贴 Java 学习知识内容，由系统分析生成题库草稿。
4. 用户确认生成题或导入题并提交审核，管理员通过后进入公共题库。
5. 用户选择知识点、难度、题型生成练习。
6. 用户也可以手动组合题目生成考试卷。
7. 用户提交答案后，系统返回得分、逐题正误和解析。
8. 答错题目进入错题整理候选。
9. 用户发现题目答案或解析错误时提交题目反馈。
10. 管理员审核反馈并修订题库。
11. 系统生成规则报告，或在开启在线模型时生成 AI 学习建议。

## 本地启动

默认无数据库也能先跑通学习闭环：

```powershell
.\scripts\start-local.ps1
```

本地聚合后端由 `backend/local-app` 启动，默认端口为 `18080`。前端由 Vite 启动，默认端口为 `5173`，并通过 `/api` 代理访问后端。

## 本地数据库

MySQL 本地账号：

- 用户名：`root`
- 密码：`root`
- 数据库：`study_collection`

初始化数据库：

```powershell
mysql -u root -proot < scripts/mysql-init.sql
```

带 root/root 环境变量启动：

```powershell
.\scripts\start-local.ps1 -UseMysql
```

`-UseMysql` 会启用后端 `local-mysql` profile。默认连接地址是 `jdbc:mysql://127.0.0.1:3306/study_collection`，默认用户名和密码均为 `root`，也可以通过 `STUDY_COLLECTION_DB_URL`、`STUDY_COLLECTION_DB_USER`、`STUDY_COLLECTION_DB_PASSWORD` 覆盖。

## 账号接口

- `POST /auth/register`：创建普通学习账号。默认本地启动使用内存仓储，`-UseMysql` 启动时使用 MySQL 用户表。
- `POST /auth/login`：账号密码登录，返回签名 token、用户编号、账号、角色和展示名称。

登录和注册响应中的 `data` 均包含：

```json
{
  "token": "signed-token",
  "userId": 2,
  "username": "user",
  "role": "USER",
  "displayName": "学习用户"
}
```

除 `/auth/login` 和 `/auth/register` 外，请求需要携带：

```http
Authorization: Bearer signed-token
```

本地内置账号：

- 管理员：`admin` / `admin123`
- 学习用户：`user` / `user123`

## 题库接口

- `POST /questions`：管理员新增题目，字段包含题干、题型、难度、知识点、标准答案和解析。
- `GET /questions?knowledgePoint=集合框架&difficulty=INTERMEDIATE&type=SINGLE_CHOICE`：按知识点、难度和题型查询题目。

普通用户查询题库时不会收到标准答案和解析；管理员题库管理页面可以查看完整字段。练习评分始终从服务端题库读取标准答案，不接受客户端上传“正确答案”。

管理员删除题目使用软删除：题目会从后续搜索、练习和判分中排除，但已存在的反馈和修订关联继续保留。

前端题库管理入口：`/questions`。

## 导入与生成接口

- `POST /imports/preview`：提交结构化 Markdown/TXT 题目内容，返回题目预览列表。
- `POST /imports/knowledge/generate`：提交 Java 学习知识内容，返回可入库的题库草稿。
- `POST /imports/knowledge/upload`：上传 Java 学习资料文件，字段名为 `file`，当前支持 `.txt`、`.md`、`.csv`、`.xlsx`、`.docx`、`.pdf`，返回待预览的题库草稿。

结构化题目格式示例：

```markdown
## 单选题
题目: Java 中 int 默认值是多少？
答案: A
知识点: Java 基础
难度: BEGINNER
```

知识内容生成示例：

```text
HashMap 是 Java 集合框架中的常用 Map 实现。
HashMap 默认负载因子是 0.75，达到阈值后会进行扩容。
Java 中局部变量没有默认值，必须先赋值再使用。
```

前端导入入口：`/import`。

## 练习接口

- `POST /practice/submit`：提交练习答案，返回总分、满分、逐题正误、标准答案和解析。

请求示例：

```json
{
  "answers": [
    { "questionId": 1, "answer": "A" },
    { "questionId": 2, "answer": "false" }
  ]
}
```

前端练习入口：`/practice`。

练习统计、错题、待审核题和题目反馈在 `local-mysql` profile 下写入 MySQL。服务端根据令牌中的用户编号读写当前用户数据，忽略请求中伪造的 `userId`。

多选题答案可使用逗号、中文逗号、顿号、分号或空白分隔，服务端会去重排序后判分，例如 `A,C` 与 `C、A` 等价。空答案集合会返回 `VALIDATION_FAILED`。

## 题目反馈接口

- `POST /questions/feedback`：用户提交题目答案、解析、题干、选项、知识点或难度问题反馈。
- `GET /questions/feedback/pending`：管理员查看待处理反馈。
- `POST /questions/feedback/{feedbackId}/accept`：管理员采纳反馈并生成题目修订记录。

第一版接口使用统一响应结构：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```
