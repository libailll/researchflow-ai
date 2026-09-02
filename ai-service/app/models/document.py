from pathlib import Path

from pydantic import BaseModel, Field


class DocumentParseMessage(BaseModel):
    document_id: int = Field(alias="documentId", gt=0)
    project_id: int = Field(alias="projectId", gt=0)
    file_path: Path = Field(alias="filePath")


class ParsedChunk(BaseModel):
    page_number: int | None = Field(default=None, alias="pageNumber", ge=0)
    chunk_index: int = Field(alias="chunkIndex", ge=0)
    content: str = Field(min_length=1, max_length=10000)

    model_config = {"populate_by_name": True}


class DocumentVectorMessage(BaseModel):
    document_id: int = Field(alias="documentId", gt=0)
    project_id: int = Field(alias="projectId", gt=0)


class VectorChunk(BaseModel):
    id: int = Field(gt=0)
    page_number: int | None = Field(default=None, alias="pageNumber", ge=0)
    chunk_index: int = Field(alias="chunkIndex", ge=0)
    content: str = Field(min_length=1)
    char_count: int = Field(alias="charCount", ge=0)


class DocumentVectorData(BaseModel):
    document_id: int = Field(alias="documentId", gt=0)
    project_id: int = Field(alias="projectId", gt=0)
    document_name: str = Field(alias="documentName", min_length=1)
    chunks: list[VectorChunk]
