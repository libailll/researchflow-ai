# ResearchFlow AI Docker 启动说明

根目录的 `docker-compose.yml` 会统一启动：前端、Java 后端、Python AI Service、MySQL、Redis、RabbitMQ 和 Qdrant。

## 首次启动

1. 默认将容器 MySQL 映射到宿主机 `3307`，避免与常见的本机 MySQL `3306` 冲突。若 `3000`、`8080`、`8090`、`3307`、`6379`、`5672`、`15672`、`6333` 或 `6334` 已被占用，可在根目录 `.env` 中修改对应端口。
2. 将根目录 `.env.example` 复制为 `.env`，替换其中带 `change-me` 的密码和令牌。
3. DeepSeek 配置继续使用 `ai-service/.env`。确认其中已经设置 `DEEPSEEK_API_KEY`，不要提交该文件。
4. 在 `C:\ResearchFlow AI` 执行：

```powershell
docker compose up -d --build
```

也可以直接双击根目录的 `start-docker.cmd`。脚本会自动识别系统 PATH，以及当前电脑的 `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe` 用户级安装路径；如果 Docker Desktop 尚未运行，会自动启动并等待引擎就绪，服务启动后自动打开前端。失败时会保留窗口并显示原因，不会再出现“闪一下就没了”。`stop-docker.cmd` 会停止服务并保留数据，`logs-docker.cmd` 用于持续查看日志。

首次构建会下载 Java、Node、Python 依赖以及基础镜像；AI 首次向量化还会下载 Embedding 模型，因此耗时会明显长一些。

## 访问地址

- 前端：`http://localhost:3000`
- Java OpenAPI：`http://localhost:8080/swagger-ui.html`
- Java 健康检查：`http://localhost:8080/api/health`
- AI 健康检查：`http://localhost:8090/health`
- RabbitMQ 管理台：`http://localhost:15672`
- Qdrant 控制台：`http://localhost:6333/dashboard`

## 常用维护命令

```powershell
# 查看服务状态
docker compose ps

# 查看全部日志
docker compose logs -f

# 只查看后端或 AI 日志
docker compose logs -f backend
docker compose logs -f ai-service

# 停止服务但保留数据库、文档和模型
docker compose down

# 重新构建有代码修改的服务
docker compose up -d --build backend ai-service frontend
```

不要随意执行 `docker compose down -v`：`-v` 会删除 MySQL、Redis、RabbitMQ、Qdrant、上传文档和 Embedding 模型等持久化卷。

## 数据初始化与现有数据

数据库结构由 Java 后端启动时通过 Flyway 自动维护，迁移脚本位于 `researchflow/src/main/resources/db/migration`。全新数据卷会执行全部迁移；已有数据库会建立版本基线并执行缺失迁移，因此新增表或字段后无需删除数据卷，也不需要手工运行 SQL。

已经执行过的迁移文件不能再修改。后续结构变更必须新增更高版本的 `V<n>__description.sql`，并正常重启后端。

Java 与 AI Service 将 `document-data` 同时挂载到 `/data/documents`，因此 Java 上传后的文件可以被 Python 文档解析 Worker 直接读取。所有数据库、中间件、文档和模型均使用命名卷持久化，普通的 `docker compose down` 不会删除数据。
