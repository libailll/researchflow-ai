from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_host: str = "127.0.0.1"
    app_port: int = 8090
    java_api_base_url: str = "http://127.0.0.1:8080"
    ai_internal_token: str = "researchflow-local-ai-token"

    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-v4-flash"
    deepseek_api_key: str = ""
    deepseek_temperature: float = Field(default=0.5, ge=0, le=2)
    deepseek_max_tokens: int = Field(default=4096, ge=128, le=8192)
    agent_max_steps: int = Field(default=6, ge=1, le=10)

    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "guest"
    rabbitmq_password: str = "guest"
    rabbitmq_vhost: str = "/"
    rabbitmq_retry_exchange: str = "researchflow.document.retry"
    rabbitmq_dead_letter_exchange: str = "researchflow.document.dlx"
    rabbitmq_max_retries: int = Field(default=3, ge=0, le=3)
    document_parse_queue: str = "document.parse"
    document_vectorize_queue: str = "document.vectorize"
    document_vector_delete_queue: str = "document.vector.delete"

    qdrant_url: str = "http://127.0.0.1:6333"
    qdrant_collection: str = "researchflow_documents"
    embedding_model: str = "sentence-transformers/paraphrase-multilingual-mpnet-base-v2"
    embedding_cache_dir: str = "data/models"
    rag_top_k: int = Field(default=5, ge=1, le=12)
    rag_min_score: float = Field(default=0.25, ge=0, le=1)
    rag_max_context_chars: int = Field(default=12000, ge=2000, le=30000)

    chunk_size: int = Field(default=1000, ge=300, le=5000)
    chunk_overlap: int = Field(default=150, ge=0, le=1000)


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    if settings.chunk_overlap >= settings.chunk_size:
        raise ValueError("CHUNK_OVERLAP must be smaller than CHUNK_SIZE")
    return settings
