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

默认启动会使用内存仓储，方便没有数据库时快速体验；使用 `-UseMysql` 启动会启用 `local-mysql` profile，账号和题库保存会连接本地 MySQL。

## Java 知识内容生成题库

进入前端 `/import` 页面，在“学习内容生成题库”区域粘贴 Java 学习材料，例如 HashMap、局部变量、面向对象等知识内容，点击“分析生成题库”即可调用后端规则生成题库草稿。

也可以点击“上传学习资料”选择 `.txt`、`.md`、`.csv`、`.docx` 或 `.pdf` 文件，前端会通过后端上传接口分析文件内容并生成题库草稿。确认无误后点击“确认生成题入库”保存到题库。

## 验证

```powershell
.\scripts\verify-local.ps1
```

验证会运行后端 Maven 测试、前端 Vitest 测试和前端生产构建。
