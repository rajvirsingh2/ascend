from contextlib import asynccontextmanager
import asyncio
import logging

from fastapi import FastAPI

from app.config import settings
from app.database import init_pool, close_pool
from app.routes import router
from app.worker import run_worker

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    logger.info("initialising database pool...")
    await init_pool()
    logger.info("starting ingestion worker...")
    worker_task = asyncio.create_task(run_worker())

    yield

    # shutdown
    worker_task.cancel()
    try:
        await worker_task
    except asyncio.CancelledError:
        pass
    await close_pool()
    logger.info("rag service shut down cleanly")


app = FastAPI(title="Ascend RAG Service", lifespan=lifespan)
app.include_router(router)