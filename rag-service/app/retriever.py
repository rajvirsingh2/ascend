import logging
from dataclasses import dataclass

import numpy as np

from app.config import settings
from app.database import get_conn
from app.embedder import BaseEmbedder

logger = logging.getLogger(__name__)

CANDIDATE_K = 20   # fetch this many from pgvector
FINAL_K = 8        # return this many after MMR
MMR_LAMBDA = 0.6   # 0=max diversity, 1=max relevance


@dataclass
class RetrievedMemory:
    id: int
    doc_type: str
    content: str
    metadata: dict
    similarity: float


async def retrieve_memories(
    user_id: str,
    query_embedding: list[float],
    embedder: BaseEmbedder,
    days_back: int = 90,
) -> list[RetrievedMemory]:
    """
    1. Cosine similarity search → CANDIDATE_K results from pgvector
    2. MMR rerank → FINAL_K diverse results returned
    """
    vector_str = "[" + ",".join(str(v) for v in query_embedding) + "]"

    async with get_conn() as conn:
        cur = await conn.execute(
            """
            SELECT id, doc_type, content, metadata,
                   1 - (embedding <=> %s::vector) AS similarity
            FROM user_memories
            WHERE user_id = %s
              AND created_at > NOW() - INTERVAL '%s days'
            ORDER BY embedding <=> %s::vector
            LIMIT %s
            """,
            (vector_str, user_id, days_back, vector_str, CANDIDATE_K),
        )
        rows = await cur.fetchall()

    if not rows:
        return []

    candidates = [
        RetrievedMemory(
            id=r[0], doc_type=r[1], content=r[2],
            metadata=r[3], similarity=float(r[4])
        )
        for r in rows
    ]

    return _mmr_rerank(candidates, query_embedding)


def _mmr_rerank(
    candidates: list[RetrievedMemory],
    query_embedding: list[float],
) -> list[RetrievedMemory]:
    """
    Maximal Marginal Relevance — balances relevance to query with
    diversity among selected documents.
    """
    if len(candidates) <= FINAL_K:
        return candidates

    query_vec = np.array(query_embedding, dtype=np.float32)
    # precompute content embeddings — we only have similarity scores,
    # so we approximate using similarity as the relevance signal
    # and use doc_type + metadata as diversity proxy
    selected: list[RetrievedMemory] = []
    remaining = list(candidates)

    while len(selected) < FINAL_K and remaining:
        if not selected:
            # first pick: highest similarity
            best = max(remaining, key=lambda d: d.similarity)
        else:
            # MMR score = lambda * relevance - (1-lambda) * max_redundancy
            selected_types = [s.doc_type for s in selected]
            selected_skills = [
                s.metadata.get("skill_area", "") for s in selected
            ]

            def mmr_score(doc: RetrievedMemory) -> float:
                relevance = doc.similarity
                # redundancy: penalise same doc_type and skill_area
                type_penalty = sum(
                    0.2 for t in selected_types if t == doc.doc_type
                )
                skill_penalty = sum(
                    0.15 for sk in selected_skills
                    if sk == doc.metadata.get("skill_area", "")
                )
                redundancy = min(type_penalty + skill_penalty, 0.8)
                return MMR_LAMBDA * relevance - (1 - MMR_LAMBDA) * redundancy

            best = max(remaining, key=mmr_score)

        selected.append(best)
        remaining.remove(best)

    return selected


def format_memories_for_prompt(memories: list[RetrievedMemory]) -> str:
    """Groups memories by type for the LLM context block."""
    if not memories:
        return "No history available yet."

    completed = [m for m in memories if m.metadata.get("status") == "completed"]
    skipped = [m for m in memories if m.metadata.get("status") in ("skipped", "abandoned")]
    goals = [m for m in memories if m.doc_type == "goal"]
    habits = [m for m in memories if m.doc_type == "habit_pattern"]

    sections = []

    if goals:
        sections.append("ACTIVE GOALS:\n" + "\n".join(f"- {m.content}" for m in goals))

    if completed:
        sections.append(
            "RECENTLY COMPLETED (do not repeat these):\n"
            + "\n".join(f"- {m.content}" for m in completed)
        )

    if skipped:
        sections.append(
            "PREVIOUSLY SKIPPED (approach differently or avoid):\n"
            + "\n".join(f"- {m.content}" for m in skipped)
        )

    if habits:
        sections.append(
            "HABIT PATTERNS:\n"
            + "\n".join(f"- {m.content}" for m in habits)
        )

    return "\n\n".join(sections)