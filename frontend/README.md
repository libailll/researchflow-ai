# ResearchFlow AI 前端

基于 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router 与 Axios 的科研项目协作平台前端。

## 本地启动

先确保后端运行在 `http://localhost:8080`，然后执行：

```bash
npm install
npm run dev
```

访问 `http://localhost:3000`。开发服务器会把 `/api` 请求代理到本机后端。

## 常用命令

```bash
npm run dev      # 启动开发环境
npm run build    # 类型检查并生成生产构建
npm test         # 运行单元测试
npm run preview  # 预览生产构建
```

## 目录结构

```text
src/
├─ api/          # Axios 实例、拦截器和后端接口模块
├─ components/   # 可复用业务组件与通用组件
├─ layouts/      # 登录布局和工作台布局
├─ router/       # 路由与登录守卫
├─ stores/       # Pinia 用户及工作空间状态
├─ styles/       # 全局主题和 Element Plus 样式覆盖
├─ types/        # 接口响应与业务模型类型
├─ utils/        # 状态常量、日期及显示工具
└─ views/        # 登录、仪表盘、项目、任务、成员、文档和个人资料页面
```

## 环境变量

复制 `.env.example` 为 `.env.local` 后可以修改后端接口前缀：

```env
VITE_API_BASE_URL=/api
```

## 文档模块依赖

上传文档前需要启动 MySQL、Redis、RabbitMQ 和 Qdrant。RabbitMQ 默认连接
`localhost:5672`，账号密码均为 `guest`；可通过后端环境变量
`RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
覆盖。

文档解析由根目录下的 `ai-service` 提供。首次运行其 `setup.cmd`，之后运行
`start.cmd`；文档页面会每 5 秒自动刷新解析或向量化中的文档状态。已向量化的
文档可以在文档页使用自然语言进行项目内语义检索，失败任务支持一键重新提交。

“AI 助手”页面通过 Java 后端连接 AI Service，支持 DeepSeek SSE 流式 RAG 回答、
参考文档和页码展示、停止生成与失败重试。会话、消息、思考过程和引用来源由
Java 后端写入 MySQL；支持历史恢复、重命名、清空与删除，并按项目和当前用户隔离。
