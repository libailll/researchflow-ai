import asyncio
import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.config import Settings, get_settings
from app.core.security import verify_internal_token
from app.models.search import SemanticSearchRequest, SemanticSearchResult
from app.services.vector_store import get_vector_store

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/ai", tags=["semantic-search"])


@router.post(
    "/search",
    response_model=list[SemanticSearchResult],
    dependencies=[Depends(verify_internal_token)],
)
async def semantic_search(
    request: SemanticSearchRequest,
    settings: Settings = Depends(get_settings),
) -> list[SemanticSearchResult]:
    try:
        store = get_vector_store(settings)
        return await asyncio.to_thread(store.search, request.project_id, request.query.strip(), request.top_k)
    except Exception as exc:
        logger.exception("Semantic search failed for project %s", request.project_id)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="向量检索服务暂时不可用",
        ) from exc
