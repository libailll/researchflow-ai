import json
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from app.core.config import get_settings
from app.core.security import verify_internal_token
from app.models.chat import ChatRequest, ChatResponse
from app.services.agent_service import AgentService

router = APIRouter(prefix="/ai", tags=["AI 对话"], dependencies=[Depends(verify_internal_token)])
logger = logging.getLogger(__name__)


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    try:
        return await AgentService(get_settings()).chat(request)
    except Exception as exc:
        logger.exception("RAG chat failed: project_id=%s", request.project_id)
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc


@router.post("/chat/stream")
async def chat_stream(request: ChatRequest) -> StreamingResponse:
    async def event_stream():
        try:
            async for event in AgentService(get_settings()).stream(request):
                yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"
        except Exception as exc:
            logger.exception("RAG streaming chat failed: project_id=%s", request.project_id)
            error = {"type": "error", "message": str(exc) or "AI 服务暂时不可用"}
            yield f"data: {json.dumps(error, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
