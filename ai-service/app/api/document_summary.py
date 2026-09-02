import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.config import get_settings
from app.core.security import verify_internal_token
from app.models.document_summary import DocumentSummaryRequest, DocumentSummaryResponse
from app.services.document_summary_service import DocumentSummaryGenerationService

router = APIRouter(
    prefix="/ai",
    tags=["AI 文档总结"],
    dependencies=[Depends(verify_internal_token)],
)
logger = logging.getLogger(__name__)


@router.post("/document-summary", response_model=DocumentSummaryResponse)
async def generate_document_summary(request: DocumentSummaryRequest) -> DocumentSummaryResponse:
    try:
        return await DocumentSummaryGenerationService(get_settings()).generate(request)
    except Exception as exc:
        logger.exception("Document summary generation failed: document_id=%s", request.document_id)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exc) or "AI 文档总结生成失败",
        ) from exc
