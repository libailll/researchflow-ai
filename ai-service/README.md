# ResearchFlow AI Service

当前阶段提供 FastAPI 健康检查、RabbitMQ 文档解析与向量化、RAG 检索、DeepSeek 流式对话以及 Agent Tool Calling。Agent 可以读取项目、成员、任务和文档数据，并为创建或修改任务生成待确认提案；写操作必须由前端用户确认后交给 Java Service 执行。

同时提供基于 DeepSeek OpenAI 兼容接口的普通对话、SSE 对话和只读 Agent Tool Calling。Agent 会按问题自主读取项目概况、任务、逾期任务、项目统计，或检索当前项目的 Qdrant 文档片段，再基于真实数据生成回答：

```text
POST /ai/chat
POST /ai/chat/stream
POST /ai/search
```

## Windows 启动

确保 Java 后端、RabbitMQ 和 Qdrant 已启动，然后在本目录执行：

首次执行 `setup.cmd` 安装依赖，以后双击 `start.cmd` 即可启动。也可以手动执行：

```cmd
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

启动成功后访问 `http://127.0.0.1:8090/health`，`documentWorker` 和 `vectorWorker` 显示 `CONNECTED` 表示两个消费者均已连接 RabbitMQ。FastAPI 文档位于 `http://127.0.0.1:8090/docs`。

## 配置

`.env` 中的 `AI_INTERNAL_TOKEN` 必须与 Java 后端的 `AI_INTERNAL_TOKEN` 环境变量一致。正式环境务必替换默认开发令牌。

DeepSeek 配置示例：

```env
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_API_KEY=sk-你的密钥
```

向量检索配置示例：

```env
QDRANT_URL=http://127.0.0.1:6333
QDRANT_COLLECTION=researchflow_documents
EMBEDDING_MODEL=sentence-transformers/paraphrase-multilingual-mpnet-base-v2
EMBEDDING_CACHE_DIR=data/models
RAG_TOP_K=5
RAG_MIN_SCORE=0.25
RAG_MAX_CONTEXT_CHARS=12000
AGENT_MAX_STEPS=6
```

`RAG_TOP_K` 控制候选片段数量，`RAG_MIN_SCORE` 控制最低相关度，`RAG_MAX_CONTEXT_CHARS` 限制送入模型的证据总长度。流式对话会先发送 `sources` 事件，再发送回答内容事件。

`AGENT_MAX_STEPS` 限制单次请求的最大工具调用轮数。当前 Agent 只开放读取类工具，不允许直接创建、修改或删除任务。

嵌入模型会在第一次向量化时自动下载并缓存，首次处理耗时会明显长于后续任务。Qdrant 控制台位于 `http://127.0.0.1:6333/dashboard`。

默认分块长度为 1000 字符、重叠 150 字符，可通过 `CHUNK_SIZE` 和 `CHUNK_OVERLAP` 调整。扫描版 PDF 暂不包含 OCR，会被标记为解析失败。
