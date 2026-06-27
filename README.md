# StudyCollection

Java 学习题库平台，本地开发版支持账号登录、题库管理、题目导入预览、练习答题、错题反馈和学习分析的基础闭环。

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

## 验证

```powershell
.\scripts\verify-local.ps1
```

验证会运行后端 Maven 测试、前端 Vitest 测试和前端生产构建。
