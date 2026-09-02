import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.config import get_settings
from app.core.security import verify_internal_token
from app.models.weekly_report import WeeklyReportRequest, WeeklyReportResponse
from app.services.weekly_report_service import WeeklyReportGenerationService

router = APIRouter(
    prefix="/ai",
    tags=["AI 项目周报"],
    dependencies=[Depends(verify_internal_token)],
)
logger = logging.getLogger(__name__)


@router.post("/weekly-report", response_model=WeeklyReportResponse)
async def generate_weekly_report(request: WeeklyReportRequest) -> WeeklyReportResponse:
    try:
        return await WeeklyReportGenerationService(get_settings()).generate(request)
    except Exception as exc:
        logger.exception("Weekly report generation failed: project_id=%s", request.project_id)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exc) or "AI 周报生成失败",
        ) from exc
