from typing import Any

from pydantic import BaseModel, Field

from app.models.search import SemanticSearchResult


class WeeklyReportRequest(BaseModel):
    project_id: int = Field(alias="projectId", gt=0)
    user_id: int = Field(alias="userId", gt=0)
    period_start: str = Field(alias="periodStart")
    period_end: str = Field(alias="periodEnd")
    project: dict[str, Any]
    statistics: dict[str, Any]
    tasks: list[dict[str, Any]] = Field(default_factory=list)
    activities: list[dict[str, Any]] = Field(default_factory=list)
    documents: list[dict[str, Any]] = Field(default_factory=list)


class WeeklyReportResponse(BaseModel):
    content: str
    model: str
    sources: list[SemanticSearchResult] = Field(default_factory=list)
