import threading
import uuid
import warnings

from fastembed import TextEmbedding
from qdrant_client import QdrantClient, models

from app.core.config import Settings
from app.models.document import DocumentVectorData
from app.models.search import SemanticSearchResult


class VectorStore:
    def __init__(self, settings: Settings) -> None:
        self._client = QdrantClient(url=settings.qdrant_url, timeout=60)
        self._collection = settings.qdrant_collection
        with warnings.catch_warnings():
            warnings.filterwarnings("ignore", message=r"The model .* now uses mean pooling.*", category=UserWarning)
            self._embedding = TextEmbedding(
                model_name=settings.embedding_model,
                cache_dir=settings.embedding_cache_dir,
            )
        self._lock = threading.Lock()

    def index_document(self, document: DocumentVectorData) -> None:
        texts = [chunk.content for chunk in document.chunks]
        if not texts:
            raise ValueError("文档没有可向量化的文本分块")

        with self._lock:
            vectors = [vector.tolist() for vector in self._embedding.embed(texts, batch_size=32)]
        self._ensure_collection(len(vectors[0]))
        self.delete_document(document.document_id)

        points = []
        for chunk, vector in zip(document.chunks, vectors, strict=True):
            point_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"researchflow:chunk:{chunk.id}"))
            points.append(
                models.PointStruct(
                    id=point_id,
                    vector=vector,
                    payload={
                        "project_id": document.project_id,
                        "document_id": document.document_id,
                        "document_name": document.document_name,
                        "page_number": chunk.page_number,
                        "chunk_index": chunk.chunk_index,
                        "content": chunk.content,
                    },
                )
            )
        self._client.upsert(collection_name=self._collection, points=points, wait=True)

    def delete_document(self, document_id: int) -> None:
        if not self._client.collection_exists(self._collection):
            return
        self._client.delete(
            collection_name=self._collection,
            points_selector=models.FilterSelector(
                filter=models.Filter(
                    must=[models.FieldCondition(key="document_id", match=models.MatchValue(value=document_id))]
                )
            ),
            wait=True,
        )

    def search(self, project_id: int, query: str, top_k: int) -> list[SemanticSearchResult]:
        if not self._client.collection_exists(self._collection):
            return []
        with self._lock:
            query_vector = next(iter(self._embedding.query_embed(query))).tolist()
        response = self._client.query_points(
            collection_name=self._collection,
            query=query_vector,
            query_filter=models.Filter(
                must=[models.FieldCondition(key="project_id", match=models.MatchValue(value=project_id))]
            ),
            limit=top_k,
            with_payload=True,
        )
        results = []
        for point in response.points:
            payload = point.payload or {}
            results.append(
                SemanticSearchResult(
                    documentId=payload.get("document_id"),
                    documentName=payload.get("document_name", "未知文档"),
                    pageNumber=payload.get("page_number"),
                    chunkIndex=payload.get("chunk_index", 0),
                    score=float(point.score),
                    content=payload.get("content", ""),
                )
            )
        return results

    def _ensure_collection(self, vector_size: int) -> None:
        if self._client.collection_exists(self._collection):
            collection = self._client.get_collection(self._collection)
            configured_size = collection.config.params.vectors.size
            if configured_size != vector_size:
                raise RuntimeError(
                    f"Qdrant 集合向量维度为 {configured_size}，当前模型维度为 {vector_size}；请清理旧集合后重试"
                )
            return
        self._client.create_collection(
            collection_name=self._collection,
            vectors_config=models.VectorParams(size=vector_size, distance=models.Distance.COSINE),
        )
        self._client.create_payload_index(
            collection_name=self._collection,
            field_name="project_id",
            field_schema=models.PayloadSchemaType.INTEGER,
            wait=True,
        )
        self._client.create_payload_index(
            collection_name=self._collection,
            field_name="document_id",
            field_schema=models.PayloadSchemaType.INTEGER,
            wait=True,
        )


_instance: VectorStore | None = None
_instance_lock = threading.Lock()


def get_vector_store(settings: Settings) -> VectorStore:
    global _instance
    if _instance is None:
        with _instance_lock:
            if _instance is None:
                _instance = VectorStore(settings)
    return _instance
