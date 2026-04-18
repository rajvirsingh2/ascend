import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.database import get_pool
from app.generate import GenerateRequest, generate_quests

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
    model: str|None=None


@router.post("/generate")
async def generate(req: GenerateQuestsRequest):
    """
    Called by the Go API — not exposed publicly.
    Returns generated quests as JSON.
    """
    try:
        provider_config=None
        if req.api_key:
            provider_config=ProviderConig(
                provider=req.provider,
                api_key=req.api_key,
                model=req.model
            )
        result = await generate_quests(
            GenerateRequest(
                user_id=req.user_id,
                generate_for=req.generate_for,
                provider_config=provider_config
            )
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("generation error for user %s: %s", req.user_id, e)
        raise HTTPException(status_code=500, detail="quest generation failed")