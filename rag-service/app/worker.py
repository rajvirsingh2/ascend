import asyncio
import json
import logging

import redis.asyncio as aioredis

from app.config import settings
from app.document_builder import (
    build_goal_document,
    build_habit_document,
    build_quest_document,
)
from app.embedder import get_embedder, store_memory

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
RETRY_BASE_SECONDS = 2


async def process_job(job: dict, embedder) -> None:
    """
    Routes a job to the correct document builder then embeds and stores it.

    Expected job shape published by Go:
    {
        "event_type": "quest_completed" | "quest_skipped" | "goal_created" | "habit_milestone",
        "user_id": "uuid",
        "payload": { ...entity fields... }
    }
    """
    event_type = job.get("event_type", "")
    user_id = job.get("user_id", "")
    payload = job.get("payload", {})

    if not user_id or not payload:
        logger.warning("malformed job — missing user_id or payload: %s", job)
        return

    if event_type in ("quest_completed", "quest_skipped"):
        doc = build_quest_document(payload, user_id)
    elif event_type == "goal_created":
        doc = build_goal_document(payload, user_id)
    elif event_type == "habit_milestone":
        doc = build_habit_document(payload, user_id)
    else:
        logger.warning("unknown event_type: %s", event_type)
        return

    embedding = await embedder.embed_text(doc.content)
    memory_id = await store_memory(doc, embedding)

    logger.info(
        "stored memory id=%s type=%s user=%s",
        memory_id, doc.doc_type, user_id
    )


async def run_worker() -> None:
    """
    Long-running coroutine — blocks on Redis BLPOP waiting for ingestion jobs.
    Runs for the lifetime of the application via asyncio.create_task in lifespan.
    """
    embedder = get_embedder()
    rdb = aioredis.from_url(settings.redis_url, decode_responses=True)

    logger.info(
        "ingestion worker listening on queue '%s'",
        settings.ingestion_queue_key
    )

    try:
        while True:
            try:
                # BLPOP blocks up to worker_block_seconds then returns None
                # This allows the loop to be cancelled cleanly
                result = await rdb.blpop(
                    settings.ingestion_queue_key,
                    timeout=settings.worker_block_seconds,
                )

                if result is None:
                    continue  # timeout — loop and block again

                _, raw = result
                job = json.loads(raw)
                logger.info("received job: %s", job.get("event_type"))

                # retry loop with exponential backoff
                for attempt in range(1, MAX_RETRIES + 1):
                    try:
                        await process_job(job, embedder)
                        break
                    except Exception as e:
                        if attempt == MAX_RETRIES:
                            logger.error(
                                "job failed after %d attempts: %s — %s",
                                MAX_RETRIES, job.get("event_type"), e
                            )
                        else:
                            wait = RETRY_BASE_SECONDS ** attempt
                            logger.warning(
                                "attempt %d failed, retrying in %ds: %s",
                                attempt, wait, e
                            )
                            await asyncio.sleep(wait)

            except asyncio.CancelledError:
                raise  # propagate — let lifespan clean up
            except Exception as e:
                logger.error("worker loop error: %s", e)
                await asyncio.sleep(2)

    finally:
        await rdb.aclose()
        logger.info("ingestion worker stopped")