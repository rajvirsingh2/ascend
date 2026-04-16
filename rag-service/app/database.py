from contextlib import asynccontextmanager
import psycopg_pool
from app.config import settings

_pool: psycopg_pool.AsyncConnectionPool | None = None


async def init_pool() -> None:
    global _pool
    _pool = psycopg_pool.AsyncConnectionPool(
        conninfo=settings.database_url,
        min_size=2,
        max_size=10,
        open=False,
    )
    await _pool.open()


async def close_pool() -> None:
    if _pool:
        await _pool.close()


def get_pool() -> psycopg_pool.AsyncConnectionPool:
    if _pool is None:
        raise RuntimeError("database pool not initialised")
    return _pool


@asynccontextmanager
async def get_conn():
    async with get_pool().connection() as conn:
        yield conn