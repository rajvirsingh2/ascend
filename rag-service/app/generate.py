from __future__ import annotations
import hashlib
import json
import logging
from dataclasses import dataclass, field
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from app.providers.base import ProviderConfig

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
    provider_config: object = field(default=None)   # ProviderConfig | None


async def generate_quests(req: GenerateRequest) -> dict:
    embedder = get_embedder()

    ctx = await build_user_context(req.user_id, req.generate_for)

    query_text = (
        "quests for goals: "
        + ", ".join(g["title"] for g in ctx.active_goals[:3])
        + " skills: "
        + ", ".join(s["skill_name"] for s in ctx.skills[:3])
    ) if (ctx.active_goals or ctx.skills) else "general personal development quests"

    query_embedding = await embedder.embed_text(query_text)

    memories = await retrieve_memories(
        user_id=req.user_id,
        query_embedding=query_embedding,
        embedder=embedder,
    )

    # pass provider_config through to the chain
    result = await run_quest_chain(ctx, memories, req.provider_config)

    context_hash = hashlib.sha256(
        (query_text + req.generate_for).encode()
    ).hexdigest()

    await _log_generation(
        user_id=req.user_id,
        context_hash=context_hash,
        memory_ids=[m.id for m in memories],
        raw_response=result,
    )

    return result


async def _log_generation(
    user_id: str,
    context_hash: str,
    memory_ids: list,
    raw_response: dict,
) -> None:
    try:
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
                    memory_ids or [],
                    PROMPT_VERSION,
                    "multi-provider",
                    json.dumps(raw_response),
                ),
            )
    except Exception as e:
        logger.warning("failed to log generation: %s", e)