from fastapi.testclient import TestClient
import pytest


@pytest.fixture
def client():
    # patch pool so lifespan does not try to connect to a real DB in CI
    import unittest.mock as mock
    with mock.patch("app.database.init_pool", return_value=None), \
         mock.patch("app.database.close_pool", return_value=None), \
         mock.patch("app.worker.run_worker", return_value=asyncio_noop()):
        from main import app
        with TestClient(app, raise_server_exceptions=False) as c:
            yield c


async def asyncio_noop():
    import asyncio
    await asyncio.sleep(0)


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"