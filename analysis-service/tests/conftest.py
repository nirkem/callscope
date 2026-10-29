import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.analyzers.conversation import ConversationAnalyzer
from app.analyzers.fakes import FakeEmpathyClassifier, FakeSentimentClassifier
from app.api.routes import router


@pytest.fixture
def analyzer() -> ConversationAnalyzer:
    return ConversationAnalyzer(FakeSentimentClassifier(), FakeEmpathyClassifier())


@pytest.fixture
def client(analyzer: ConversationAnalyzer) -> TestClient:
    """The routes on a bare app, with the fake analyzer where main.py would put the real one."""
    app = FastAPI()
    app.include_router(router)
    app.state.conversation_analyzer = analyzer
    return TestClient(app)
