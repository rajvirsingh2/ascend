from fastapi import APIRouter
from app.database import get_pool

router = APIRouter()


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
        from fastapi import HTTPException
        raise HTTPException(status_code=503, detail=str(e))