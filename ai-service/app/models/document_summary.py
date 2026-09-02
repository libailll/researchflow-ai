from pydantic import BaseModel, Field


class DocumentSummaryChunk(BaseModel):
    page_number: int | None = Field(default=None, alias="pageNumber")
    chunk_index: int = Field(alias="chunkIndex", ge=0)
    content: str = Field(min_length=1)


class DocumentSummaryRequest(BaseModel):
    document_id: int = Field(alias="documentId", gt=0)
    project_id: int = Field(alias="projectId", gt=0)
    user_id: int = Field(alias="userId", gt=0)
    document_name: str = Field(alias="documentName", min_length=1)
    file_type: str = Field(alias="fileType")
    chunks: list[DocumentSummaryChunk] = Field(min_length=1)


class DocumentSummarySource(BaseModel):
    page_number: int | None = Field(default=None, alias="pageNumber")
    chunk_index: int = Field(alias="chunkIndex")
    excerpt: str

    model_config = {"populate_by_name": True}


class DocumentSummaryResponse(BaseModel):
    content: str
    model: str
    sources: list[DocumentSummarySource] = Field(default_factory=list)
