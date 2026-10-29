"""The HTTP layer: what Spring Boot will actually send and receive."""


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_analyze_returns_camel_case_json(client):
    response = client.post("/v1/analyze", json={
        "conversationId": "c-123",
        "turns": [
            {"speaker": "CUSTOMER", "text": "My order never arrived"},
            {"speaker": "AGENT", "text": "I'm so sorry, let me check"},
        ],
    })
    assert response.status_code == 200
    body = response.json()
    assert body["conversationId"] == "c-123"
    assert set(body) == {"conversationId", "modelVersions", "turns", "summary"}
    assert body["turns"][0]["sentiment"] == "NEGATIVE"
    assert body["turns"][1]["empathy"] == "EMPATHETIC"


def test_bad_speaker_is_rejected_before_the_analyzer_runs(client):
    response = client.post("/v1/analyze", json={
        "conversationId": "c-1",
        "turns": [{"speaker": "BOT", "text": "hi"}],
    })
    assert response.status_code == 422


def test_empty_conversation_is_rejected(client):
    response = client.post("/v1/analyze", json={"conversationId": "c-1", "turns": []})
    assert response.status_code == 422
