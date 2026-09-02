from fastapi import APIRouter, Request

router = APIRouter(tags=["系统状态"])


@router.get("/health")
def health(request: Request) -> dict:
    consumer = request.app.state.document_consumer
    vector_consumer = request.app.state.vector_consumer
    return {
        "status": "UP",
        "documentWorker": "CONNECTED" if consumer.connected else "CONNECTING",
        "vectorWorker": "CONNECTED" if vector_consumer.connected else "CONNECTING",
    }
