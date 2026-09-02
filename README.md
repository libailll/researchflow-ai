# ResearchFlow AI

ResearchFlow AI 是面向科研团队的协作与知识检索平台，将项目、任务、成员、文档与 AI 助手整合在同一个工作空间中。

## 核心能力

- 项目、成员、任务和进度管理
- JWT + Redis 身份认证与项目级 RBAC 权限控制
- PDF、Word、TXT、Markdown 文档上传与异步解析
- 基于 Embedding、Qdrant 和 DeepSeek 的 RAG 检索问答
- 支持多轮会话、流式输出以及文档和页码级引用
- 基于 Tool Calling 的项目管理 Agent
- 周报、文档摘要和项目风险报告生成与导出
- 任务状态变更、逾期提醒等站内通知

## 技术栈

| 层级 | 技术 |
| --- | --- |
| Web 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus |
| Java 后端 | Java 17、Spring Boot、MyBatis-Plus、JWT |
| AI 服务 | Python、FastAPI、DeepSeek、Embedding |
| 数据与中间件 | MySQL、Redis、RabbitMQ、Qdrant |
| 部署 | Docker、Docker Compose、Nginx |

## 系统组成

```text
Vue 3 Frontend
       |
Spring Boot Backend
   |       |       \
 MySQL   Redis   RabbitMQ
                     |
               FastAPI AI Service
                  |         |
               Qdrant    DeepSeek
```

Java 服务负责业务、认证、权限和数据持久化；Python AI 服务负责文档解析、向量化、检索、模型调用和 Agent 推理。RabbitMQ 用于解耦耗时的文档处理任务。

## Docker 一键启动

### 1. 准备配置

```cmd
copy .env.example .env
```

将根目录 `.env` 中带有 `change-me` 的配置替换为本地安全值，并在 `ai-service/.env` 中配置：

```env
DEEPSEEK_API_KEY=your-deepseek-api-key
```

真实 `.env` 文件已被 Git 忽略，请勿提交密钥。

### 2. 启动系统

Windows 可以直接双击 `start-docker.cmd`，也可以执行：

```bash
docker compose up -d --build
```

### 3. 访问服务

- 前端：<http://localhost:3000>
- Swagger：<http://localhost:8080/swagger-ui.html>
- Java 健康检查：<http://localhost:8080/api/health>
- AI 健康检查：<http://localhost:8090/health>
- RabbitMQ 管理台：<http://localhost:15672>
- Qdrant 控制台：<http://localhost:6333/dashboard>

### 4. 停止系统

双击 `stop-docker.cmd`，或者执行：

```bash
docker compose down
```

不要随意使用 `docker compose down -v`，该命令会删除数据库、向量库、上传文档和模型缓存等持久化数据。

## 项目结构

```text
researchflow-ai/
├─ frontend/       Vue 3 前端
├─ researchflow/   Spring Boot 后端
├─ ai-service/     FastAPI AI 服务
├─ docs/           项目文档
├─ docker-compose.yml
├─ start-docker.cmd
└─ stop-docker.cmd
```

## 配置与安全

- 仓库仅保留 `.env.example` 配置模板。
- DeepSeek API Key、JWT 密钥及数据库密码不得提交到 Git。
- 对外部署前需要替换所有示例密码并限制数据库和中间件管理端口。

## License

本项目当前用于个人学习、作品展示与技术交流，未经授权请勿用于商业用途。
