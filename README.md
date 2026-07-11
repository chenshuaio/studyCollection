# StudyCollection

Java 学习题库平台，本地开发版支持账号登录、题库管理、结构化题目导入、Java 学习知识内容生成题库、练习答题、错题反馈和学习分析的基础闭环。

## 本地启动

确保本机已安装 Java 17+、Maven、Node.js 和 npm。

```powershell
.\scripts\start-local.ps1
```

启动后访问：

- 前端页面：http://127.0.0.1:5173
- 后端 API：http://127.0.0.1:18080

本地内置账号：

- 学习用户：`user` / `user123`
- 管理员：`admin` / `admin123`

停止服务：

```powershell
.\scripts\stop-local.ps1
```

日志目录：`.local/logs`

## MySQL 本地账号

你的本地 MySQL 账号可以使用：

- 用户名：`root`
- 密码：`root`
- 数据库：`study_collection`

初始化数据库：

```powershell
mysql -u root -proot < scripts/mysql-init.sql
```

如果需要启动时注入 root/root 环境变量：

```powershell
.\scripts\start-local.ps1 -UseMysql
```

默认启动会使用内存仓储，方便没有数据库时快速体验；使用 `-UseMysql` 启动会启用 `local-mysql` profile，账号、题库、待审核题、题目反馈、练习统计和错题会保存到本地 MySQL，重启后仍可继续使用。管理员删除题目采用软删除，既有反馈与修订记录不会因删除题目而丢失。

## Java 知识内容生成题库

进入前端 `/import` 页面，在“学习内容生成题库”区域粘贴 Java 学习材料，例如 HashMap、局部变量、面向对象等知识内容，点击“分析生成题库”即可调用后端规则生成题库草稿。

也可以点击“上传学习资料”选择 `.txt`、`.md`、`.csv`、`.xlsx`、`.docx` 或 `.pdf` 文件，前端会通过后端上传接口提取文本并生成题库草稿。确认无误后提交管理员审核，通过后进入公共题库。

## 登录与接口权限

除注册和登录外，后端接口都要求登录令牌。前端会自动发送 `Authorization: Bearer <token>`；管理员题库、审核、知识点和用户管理接口还会在后端校验管理员角色。令牌默认有效期为 12 小时，重启本地后端后需要重新登录。

新注册密码使用 PBKDF2 加盐哈希保存。现有 `{plain}` 本地演示账号会在首次成功登录后自动升级，不影响 `admin/admin123` 和 `user/user123` 的使用。

单选题、判断题和多选题会显示未预选的选项控件；历史题目缺少选项文本时会提供 `A-D` 占位选项。多选答案按选项集合判分，选择顺序不影响结果。

## 验证

```powershell
.\scripts\verify-local.ps1
```

验证会运行后端 Maven 测试、前端 Vitest 测试和前端生产构建。
