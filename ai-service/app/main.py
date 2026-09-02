from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.health import router as health_router
from app.api.chat import router as chat_router
from app.api.search import router as search_router
from app.api.weekly_report import router as weekly_report_router
from app.api.document_summary import router as document_summary_router
from app.api.project_risk_report import router as project_risk_report_router
from app.core.config import get_settings
from app.core.logging import configure_logging
from app.worker.document_consumer import DocumentConsumer
from app.worker.vector_consumer import VectorConsumer

configure_logging()


@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer = DocumentConsumer(get_settings())
    vector_consumer = VectorConsumer(get_settings())
    app.state.document_consumer = consumer
    app.state.vector_consumer = vector_consumer
    consumer.start()
    vector_consumer.start()
    yield
    consumer.stop()
    vector_consumer.stop()


app = FastAPI(
    title="ResearchFlow AI Service",
    version="0.1.0",
    description="文档解析、RAG 与 Agent 能力服务",
    lifespan=lifespan,
)
app.include_router(health_router)
app.include_router(chat_router)
app.include_router(search_router)
app.include_router(weekly_report_router)
app.include_router(document_summary_router)
app.include_router(project_risk_report_router)
