from __future__ import annotations
import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.database import get_pool
from app.generate import GenerateRequest, generate_quests
from app.providers import ProviderConfig

router = APIRouter()
logger = logging.getLogger(__name__)


@router.get("/health")
async def health():
    return {"status": "ok", "service": "rag"}


@router.get("/ready")
async def ready():
    try:
        pool = get_pool()
        async with pool.connection() as conn:
            await conn.execute("SELECT 1")
        return {"status": "ready"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


class GenerateQuestsRequest(BaseModel):
    user_id: str
    generate_for: str = "daily"
    provider: str = "openai"
    api_key: str = ""
    model: str | None = None


@router.post("/generate")
async def generate(req: GenerateQuestsRequest):
    try:
        provider_config = None
        if req.api_key and len(req.api_key) >= 10:
            provider_config = ProviderConfig(
                provider=req.provider,
                api_key=req.api_key,
                model=req.model or None,
            )
            logger.info("using provider: %s", req.provider)
        else:
            logger.warning("no api_key provided — using mock provider")

        result = await generate_quests(
            GenerateRequest(
                user_id=req.user_id,
                generate_for=req.generate_for,
                provider_config=provider_config,
            )
        )
        return result
    except Exception as e:
        logger.error("generation error for user %s: %s", req.user_id, e)
        # return mock quests as fallback instead of 500
        return {
            "quests": [
                {
                    "title": "Complete a 20-minute walk",
                    "description": "Head outside for a brisk walk. Focus on your breathing.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "fitness", "xp_reward": 25,
                    "rationale": "Building movement baseline."
                },
                {
                    "title": "Read for 20 minutes",
                    "description": "Pick any book and read uninterrupted for 20 minutes.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "learning", "xp_reward": 20,
                    "rationale": "Daily reading compounds into knowledge."
                },
                {
                    "title": "Drink 8 glasses of water",
                    "description": "Track your water intake throughout the day.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "fitness", "xp_reward": 15,
                    "rationale": "Hydration is foundational health."
                }
            ]
        }