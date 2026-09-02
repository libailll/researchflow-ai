import asyncio
from dataclasses import dataclass, field
from typing import Any

import httpx

from app.core.config import Settings
from app.models.chat import ChatRequest
from app.models.search import SemanticSearchResult
from app.services.vector_store import get_vector_store


TOOL_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "get_project",
            "description": "读取当前项目的名称、描述、状态、进度和起止时间。",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_tasks",
            "description": "列出当前项目全部任务及状态、优先级、进度、负责人和截止日期。",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_overdue_tasks",
            "description": "列出当前项目已经超过截止日期且尚未完成或取消的任务。",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_project_statistics",
            "description": "读取当前项目任务总数、完成数、进行中数量、逾期数和完成率。",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_members",
            "description": "列出当前项目成员的用户ID、用户名、昵称和项目角色。按姓名指定任务负责人前必须先使用此工具解析用户ID。",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_documents",
            "description": "在当前项目知识库中进行语义检索。回答论文、资料、方案或研究内容问题时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "用于检索的完整问题或关键词"},
                    "topK": {"type": "integer", "minimum": 1, "maximum": 10, "default": 5},
                },
                "required": ["query"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "create_task",
            "description": "生成创建项目任务的待确认操作。该工具不会直接写入数据，必须由用户在界面中确认。",
            "parameters": {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "任务标题"},
                    "description": {"type": "string", "description": "任务说明"},
                    "assigneeId": {"type": "integer", "description": "负责人用户ID"},
                    "priority": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"]},
                    "startDate": {"type": "string", "description": "开始日期，YYYY-MM-DD"},
                    "dueDate": {"type": "string", "description": "截止日期，YYYY-MM-DD"},
                },
                "required": ["title", "priority"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "update_task",
            "description": "生成修改任务的待确认操作。只提供需要修改的字段；该工具不会直接写入数据，必须由用户在界面中确认。",
            "parameters": {
                "type": "object",
                "properties": {
                    "taskId": {"type": "integer", "description": "任务ID"},
                    "title": {"type": "string"},
                    "description": {"type": "string"},
                    "assigneeId": {"type": "integer"},
                    "priority": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"]},
                    "progress": {"type": "integer", "minimum": 0, "maximum": 100},
                    "startDate": {"type": "string", "description": "YYYY-MM-DD"},
                    "dueDate": {"type": "string", "description": "YYYY-MM-DD"},
                },
                "required": ["taskId"],
                "additionalProperties": False,
            },
        },
    },
]


TOOL_LABELS = {
    "get_project": "读取项目概况",
    "list_tasks": "读取项目任务",
    "get_overdue_tasks": "检查逾期任务",
    "get_project_statistics": "统计项目进度",
    "list_members": "读取项目成员",
    "search_documents": "检索项目文档",
    "create_task": "准备创建任务",
    "update_task": "准备修改任务",
}


@dataclass
class ToolExecution:
    result: Any
    summary: str
    sources: list[SemanticSearchResult] = field(default_factory=list)
    proposal: dict[str, Any] | None = None


class AgentToolExecutor:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    async def execute(self, name: str, arguments: dict[str, Any], request: ChatRequest) -> ToolExecution:
        if name in {"create_task", "update_task"}:
            return self._propose_action(name, arguments)
        if name == "search_documents":
            return await self._search_documents(arguments, request)

        paths = {
            "get_project": "project",
            "list_tasks": "tasks",
            "get_overdue_tasks": "tasks/overdue",
            "get_project_statistics": "statistics",
            "list_members": "members",
        }
        path = paths.get(name)
        if path is None:
            raise ValueError(f"不支持的工具：{name}")
        result = await self._java_get(
            f"/internal/ai/projects/{request.project_id}/tools/{path}",
            {"userId": request.user_id},
        )
        count = len(result) if isinstance(result, list) else None
        summary = f"已返回 {count} 条记录" if count is not None else "已获取数据"
        return ToolExecution(result=result, summary=summary)

    def _propose_action(self, name: str, arguments: dict[str, Any]) -> ToolExecution:
        action_type = "CREATE_TASK" if name == "create_task" else "UPDATE_TASK"
        title = str(arguments.get("title") or "").strip()
        task_id = arguments.get("taskId")
        if name == "create_task" and not title:
            raise ValueError("创建任务必须提供标题")
        if name == "update_task" and not task_id:
            raise ValueError("修改任务必须提供任务ID")
        label = "创建任务" if name == "create_task" else "修改任务"
        subject = title or f"任务 #{task_id}"
        proposal = {
            "actionType": action_type,
            "label": label,
            "description": subject,
            "payload": arguments,
        }
        return ToolExecution(
            result={"status": "AWAITING_CONFIRMATION", "proposal": proposal},
            summary="已生成待确认操作，尚未写入数据",
            proposal=proposal,
        )

    async def _search_documents(self, arguments: dict[str, Any], request: ChatRequest) -> ToolExecution:
        query = str(arguments.get("query") or request.message).strip()
        top_k = min(max(int(arguments.get("topK") or self.settings.rag_top_k), 1), 10)
        results = await asyncio.to_thread(
            get_vector_store(self.settings).search,
            request.project_id,
            query,
            top_k,
        )
        selected = [item for item in results if item.score >= self.settings.rag_min_score]
        limited: list[SemanticSearchResult] = []
        used_chars = 0
        for item in selected:
            remaining = self.settings.rag_max_context_chars - used_chars
            if remaining <= 0:
                break
            if len(item.content) > remaining:
                item = item.model_copy(update={"content": item.content[:remaining]})
            limited.append(item)
            used_chars += len(item.content)
        return ToolExecution(
            result=[item.model_dump(by_alias=True) for item in limited],
            summary=f"检索到 {len(limited)} 个相关片段",
            sources=limited,
        )

    async def _java_get(self, path: str, params: dict[str, Any]) -> Any:
        async with httpx.AsyncClient(
            base_url=self.settings.java_api_base_url.rstrip("/"),
            headers={"X-Internal-Token": self.settings.ai_internal_token},
            timeout=30.0,
        ) as client:
            response = await client.get(path, params=params)
        response.raise_for_status()
        payload = response.json()
        if payload.get("code") != 200:
            raise RuntimeError(payload.get("message") or "Java 服务处理失败")
        return payload.get("data")
