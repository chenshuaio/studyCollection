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

## 本地数据库

使用 MySQL 8 初始化第一版表结构：

```powershell
mysql -u root -p < scripts/mysql-init.sql
```

脚本会创建 `study_collection` 数据库，并初始化用户、题目、选项、题目反馈、题目修订、考试卷、错题和学习报告等核心表。

`user-service` 已接入 MyBatis-Plus 风格的用户持久化适配器。使用本地 MySQL 时启动 `local` profile：

```powershell
cd backend
$env:STUDY_COLLECTION_DB_USER="root"
$env:STUDY_COLLECTION_DB_PASSWORD="你的密码"
mvn -pl user-service spring-boot:run -Dspring-boot.run.profiles=local
```

默认连接地址是 `jdbc:mysql://127.0.0.1:3306/study_collection`，也可以通过 `STUDY_COLLECTION_DB_URL` 覆盖。

## 题目反馈接口

- `POST /questions/feedback`：用户提交题目答案、解析、题干、选项、知识点或难度问题反馈。
- `GET /questions/feedback/pending`：管理员查看待处理反馈。
- `POST /questions/feedback/{feedbackId}/accept`：管理员采纳反馈并生成题目修订记录。

## 题库接口

- `POST /questions`：新增题目，字段包含题干、题型、难度、知识点、标准答案和解析。
- `GET /questions?knowledgePoint=集合框架&difficulty=INTERMEDIATE&type=SINGLE_CHOICE`：按知识点、难度和题型查询题目。

前端题库管理入口：`/questions`。

## 账号接口

- `POST /auth/register`：创建普通学习账号，当前本地实现使用内存仓储，后续可替换为 MyBatis-Plus + MySQL。
- `POST /auth/login`：账号密码登录，返回签名 token、角色和展示名称。

本地内置账号：

- 管理员：`admin` / `admin123`
- 学习用户：`user` / `user123`

第一版接口使用统一响应结构：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```
