"""The real app from main.py, started the way uvicorn starts it."""

from fastapi.testclient import TestClient

from app.main import app


def test_startup_builds_the_analyzer_and_analyze_works():
    # "with" runs the lifespan: startup before the block, shutdown after it.
    with TestClient(app) as client:
        response = client.post("/v1/analyze", json={
            "conversationId": "c-1",
            "turns": [{"speaker": "CUSTOMER", "text": "Thanks, that's great"}],
        })
    assert response.status_code == 200
    assert response.json()["summary"]["customerSentiment"] == 90


def test_docs_describe_the_service():
    with TestClient(app) as client:
        spec = client.get("/openapi.json").json()
    assert spec["info"]["title"] == "CallScope analysis-service"
    assert {"/health", "/v1/analyze"} <= set(spec["paths"])
