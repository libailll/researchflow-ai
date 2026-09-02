from pydantic import BaseModel, Field


class SemanticSearchRequest(BaseModel):
    project_id: int = Field(alias="projectId", gt=0)
    query: str = Field(min_length=1, max_length=1000)
    top_k: int = Field(default=5, alias="topK", ge=1, le=20)


class SemanticSearchResult(BaseModel):
    document_id: int = Field(alias="documentId")
    document_name: str = Field(alias="documentName")
    page_number: int | None = Field(default=None, alias="pageNumber")
    chunk_index: int = Field(alias="chunkIndex")
    score: float
    content: str

    model_config = {"populate_by_name": True}
