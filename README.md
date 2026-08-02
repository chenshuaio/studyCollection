# StudyCollection

Java 学习题库平台，本地开发版支持账号登录、公共/个人题库管理、结构化题目导入、Java 学习知识内容生成题库、练习答题、持久化考试、错题反馈和学习分析的基础闭环。

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

默认启动会使用内存仓储，方便没有数据库时快速体验；使用 `-UseMysql` 启动会启用 `local-mysql` profile，账号、题库、待审核题、题目反馈、练习统计、错题和考试会话会保存到本地 MySQL，重启后仍可继续使用。管理员删除题目采用软删除，既有反馈、修订记录和历史试卷快照不会因删除题目而丢失。

## Java 知识内容生成题库

进入前端 `/import` 页面，在“结构化题目导入”区域上传 `.json`、`.csv`、`.xlsx`、`.txt` 或 `.md` 题目文件。系统会按字段批量解析题目；题干与选项、题型、难度、知识点、答案和解析都可以在页面中编辑、增删，确认后再提交管理员审核。示例文件见 `examples/questions-import.json`、`examples/questions-import.csv` 和 `examples/questions-import.xlsx`。

进入前端 `/import` 页面，在“学习内容生成题库”区域粘贴 Java 学习材料，例如 HashMap、局部变量、面向对象等知识内容，点击“分析生成题库”即可调用后端规则生成题库草稿。

也可以点击“上传学习资料”选择 `.txt`、`.md`、`.csv`、`.xlsx`、`.docx` 或 `.pdf` 文件，单个文件最大 10 MB。PDF 必须包含可复制的文本层，扫描版或纯图片 PDF 暂不支持。上传成功后会展示后端生成的可编辑题库草稿，确认后再提交管理员审核。

结构化题目和学习资料生成题共用“个人题库 / 申请公开”目标选择，默认提交到个人题库。两种目标都必须先由管理员审核：个人题审核通过后仅提交者本人可见和使用，公开申请审核通过后所有用户可用。普通用户可在题库、练习和自定义组卷中选择“全部可用、公共题库、我的题库”；管理员模拟考试固定只从公共题库抽题。

## 在线 AI 与规则回退

学习报告支持“规则分析”和“在线模型”两种模式。在线模式使用 OpenAI 兼容 Chat Completions 接口；远端不可用、超时、响应格式错误或配置不完整时，报告仍会保存，并自动回退到规则分析。

API 密钥只通过启动环境提供，不会保存到数据库或返回浏览器：

```powershell
$env:STUDY_COLLECTION_AI_API_KEY='your-api-key'
.\scripts\start-local.ps1 -UseMysql
```

管理员登录后进入 `/ai-settings`，可维护完整接口地址和模型名称、测试连接并查看最近调用审计。也可以在启动前提供默认值：

```powershell
$env:STUDY_COLLECTION_AI_ENDPOINT='https://api.example.com/v1/chat/completions'
$env:STUDY_COLLECTION_AI_MODEL='model-name'
```

调用审计只记录用户、用途、模型、成功/回退状态、耗时和脱敏失败原因，不保存提示词、模型输出、Authorization 请求头或 API 密钥。

## 登录与接口权限

除注册和登录外，后端接口都要求登录令牌。前端会自动发送 `Authorization: Bearer <token>`；管理员题库、审核、知识点和用户管理接口还会在后端校验管理员角色。令牌默认有效期为 12 小时，重启本地后端后需要重新登录。

新注册密码使用 PBKDF2 加盐哈希保存。现有 `{plain}` 本地演示账号会在首次成功登录后自动升级，不影响 `admin/admin123` 和 `user/user123` 的使用。

单选题、判断题和多选题会显示未预选的选项控件；历史题目缺少选项文本时会提供 `A-D` 占位选项。多选答案按选项集合判分，选择顺序不影响结果。

## 自定义考试

进入 `/exams` 勾选题目并设置试卷名称和时长。生成后的考试保存在服务端，答题页会逐题自动保存并显示真实倒计时；刷新页面、重新进入或重新登录后可以继续答题。考试到时会按截止前已保存答案自动提交，完成后可从考试记录查看成绩和逐题解析。

提交前，普通用户不会收到题目标准答案和解析。简答题、编程题只保存作答并展示参考答案，不自动评分，也不会因文字与参考答案不同而自动进入错题本。

## 验证

```powershell
.\scripts\verify-local.ps1
```

验证脚本会依次执行 Maven 测试、Vue TypeScript 类型检查、Vitest 和 Vite 生产构建。

验证会运行后端 Maven 测试、前端 Vitest 测试和前端生产构建。
