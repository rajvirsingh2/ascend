import logging
from abc import ABC, abstractmethod

from app.config import settings
from app.database import get_conn
from app.document_builder import MemoryDocument

logger = logging.getLogger(__name__)


class BaseEmbedder(ABC):
    @abstractmethod
    async def embed_text(self, text: str) -> list[float]:
        pass


class OpenAIEmbedder(BaseEmbedder):
    def __init__(self):
        from openai import AsyncOpenAI
        self._client = AsyncOpenAI(api_key=settings.openai_api_key)

    async def embed_text(self, text: str) -> list[float]:
        response = await self._client.embeddings.create(
            model=settings.embedding_model,
            input=text,
        )
        return response.data[0].embedding


class MockEmbedder(BaseEmbedder):
    """Used in tests and when OPENAI_API_KEY is not set."""
    async def embed_text(self, text: str) -> list[float]:
        # deterministic fake vector — same text always produces same vector
        seed = sum(ord(c) for c in text[:100])
        base = (seed % 100) / 100.0
        return [base] * settings.embedding_dimensions


def get_embedder() -> BaseEmbedder:
    if settings.openai_api_key and settings.app_env != "test":
        return OpenAIEmbedder()
    logger.warning("OPENAI_API_KEY not set — using mock embedder")
    return MockEmbedder()


async def store_memory(doc: MemoryDocument, embedding: list[float]) -> int:
    """
    Inserts a document + its embedding into user_memories.
    Returns the new row id.
    """
    import json

    vector_str = "[" + ",".join(str(v) for v in embedding) + "]"

    async with get_conn() as conn:
        cur = await conn.execute(
            """
            INSERT INTO user_memories
                (user_id, doc_type, entity_id, content, embedding, metadata, model_version)
            VALUES
                (%s, %s, %s, %s, %s::vector, %s, %s)
            ON CONFLICT DO NOTHING
            RETURNING id
            """,
            (
                doc.user_id,
                doc.doc_type,
                doc.entity_id,
                doc.content,
                vector_str,
                json.dumps(doc.metadata),
                settings.model_version,
            ),
        )
        row = await cur.fetchone()
    return row[0] if row else -1