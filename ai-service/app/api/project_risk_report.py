import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.config import get_settings
from app.core.security import verify_internal_token
from app.models.project_risk_report import ProjectRiskReportRequest, ProjectRiskReportResponse
from app.services.project_risk_report_service import ProjectRiskReportGenerationService

router = APIRouter(
    prefix="/ai",
    tags=["AI 项目风险分析"],
    dependencies=[Depends(verify_internal_token)],
)
logger = logging.getLogger(__name__)


@router.post("/project-risk-report", response_model=ProjectRiskReportResponse)
async def generate_project_risk_report(request: ProjectRiskReportRequest) -> ProjectRiskReportResponse:
    try:
        return await ProjectRiskReportGenerationService(get_settings()).generate(request)
    except Exception as exc:
        logger.exception("Project risk report generation failed: project_id=%s", request.project_id)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exc) or "AI 项目风险分析失败",
        ) from exc
