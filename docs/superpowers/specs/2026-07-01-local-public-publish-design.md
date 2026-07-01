# 本地服务外网发布设计

**目标：** 每次更新并重启本地服务后，可以一键重新发布公网访问地址，方便手机或外部网络访问当前学习平台。

## 范围

- 使用本地前端 `http://127.0.0.1:5173` 作为公网入口。
- 前端继续通过 Vite `/api` 代理访问后端 `http://127.0.0.1:18080`。
- 发布脚本负责启动本地服务、启动 Cloudflare 临时隧道、记录公网地址。
- 停止脚本负责停止隧道和本地服务。
- 不在本次实现固定域名、HTTPS 证书、云服务器部署或生产级权限体系。

## 设计

新增 `scripts/publish-local.ps1`。脚本会先确保本地服务可用：如果 `.local/local-pids.txt` 不存在，则调用 `scripts/start-local.ps1`，默认可带 `-UseMysql`。随后解析 `cloudflared` 可执行文件：优先使用系统 PATH 中已有版本；如果没有，则下载官方 Windows amd64 最新版本到 `.local/tools/cloudflared.exe`。脚本通过 `cloudflared tunnel --url http://127.0.0.1:5173` 建立临时公网地址，并从日志中提取 `https://*.trycloudflare.com` 写入 `.local/public-url.txt`。

新增 `scripts/stop-publish.ps1`。脚本先停止公网隧道进程，再复用 `scripts/stop-local.ps1` 停止本地前后端服务。

为避免 Vite dev server 因公网 Host 头拒绝请求，`frontend/vite.config.ts` 的 dev server 增加 `allowedHosts: true`，但仍保持 `start-local.ps1` 使用 `--host 127.0.0.1`，只允许本机进程直接连接 dev server。

## 安全说明

公网 URL 持有者可以访问当前本地平台。该方案用于开发演示和个人临时访问，不建议长期公开。管理员账号密码应避免泄露。

## 验证

- 新增脚本结构测试，确认发布脚本、停止脚本、Vite Host 配置和 Cloudflare 下载地址存在。
- 执行 PowerShell 语法解析验证。
- 运行 `scripts/publish-local.ps1 -UseMysql`，确认生成 `.local/public-url.txt`。
- 用公网 URL 访问登录页，确认 HTTP 200 或页面可加载。
