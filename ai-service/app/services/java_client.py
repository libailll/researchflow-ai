import httpx

from app.models.document import DocumentVectorData, ParsedChunk


class JavaDocumentClient:
    def __init__(self, base_url: str, internal_token: str) -> None:
        self._client = httpx.Client(
            base_url=base_url.rstrip("/"),
            headers={"X-Internal-Token": internal_token},
            timeout=60.0,
        )

    def close(self) -> None:
        self._client.close()

    def mark_processing(self, document_id: int) -> None:
        self._post(f"/internal/ai/documents/{document_id}/processing")

    def save_parsed(self, document_id: int, chunks: list[ParsedChunk]) -> None:
        self._post(
            f"/internal/ai/documents/{document_id}/parsed",
            json={"chunks": [chunk.model_dump(by_alias=True) for chunk in chunks]},
        )

    def mark_failed(self, document_id: int, error: str) -> None:
        self._post(f"/internal/ai/documents/{document_id}/failed", json={"error": error[:2000]})

    def get_vector_data(self, document_id: int) -> DocumentVectorData:
        payload = self._get(f"/internal/ai/documents/{document_id}/vector-data")
        return DocumentVectorData.model_validate(payload)

    def mark_vector_processing(self, document_id: int) -> None:
        self._post(f"/internal/ai/documents/{document_id}/vector/processing")

    def mark_vector_success(self, document_id: int) -> None:
        self._post(f"/internal/ai/documents/{document_id}/vector/success")

    def mark_vector_failed(self, document_id: int, error: str) -> None:
        self._post(f"/internal/ai/documents/{document_id}/vector/failed", json={"error": error[:2000]})

    def _get(self, path: str):
        response = self._client.get(path)
        response.raise_for_status()
        payload = response.json()
        if payload.get("code") != 200:
            raise RuntimeError(payload.get("message") or "Java 服务处理失败")
        return payload.get("data")

    def _post(self, path: str, json: dict | None = None) -> None:
        response = self._client.post(path, json=json)
        response.raise_for_status()
        payload = response.json()
        if payload.get("code") != 200:
            raise RuntimeError(payload.get("message") or "Java 服务处理失败")
