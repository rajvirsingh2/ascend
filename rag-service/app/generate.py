import hashlib
import json
import logging
from dataclasses import dataclass

from app.config import settings
from app.context_builder import build_user_context
from app.database import get_conn
from app.embedder import get_embedder
from app.quest_chain import PROMPT_VERSION, run_quest_chain
from app.retriever import retrieve_memories

logger = logging.getLogger(__name__)


@dataclass
class GenerateRequest:
    user_id: str
    generate_for: str = "daily"


async def generate_quests(req: GenerateRequest) -> dict:
    """
    Full RAG pipeline:
    1. Build user context from DB
    2. Embed query intent
    3. Retrieve diverse memories via MMR
    4. Run LangChain chain
    5. Log generation metadata
    6. Return quests
    """
    embedder = get_embedder()

    # step 1 — user context
    ctx = await build_user_context(req.user_id, req.generate_for)

    # step 2 — embed the query intent
    # we embed a summary of the user's goals + skills as the search query
    query_text = (
        f"quests for {req.generate_for} goals: "
        + ", ".join(g["title"] for g in ctx.active_goals[:3])
        + " skills: "
        + ", ".join(s["skill_name"] for s in ctx.skills[:3])
    )
    query_embedding = await embedder.embed_text(query_text)

    # step 3 — retrieve memories
    memories = await retrieve_memories(
        user_id=req.user_id,
        query_embedding=query_embedding,
        embedder=embedder,
    )

    # step 4 — run LangChain chain
    result = await run_quest_chain(ctx, memories)

    # step 5 — log generation
    context_hash = hashlib.sha256(
        (query_text + req.generate_for).encode()
    ).hexdigest()

    memory_ids = [m.id for m in memories]

    await _log_generation(
        user_id=req.user_id,
        context_hash=context_hash,
        memory_ids=memory_ids,
        raw_response=result,
    )

    return result


async def _log_generation(
    user_id: str,
    context_hash: str,
    memory_ids: list[int],
    raw_response: dict,
) -> None:
    async with get_conn() as conn:
        await conn.execute(
            """
            INSERT INTO quest_generation_log
                (user_id, context_hash, retrieved_memory_ids,
                 prompt_version, llm_model, raw_response)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (
                user_id,
                context_hash,
                memory_ids,
                PROMPT_VERSION,
                "gpt-4o",
                json.dumps(raw_response),
            ),
        )