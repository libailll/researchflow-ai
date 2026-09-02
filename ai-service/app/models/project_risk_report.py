from typing import Any

from pydantic import BaseModel, Field

from app.models.search import SemanticSearchResult


class ProjectRiskReportRequest(BaseModel):
    project_id: int = Field(alias="projectId", gt=0)
    user_id: int = Field(alias="userId", gt=0)
    risk_level: str = Field(alias="riskLevel")
    risk_score: int = Field(alias="riskScore", ge=0, le=100)
    project: dict[str, Any]
    indicators: dict[str, Any]
    tasks: list[dict[str, Any]] = Field(default_factory=list)
    members: list[dict[str, Any]] = Field(default_factory=list)
    documents: list[dict[str, Any]] = Field(default_factory=list)


class ProjectRiskReportResponse(BaseModel):
    content: str
    model: str
    sources: list[SemanticSearchResult] = Field(default_factory=list)
